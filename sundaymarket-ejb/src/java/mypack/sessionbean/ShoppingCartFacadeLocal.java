/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.ShoppingCart;

/**
 *
 * @author MC
 */
@Local
public interface ShoppingCartFacadeLocal {

    void create(ShoppingCart shoppingCart);

    void edit(ShoppingCart shoppingCart);

    void remove(ShoppingCart shoppingCart);

    ShoppingCart find(Object id);

    List<ShoppingCart> findAll();

    List<ShoppingCart> findRange(int[] range);

    int count();
    
}
