///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
// */
//package mypack.sessionbean;
//
//import jakarta.ejb.Local;
//import java.util.List;
//import mypack.entity.Product;
//
///**
// *
// * @author MC
// */
//@Local
//public interface ProductFacadeLocal {
//
//    void create(Product product);
//
//    void edit(Product product);
//
//    void remove(Product product);
//
//    Product find(Object id);
//
//    List<Product> findAll();
//
//    List<Product> findRange(int[] range);
//
//    int count();
//    
//    //thêm mới
//    List<Product> findByCategory(Integer categoryId);
//
//    List<Product> findAllActive();
//    
//}

// =================== ProductFacadeLocal.java ===================
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Product;

@Local
public interface ProductFacadeLocal {
    void create(Product product);
    void edit(Product product);
    void remove(Product product);
    Product find(Object id);
    List<Product> findAll();
    List<Product> findRange(int[] range);
    int count();
    
    // Custom methods
    List<Product> findAllActive();
    List<Product> findByCategory(Integer categoryId);
}