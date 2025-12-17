//package mypack.sessionbean;
//
//import jakarta.ejb.Stateless;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import jakarta.persistence.TypedQuery;
//import java.util.ArrayList;
//import java.util.List;
//import mypack.entity.Category;
//import mypack.entity.Product;
//
//@Stateless
//public class ProductFacade extends AbstractFacade<Product> implements ProductFacadeLocal {
//    
//    @PersistenceContext(unitName = "sundaymarket-ejbPU")
//    private EntityManager em;
//    
//    @Override
//    protected EntityManager getEntityManager() {
//        return em;
//    }
//    
//    public ProductFacade() {
//        super(Product.class);
//    }
//    
//    /**
//     * Tìm sản phẩm theo category và tất cả children của nó (đệ quy)
//     * @param categoryId ID của category cha
//     * @return Danh sách sản phẩm
//     */
//    //thêm mới
//    
//    // SẢN PHẨM THEO CATEGORY (LEVEL 3)
//    public List<Product> findByCategory(Integer categoryId) {
//        return em.createQuery(
//            "SELECT p FROM Product p " +
//            "WHERE p.categoryID.categoryID = :cid " +
//            "AND p.status = 'Active'",
//            Product.class
//        ).setParameter("cid", categoryId)
//         .getResultList();
//    }
//
//    public List<Product> findAllActive() {
//        return em.createQuery(
//            "SELECT p FROM Product p WHERE p.status = 'Active'",
//            Product.class
//        ).getResultList();
//    }
//    
//    public List<Product> findByCategoryIncludingChildren(Integer categoryId) {
//        // Lấy tất cả ID của category và children
//        List<Integer> categoryIds = new ArrayList<>();
//        collectCategoryIdsRecursive(categoryId, categoryIds);
//        
//        // Nếu không có category nào, trả về list rỗng
//        if (categoryIds.isEmpty()) {
//            return new ArrayList<>();
//        }
//        
//        // Query với IN clause - JPA hỗ trợ truyền List trực tiếp
//        TypedQuery<Product> query = em.createQuery(
//            "SELECT p FROM Product p WHERE p.categoryID.categoryID IN :categoryIds AND p.status = 'Active'", 
//            Product.class
//        );
//        query.setParameter("categoryIds", categoryIds);
//        
//        return query.getResultList();
//    }
//    
//    /**
//     * Thu thập tất cả ID của category và children đệ quy
//     * @param categoryId ID category hiện tại
//     * @param categoryIds List để lưu trữ kết quả
//     */
//    private void collectCategoryIdsRecursive(Integer categoryId, List<Integer> categoryIds) {
//        // Thêm ID hiện tại
//        categoryIds.add(categoryId);
//        
//        // Tìm tất cả category con
//        TypedQuery<Category> query = em.createQuery(
//            "SELECT c FROM Category c WHERE c.parentCategory.categoryID = :parentId", 
//            Category.class
//        );
//        query.setParameter("parentId", categoryId);
//        List<Category> children = query.getResultList();
//        
//        // Đệ quy với từng con
//        for (Category child : children) {
//            collectCategoryIdsRecursive(child.getCategoryID(), categoryIds);
//        }
//    }
//    
//    
//    /**
//     * Tìm sản phẩm active
//     * @return Danh sách sản phẩm
//     */
//    public List<Product> findActiveProducts() {
//        TypedQuery<Product> query = em.createQuery(
//            "SELECT p FROM Product p WHERE p.status = 'Active' ORDER BY p.createdAt DESC", 
//            Product.class
//        );
//        return query.getResultList();
//    }
//    
//    /**
//     * Tìm sản phẩm theo tên (search)
//     * @param searchTerm Từ khóa tìm kiếm
//     * @return Danh sách sản phẩm
//     */
//    public List<Product> searchProducts(String searchTerm) {
//        TypedQuery<Product> query = em.createQuery(
//            "SELECT p FROM Product p WHERE LOWER(p.name) LIKE :searchTerm AND p.status = 'Active'", 
//            Product.class
//        );
//        query.setParameter("searchTerm", "%" + searchTerm.toLowerCase() + "%");
//        return query.getResultList();
//    }
//    
//    /**
//     * Tìm sản phẩm hot (stock thấp)
//     * @param threshold Ngưỡng stock
//     * @return Danh sách sản phẩm
//     */
//    public List<Product> findLowStockProducts(int threshold) {
//        TypedQuery<Product> query = em.createQuery(
//            "SELECT p FROM Product p WHERE p.stock > 0 AND p.stock < :threshold AND p.status = 'Active'", 
//            Product.class
//        );
//        query.setParameter("threshold", threshold);
//        return query.getResultList();
//    }
//}

// =================== ProductFacade.java ===================
package mypack.sessionbean;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import mypack.entity.Category;
import mypack.entity.Product;

@Stateless
public class ProductFacade extends AbstractFacade<Product> implements ProductFacadeLocal {

    @PersistenceContext(unitName = "sundaymarket-ejbPU")
    private EntityManager em;
    
    @EJB
    private CategoryFacadeLocal categoryFacade;

    public ProductFacade() {
        super(Product.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<Product> findAllActive() {
        try {
            return em.createNamedQuery("Product.findActive", Product.class)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("ProductFacade.findAllActive() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Product> findByCategory(Integer categoryId) {
        if (categoryId == null) {
            return findAllActive();
        }
        
        try {
            Category category = categoryFacade.find(categoryId);
            if (category == null) {
                return new ArrayList<>();
            }
            
            Integer level = category.getLevel();
            
            if (level != null && level == 3) {
                // Level 3: Lấy sản phẩm trực tiếp
                return em.createNamedQuery("Product.findByCategory", Product.class)
                        .setParameter("categoryId", categoryId)
                        .getResultList();
                        
            } else if (level != null && level == 2) {
                // Level 2: Lấy tất cả sản phẩm của các Level 3 con
                List<Category> level3Children = categoryFacade.findLevel3(category);
                
                if (level3Children.isEmpty()) {
                    return new ArrayList<>();
                }
                
                List<Integer> level3Ids = level3Children.stream()
                    .map(Category::getCategoryID)
                    .collect(Collectors.toList());
                
                return em.createQuery(
                    "SELECT p FROM Product p " +
                    "WHERE p.categoryID.categoryID IN :categoryIds " +
                    "AND p.status = 'Active' " +
                    "ORDER BY p.productID DESC",
                    Product.class
                )
                .setParameter("categoryIds", level3Ids)
                .getResultList();
                
            } else {
                // Level 1: Không hỗ trợ trực tiếp
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            System.err.println("ProductFacade.findByCategory() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}