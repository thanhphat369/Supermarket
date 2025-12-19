/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.Order1;
import mypack.entity.User;

/**
 *
 * @author MC
 */
@Local
public interface DeliveryFacadeLocal {

    void create(Delivery delivery);

    void edit(Delivery delivery);

    void remove(Delivery delivery);

    Delivery find(Object id);

    List<Delivery> findAll();

    List<Delivery> findRange(int[] range);

    int count();
    
    // Find deliveries by shipper (User)
    List<Delivery> findByShipper(User shipper);
    
    // Find deliveries by order
    List<Delivery> findByOrder(Order1 order);
    
    // Find delivery by order and shipper
    Delivery findByOrderAndShipper(Order1 order, User shipper);
    
}
