/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.OrderDetails;

/**
 *
 * @author MC
 */
@Local
public interface OrderDetailsFacadeLocal {

    void create(OrderDetails orderDetails);

    void edit(OrderDetails orderDetails);

    void remove(OrderDetails orderDetails);

    OrderDetails find(Object id);

    List<OrderDetails> findAll();

    List<OrderDetails> findRange(int[] range);

    int count();
    
}
