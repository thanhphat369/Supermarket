package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import mypack.entity.Feedback;
import mypack.entity.Product;
import mypack.entity.User;
import mypack.sessionbean.FeedbackFacadeLocal;
import mypack.sessionbean.ProductFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "feedbackMBean")
@SessionScoped
public class FeedbackMBean implements Serializable {

    @EJB
    private FeedbackFacadeLocal feedbackFacade;
    
    @EJB
    private ProductFacadeLocal productFacade;
    
    @EJB
    private UserFacadeLocal userFacade;
    
    private Feedback selected = new Feedback();
    private Integer selectedProductId;
    private Integer rating;
    private String content;
    private Product productForFeedback;
    private boolean showFeedbackModal = false;
    
    // === Admin Feedback Management ===
    private Product selectedProductForView;     // Sản phẩm đang xem feedback
    private boolean showProductFeedbacks = false; // Hiển thị modal feedback
    private String searchKeyword;               // Tìm kiếm sản phẩm
    private Integer ratingFilter;               // Lọc theo rating (1-5, null = tất cả)
    private int currentPage = 1;
    private int pageSize = 10;

    public FeedbackMBean() {
    }
    
    // Get feedbacks for a product
    public List<Feedback> getFeedbacksForProduct(Product product) {
        if (product == null) {
            return new java.util.ArrayList<>();
        }
        try {
            List<Feedback> all = feedbackFacade.findAll();
            List<Feedback> result = new java.util.ArrayList<>();
            for (Feedback f : all) {
                if (f.getProductID() != null && f.getProductID().getProductID().equals(product.getProductID())) {
                    result.add(f);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Get average rating for a product
    public double getAverageRating(Product product) {
        List<Feedback> feedbacks = getFeedbacksForProduct(product);
        if (feedbacks == null || feedbacks.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        int count = 0;
        for (Feedback f : feedbacks) {
            if (f.getRating() != null) {
                sum += f.getRating();
                count++;
            }
        }
        if (count == 0) {
            return 0.0;
        }
        return (double) sum / count;
    }
    
    // Prepare feedback form
    public void prepareFeedback(Product product) {
        if (product != null) {
            // Refresh product from database to ensure it's managed
            try {
                Product refreshed = productFacade.find(product.getProductID());
                if (refreshed != null) {
                    productForFeedback = refreshed;
                } else {
                    productForFeedback = product;
                }
            } catch (Exception e) {
                productForFeedback = product;
            }
            selectedProductId = product.getProductID();
            rating = null;
            content = null;
            showFeedbackModal = true;
        }
    }
    
    // Submit feedback
    public void submitFeedback() {
        submitFeedback(productForFeedback);
    }
    
    // Submit feedback
    public void submitFeedback(Product product) {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addErr("⚠️ Please login to submit feedback!");
                return;
            }
            
            // Use productForFeedback if product parameter is null
            Product productToUse = product != null ? product : productForFeedback;
            
            if (productToUse == null) {
                addErr("⚠️ Product not found!");
                return;
            }
            
            // Refresh product from database to ensure it's managed
            try {
                Product refreshed = productFacade.find(productToUse.getProductID());
                if (refreshed != null) {
                    productToUse = refreshed;
                }
            } catch (Exception e) {
                // Use original product if refresh fails
            }
            
            if (rating == null || rating < 1 || rating > 5) {
                addErr("⚠️ Please select a rating (1-5 stars)!");
                return;
            }
            
            if (content == null || content.trim().isEmpty()) {
                addErr("⚠️ Please enter feedback content!");
                return;
            }
            
            User currentUser = loginMBean.getCurrentUser();
            
            // Refresh user from database to ensure it's managed
            User userToUse = currentUser;
            if (currentUser != null && currentUser.getUserID() != null) {
                try {
                    User refreshed = userFacade.find(currentUser.getUserID());
                    if (refreshed != null) {
                        userToUse = refreshed;
                    }
                } catch (Exception e) {
                    // Use original user if refresh fails
                    e.printStackTrace();
                }
            }
            
            if (userToUse == null) {
                addErr("⚠️ User not found!");
                return;
            }
            
            Feedback feedback = new Feedback();
            feedback.setProductID(productToUse);
            feedback.setUserID(userToUse);
            feedback.setRating(rating);
            feedback.setContent(content.trim());
            feedback.setCreatedAt(new Date());
            
            System.out.println("FeedbackMBean.submitFeedback() - Creating feedback:");
            System.out.println("  Product ID: " + (productToUse != null ? productToUse.getProductID() : "null"));
            System.out.println("  User ID: " + (userToUse != null ? userToUse.getUserID() : "null"));
            System.out.println("  Rating: " + rating);
            System.out.println("  Content: " + (content != null ? content.substring(0, Math.min(50, content.length())) : "null"));
            
            feedbackFacade.create(feedback);
            
            System.out.println("FeedbackMBean.submitFeedback() - Feedback created successfully with ID: " + feedback.getFeedbackID());
            
            addInfo("✅ Feedback submitted successfully!");
            
            // Reset form
            rating = null;
            content = null;
            selectedProductId = null;
            productForFeedback = null;
            showFeedbackModal = false;
            
            // Refresh products list
            // The list will be refreshed when modal closes
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("FeedbackMBean.submitFeedback() - Exception: " + e.getClass().getName());
            System.err.println("FeedbackMBean.submitFeedback() - Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("FeedbackMBean.submitFeedback() - Cause: " + e.getCause().getMessage());
            }
            addErr("❌ Error submitting feedback: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
    
    // Close feedback modal
    public void closeFeedbackModal() {
        showFeedbackModal = false;
        productForFeedback = null;
        rating = null;
        content = null;
        selectedProductId = null;
    }
    
    // Format rating stars
    public String getRatingStars(Integer rating) {
        if (rating == null) {
            return "";
        }
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("⭐");
        }
        return stars.toString();
    }
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Getters and Setters
    public Feedback getSelected() {
        return selected;
    }

    public void setSelected(Feedback selected) {
        this.selected = selected;
    }

    public Integer getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Integer selectedProductId) {
        this.selectedProductId = selectedProductId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    // Action method to set rating from UI (called from actionListener)
    public void setRatingAction(jakarta.faces.event.ActionEvent event) {
        // Get rating from f:param
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        String ratingValue = facesContext.getExternalContext().getRequestParameterMap().get("ratingValue");
        if (ratingValue != null && !ratingValue.isEmpty()) {
            try {
                this.rating = Integer.parseInt(ratingValue);
            } catch (NumberFormatException e) {
                this.rating = null;
            }
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Product getProductForFeedback() {
        return productForFeedback;
    }

    public void setProductForFeedback(Product productForFeedback) {
        this.productForFeedback = productForFeedback;
    }

    public boolean isShowFeedbackModal() {
        return showFeedbackModal;
    }

    public void setShowFeedbackModal(boolean showFeedbackModal) {
        this.showFeedbackModal = showFeedbackModal;
    }
    
    // ==================== ADMIN FEEDBACK MANAGEMENT ====================
    
    /**
     * Lấy danh sách tất cả sản phẩm có feedback (cho Admin)
     * Sắp xếp theo số lượng feedback giảm dần
     */
    public List<Product> getProductsWithFeedback() {
        try {
            List<Product> allProducts = productFacade.findAll();
            List<Feedback> allFeedbacks = feedbackFacade.findAll();
            
            // Lọc sản phẩm có feedback
            List<Product> productsWithFeedback = new ArrayList<>();
            for (Product p : allProducts) {
                int feedbackCount = 0;
                for (Feedback f : allFeedbacks) {
                    if (f.getProductID() != null && f.getProductID().getProductID().equals(p.getProductID())) {
                        feedbackCount++;
                    }
                }
                if (feedbackCount > 0) {
                    productsWithFeedback.add(p);
                }
            }
            
            // Sắp xếp theo số lượng feedback giảm dần
            productsWithFeedback.sort((p1, p2) -> {
                int count1 = getFeedbackCountForProduct(p1);
                int count2 = getFeedbackCountForProduct(p2);
                return count2 - count1;
            });
            
            // Apply search filter
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                productsWithFeedback = productsWithFeedback.stream()
                    .filter(p -> p.getName().toLowerCase().contains(keyword) ||
                                (p.getBrandID() != null && p.getBrandID().getBrandName().toLowerCase().contains(keyword)) ||
                                (p.getCategoryID() != null && p.getCategoryID().getCategoryName().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
            }
            
            return productsWithFeedback;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Lấy danh sách TẤT CẢ sản phẩm (cho Customer - để có thể đánh giá)
     * Sắp xếp: sản phẩm có feedback trước, sau đó là sản phẩm chưa có feedback
     */
    public List<Product> getAllProductsForCustomer() {
        try {
            List<Product> allProducts = productFacade.findAll();
            List<Feedback> allFeedbacks = feedbackFacade.findAll();
            
            // Phân loại: sản phẩm có feedback và chưa có feedback
            List<Product> productsWithFeedback = new ArrayList<>();
            List<Product> productsWithoutFeedback = new ArrayList<>();
            
            for (Product p : allProducts) {
                boolean hasFeedback = false;
                for (Feedback f : allFeedbacks) {
                    if (f.getProductID() != null && f.getProductID().getProductID().equals(p.getProductID())) {
                        hasFeedback = true;
                        break;
                    }
                }
                if (hasFeedback) {
                    productsWithFeedback.add(p);
                } else {
                    productsWithoutFeedback.add(p);
                }
            }
            
            // Sắp xếp sản phẩm có feedback theo số lượng feedback giảm dần
            productsWithFeedback.sort((p1, p2) -> {
                int count1 = getFeedbackCountForProduct(p1);
                int count2 = getFeedbackCountForProduct(p2);
                return count2 - count1;
            });
            
            // Gộp lại: có feedback trước, chưa có feedback sau
            List<Product> result = new ArrayList<>();
            result.addAll(productsWithFeedback);
            result.addAll(productsWithoutFeedback);
            
            // Apply search filter
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                result = result.stream()
                    .filter(p -> p.getName().toLowerCase().contains(keyword) ||
                                (p.getBrandID() != null && p.getBrandID().getBrandName().toLowerCase().contains(keyword)) ||
                                (p.getCategoryID() != null && p.getCategoryID().getCategoryName().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Lấy danh sách sản phẩm có feedback - phân trang (cho Admin)
     */
    public List<Product> getPagedProductsWithFeedback() {
        List<Product> all = getProductsWithFeedback();
        if (all.isEmpty()) {
            return all;
        }
        
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        
        if (start >= all.size()) {
            currentPage = 1;
            start = 0;
            end = Math.min(pageSize, all.size());
        }
        
        return all.subList(start, end);
    }
    
    /**
     * Lấy danh sách TẤT CẢ sản phẩm - phân trang (cho Customer)
     */
    public List<Product> getPagedAllProductsForCustomer() {
        List<Product> all = getAllProductsForCustomer();
        if (all.isEmpty()) {
            return all;
        }
        
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        
        if (start >= all.size()) {
            currentPage = 1;
            start = 0;
            end = Math.min(pageSize, all.size());
        }
        
        return all.subList(start, end);
    }
    
    /**
     * Tổng số sản phẩm cho Customer (tất cả sản phẩm)
     */
    public int getTotalProductsForCustomer() {
        return getAllProductsForCustomer().size();
    }
    
    /**
     * Tổng số trang cho Customer
     */
    public int getTotalPagesForCustomer() {
        int total = getTotalProductsForCustomer();
        if (total == 0) return 1;
        return (int) Math.ceil((double) total / pageSize);
    }
    
    /**
     * Đếm số feedback của một sản phẩm
     */
    public int getFeedbackCountForProduct(Product product) {
        if (product == null) return 0;
        try {
            List<Feedback> feedbacks = getFeedbacksForProduct(product);
            return feedbacks != null ? feedbacks.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Lấy rating trung bình đã format (1 decimal)
     */
    public String getFormattedAverageRating(Product product) {
        double avg = getAverageRating(product);
        if (avg == 0) return "N/A";
        return String.format("%.1f", avg);
    }
    
    /**
     * Lấy feedback của sản phẩm đang xem (có lọc theo rating nếu có)
     */
    public List<Feedback> getSelectedProductFeedbacks() {
        if (selectedProductForView == null) {
            return new ArrayList<>();
        }
        
        try {
            List<Feedback> feedbacks = getFeedbacksForProduct(selectedProductForView);
            
            // Lọc theo rating nếu có
            if (ratingFilter != null && ratingFilter > 0) {
                feedbacks = feedbacks.stream()
                    .filter(f -> f.getRating() != null && f.getRating().equals(ratingFilter))
                    .collect(Collectors.toList());
            }
            
            // Sắp xếp theo ngày mới nhất
            feedbacks.sort((f1, f2) -> {
                if (f1.getCreatedAt() == null) return 1;
                if (f2.getCreatedAt() == null) return -1;
                return f2.getCreatedAt().compareTo(f1.getCreatedAt());
            });
            
            return feedbacks;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Mở modal xem feedback của sản phẩm
     */
    public void viewProductFeedbacks(Product product) {
        if (product != null) {
            selectedProductForView = product;
            ratingFilter = null; // Reset filter
            showProductFeedbacks = true;
            System.out.println("FeedbackMBean: Viewing feedbacks for product: " + product.getName());
        }
    }
    
    /**
     * Đóng modal xem feedback
     */
    public void closeProductFeedbacks() {
        selectedProductForView = null;
        showProductFeedbacks = false;
        ratingFilter = null;
    }
    
    /**
     * Lọc feedback theo rating
     */
    public void filterByRating() {
        // ratingFilter được bind tự động, chỉ cần re-render
        System.out.println("FeedbackMBean: Filter by rating: " + ratingFilter);
    }
    
    /**
     * Clear rating filter
     */
    public void clearRatingFilter() {
        ratingFilter = null;
    }
    
    /**
     * Action method to set rating filter from UI (called from actionListener)
     * JSF không thể gọi setter trực tiếp với tham số từ EL
     */
    public void setRatingFilterAction(jakarta.faces.event.ActionEvent event) {
        jakarta.faces.context.FacesContext facesContext = jakarta.faces.context.FacesContext.getCurrentInstance();
        String ratingValue = facesContext.getExternalContext().getRequestParameterMap().get("ratingValue");
        if (ratingValue != null && !ratingValue.isEmpty()) {
            try {
                this.ratingFilter = Integer.parseInt(ratingValue);
            } catch (NumberFormatException e) {
                this.ratingFilter = null;
            }
        }
    }
    
    /**
     * Get list of stars (1-5) for UI iteration
     * Thay thế list literal [1,2,3,4,5] trong EL
     */
    public List<Integer> getStars() {
        return List.of(1, 2, 3, 4, 5);
    }
    
    /**
     * Get list of stars in descending order (5-1) for UI iteration
     * Thay thế list literal [5,4,3,2,1] trong EL
     */
    public List<Integer> getStarsDesc() {
        return List.of(5, 4, 3, 2, 1);
    }
    
    // === Statistics methods ===
    
    /**
     * Tổng số feedback trong hệ thống
     */
    public int getTotalFeedbackCount() {
        try {
            return feedbackFacade.count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Tổng số sản phẩm có feedback
     */
    public int getTotalProductsWithFeedback() {
        return getProductsWithFeedback().size();
    }
    
    /**
     * Rating trung bình toàn hệ thống
     */
    public String getOverallAverageRating() {
        try {
            List<Feedback> all = feedbackFacade.findAll();
            if (all == null || all.isEmpty()) return "N/A";
            
            int sum = 0;
            int count = 0;
            for (Feedback f : all) {
                if (f.getRating() != null) {
                    sum += f.getRating();
                    count++;
                }
            }
            
            if (count == 0) return "N/A";
            return String.format("%.1f", (double) sum / count);
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    /**
     * Đếm số feedback theo từng rating (1-5 sao)
     */
    public int getFeedbackCountByRating(int rating) {
        try {
            List<Feedback> all = feedbackFacade.findAll();
            return (int) all.stream()
                .filter(f -> f.getRating() != null && f.getRating() == rating)
                .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Đếm feedback theo rating cho một sản phẩm cụ thể
     */
    public int getFeedbackCountByRatingForProduct(Product product, int rating) {
        if (product == null) return 0;
        try {
            List<Feedback> feedbacks = getFeedbacksForProduct(product);
            return (int) feedbacks.stream()
                .filter(f -> f.getRating() != null && f.getRating() == rating)
                .count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Tính % rating cho product
     */
    public int getRatingPercentageForProduct(Product product, int rating) {
        if (product == null) return 0;
        int total = getFeedbackCountForProduct(product);
        if (total == 0) return 0;
        int count = getFeedbackCountByRatingForProduct(product, rating);
        return (count * 100) / total;
    }
    
    // === Search & Pagination ===
    
    public void performSearch() {
        currentPage = 1;
    }
    
    public void clearSearch() {
        searchKeyword = null;
        currentPage = 1;
    }
    
    public void firstPage() { currentPage = 1; }
    
    public void previousPage() {
        if (currentPage > 1) currentPage--;
    }
    
    public void nextPage() {
        // Check if admin or customer
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
                LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
                
                if (loginMBean != null && loginMBean.isAdmin()) {
                    // Admin: use getTotalPages()
                    if (currentPage < getTotalPages()) currentPage++;
                } else {
                    // Customer: use getTotalPagesForCustomer()
                    if (currentPage < getTotalPagesForCustomer()) currentPage++;
                }
            } else {
                if (currentPage < getTotalPages()) currentPage++;
            }
        } catch (Exception e) {
            if (currentPage < getTotalPages()) currentPage++;
        }
    }
    
    public void lastPage() {
        // Check if admin or customer
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
                LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
                
                if (loginMBean != null && loginMBean.isAdmin()) {
                    // Admin: use getTotalPages()
                    currentPage = getTotalPages();
                } else {
                    // Customer: use getTotalPagesForCustomer()
                    currentPage = getTotalPagesForCustomer();
                }
            } else {
                currentPage = getTotalPages();
            }
        } catch (Exception e) {
            currentPage = getTotalPages();
        }
    }
    
    public int getTotalPages() {
        int total = getProductsWithFeedback().size();
        if (total == 0) return 1;
        return (int) Math.ceil((double) total / pageSize);
    }
    
    public int getTotalItems() {
        return getProductsWithFeedback().size();
    }
    
    // === Format methods ===
    
    /**
     * Format ngày giờ
     */
    public String formatDate(Date date) {
        if (date == null) return "-";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    /**
     * Tạo rating stars HTML
     */
    public String getRatingStarsHtml(Integer rating) {
        if (rating == null || rating < 1) return "<span style='color:#ccc;'>☆☆☆☆☆</span>";
        StringBuilder sb = new StringBuilder();
        sb.append("<span style='color:#ffc107;'>");
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                sb.append("★");
            } else {
                sb.append("☆");
            }
        }
        sb.append("</span>");
        return sb.toString();
    }
    
    /**
     * Get rating color based on rating value (Integer version - for fb.rating)
     */
    public String getRatingColor(Integer rating) {
        if (rating == null) return "#6c757d";
        if (rating >= 5) return "#28a745"; // Green - Excellent
        if (rating >= 4) return "#20c997"; // Teal - Very Good
        if (rating >= 3) return "#ffc107"; // Yellow - Good
        if (rating >= 2) return "#fd7e14"; // Orange - Average
        return "#dc3545"; // Red - Poor
    }
    
    /**
     * Get rating color based on rating value (double version - for average rating)
     */
    public String getRatingColorDouble(double rating) {
        if (rating >= 4.5) return "#28a745"; // Green - Excellent
        if (rating >= 4.0) return "#20c997"; // Teal - Very Good
        if (rating >= 3.0) return "#ffc107"; // Yellow - Good
        if (rating >= 2.0) return "#fd7e14"; // Orange - Average
        return "#dc3545"; // Red - Poor
    }
    
    // === Getters & Setters for new fields ===
    
    public Product getSelectedProductForView() {
        return selectedProductForView;
    }

    public void setSelectedProductForView(Product selectedProductForView) {
        this.selectedProductForView = selectedProductForView;
    }

    public boolean isShowProductFeedbacks() {
        return showProductFeedbacks;
    }

    public void setShowProductFeedbacks(boolean showProductFeedbacks) {
        this.showProductFeedbacks = showProductFeedbacks;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public Integer getRatingFilter() {
        return ratingFilter;
    }

    public void setRatingFilter(Integer ratingFilter) {
        this.ratingFilter = ratingFilter;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}

