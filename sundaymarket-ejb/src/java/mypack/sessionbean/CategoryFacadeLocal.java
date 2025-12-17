//package mypack.sessionbean;
//
//import jakarta.ejb.Local;
//import java.util.List;
//import mypack.entity.Category;
//
//@Local
//public interface CategoryFacadeLocal {
//
//    List<Category> findLevel1();
//
//    List<Category> findLevel2(Category parent);
//
//    List<Category> findLevel3(Category parent);
//    
//    public List<Category> findActive();
//    
//    List<Category> findByParent(Category parent);
//
//    Category find(Object id);
//
//    public List<Category> findAll();
//}

package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Category;

@Local
public interface CategoryFacadeLocal {
    void create(Category category);
    void edit(Category category);
    void remove(Category category);
    Category find(Object id);
    List<Category> findAll();
    List<Category> findRange(int[] range);
    int count();
    
    // Custom methods
    List<Category> findActive();
    List<Category> findLevel1();
    List<Category> findLevel2(Category level1);
    List<Category> findLevel3(Category level2);
    List<Category> findByParent(Category parent);
    boolean hasChildren(Category category);
}
