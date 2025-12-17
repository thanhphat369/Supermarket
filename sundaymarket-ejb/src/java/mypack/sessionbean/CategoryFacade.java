//package mypack.sessionbean;
//
//import jakarta.ejb.Stateless;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import java.util.List;
//import mypack.entity.Category;
//
//@Stateless
//public class CategoryFacade extends AbstractFacade<Category>
//        implements CategoryFacadeLocal {
//
//    @PersistenceContext(unitName = "sundaymarket-ejbPU")
//    private EntityManager em;
//
//    @Override
//    protected EntityManager getEntityManager() {
//        return em;
//    }
//
//    public CategoryFacade() {
//        super(Category.class);
//    }
//    
//    @Override
//    public Category find(Object id) {
//        return super.find(id);
//    }
//
//    @Override
//    public List<Category> findAll() {
//        return super.findAll();
//    }
//
//    
//    // LEVEL 1
//    @Override
//    // ===== LEVEL 1 =====
//    public List<Category> findLevel1() {
//        return em.createQuery(
//                "SELECT c FROM Category c "
//                + "WHERE c.parentCategory IS NULL "
//                + "AND c.level = 1 "
//                + "AND (c.isActive = true OR c.isActive IS NULL) "
//                + "ORDER BY c.sortOrder",
//                Category.class
//        ).getResultList();
//    }
//
//    // CHILDREN (LEVEL 2 hoặc 3)
//    @Override
//    public List<Category> findByParent(Category parent) {
//        return em.createQuery(
//                "SELECT c FROM Category c "
//                + "WHERE c.parentCategory = :parent "
//                + "AND (c.isActive = true OR c.isActive IS NULL) "
//                + "ORDER BY c.sortOrder",
//                Category.class
//        )
//                .setParameter("parent", parent)
//                .getResultList();
//    }
//
//    // LEVEL 2
//    @Override
//    public List<Category> findLevel2(Category parent) {
//        return em.createQuery(
//                "SELECT c FROM Category c "
//                + "WHERE c.parentCategory = :parent "
//                + "AND c.level = 2 "
//                + "AND (c.isActive = true OR c.isActive IS NULL) "
//                + "ORDER BY c.sortOrder",
//                Category.class
//        ).setParameter("parent", parent)
//                .getResultList();
//    }
//
//    // LEVEL 3
//    @Override
//    // ===== LEVEL 3 =====
//    public List<Category> findLevel3(Category parent) {
//        return em.createQuery(
//                "SELECT c FROM Category c "
//                + "WHERE c.parentCategory = :parent "
//                + "AND c.level = 3 "
//                + "AND (c.isActive = true OR c.isActive IS NULL) "
//                + "ORDER BY c.sortOrder",
//                Category.class
//        ).setParameter("parent", parent)
//                .getResultList();
//    }
//
//    @Override
//    public List<Category> findActive() {
//        return em.createQuery(
//                "SELECT c FROM Category c WHERE c.isActive = true",
//                Category.class
//        ).getResultList();
//    }
//
//}

// =================== CategoryFacade.java ===================
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import mypack.entity.Category;

@Stateless
public class CategoryFacade extends AbstractFacade<Category> implements CategoryFacadeLocal {

    @PersistenceContext(unitName = "sundaymarket-ejbPU")
    private EntityManager em;

    public CategoryFacade() {
        super(Category.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<Category> findActive() {
        try {
            return em.createNamedQuery("Category.findActive", Category.class)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("CategoryFacade.findActive() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Category> findLevel1() {
        try {
            return em.createNamedQuery("Category.findLevel1", Category.class)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("CategoryFacade.findLevel1() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Category> findLevel2(Category level1) {
        if (level1 == null || level1.getCategoryID() == null) {
            return new ArrayList<>();
        }
        
        try {
            return em.createQuery(
                "SELECT c FROM Category c " +
                "WHERE c.parentCategoryID = :parent " +
                "AND c.level = 2 " +
                "AND c.isActive = 1 " +
                "ORDER BY c.sortOrder ASC",
                Category.class
            )
            .setParameter("parent", level1)
            .getResultList();
        } catch (Exception e) {
            System.err.println("CategoryFacade.findLevel2() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Category> findLevel3(Category level2) {
        if (level2 == null || level2.getCategoryID() == null) {
            return new ArrayList<>();
        }
        
        try {
            return em.createQuery(
                "SELECT c FROM Category c " +
                "WHERE c.parentCategoryID = :parent " +
                "AND c.level = 3 " +
                "AND c.isActive = 1 " +
                "ORDER BY c.sortOrder ASC",
                Category.class
            )
            .setParameter("parent", level2)
            .getResultList();
        } catch (Exception e) {
            System.err.println("CategoryFacade.findLevel3() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Category> findByParent(Category parent) {
        if (parent == null || parent.getCategoryID() == null) {
            return new ArrayList<>();
        }
        
        try {
            return em.createNamedQuery("Category.findByParent", Category.class)
                    .setParameter("parent", parent)
                    .getResultList();
        } catch (Exception e) {
            System.err.println("CategoryFacade.findByParent() - Error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public boolean hasChildren(Category category) {
        if (category == null || category.getCategoryID() == null) {
            return false;
        }
        
        try {
            Long count = em.createQuery(
                "SELECT COUNT(c) FROM Category c " +
                "WHERE c.parentCategoryID = :parent " +
                "AND c.isActive = 1",
                Long.class
            )
            .setParameter("parent", category)
            .getSingleResult();
            
            return count != null && count > 0;
        } catch (Exception e) {
            System.err.println("CategoryFacade.hasChildren() - Error: " + e.getMessage());
            return false;
        }
    }
}
