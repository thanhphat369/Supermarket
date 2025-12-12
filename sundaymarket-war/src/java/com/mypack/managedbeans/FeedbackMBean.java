package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
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
            productForFeedback = product;
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
            
            if (product == null) {
                addErr("⚠️ Product not found!");
                return;
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
            
            Feedback feedback = new Feedback();
            feedback.setProductID(product);
            feedback.setUserID(currentUser);
            feedback.setRating(rating);
            feedback.setContent(content.trim());
            feedback.setCreatedAt(new Date());
            
            feedbackFacade.create(feedback);
            
            addInfo("✅ Feedback submitted successfully!");
            
            // Reset form
            rating = null;
            content = null;
            selectedProductId = null;
            productForFeedback = null;
            showFeedbackModal = false;
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Error submitting feedback: " + e.getMessage());
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
}

