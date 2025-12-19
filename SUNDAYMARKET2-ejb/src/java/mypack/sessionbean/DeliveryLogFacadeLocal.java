/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.DeliveryLog;
import mypack.entity.User;

/**
 *
 * @author MC
 */
@Local
public interface DeliveryLogFacadeLocal {

    void create(DeliveryLog deliveryLog);

    void edit(DeliveryLog deliveryLog);

    void remove(DeliveryLog deliveryLog);

    DeliveryLog find(Object id);

    List<DeliveryLog> findAll();

    List<DeliveryLog> findRange(int[] range);

    int count();
    
    // Find logs by delivery
    List<DeliveryLog> findByDelivery(Delivery delivery);
    
    // Find logs by shipper
    List<DeliveryLog> findByShipper(User shipper);
    
    // Find logs by event type
    List<DeliveryLog> findByEventType(String eventType);
    
    // Create a new log entry for a delivery
    DeliveryLog createLogEntry(Delivery delivery, String eventType, String description, String imagePath);
}
