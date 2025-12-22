/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Order1;
import mypack.entity.Payment;

/**
 *
 * @author MC
 */
@Local
public interface PaymentFacadeLocal {

    void create(Payment payment);

    void edit(Payment payment);

    void remove(Payment payment);

    Payment find(Object id);

    List<Payment> findAll();

    List<Payment> findRange(int[] range);

    int count();
    
    Payment findByOrder(Order1 order);
    
    List<Payment> findByPaymentMethod(String paymentMethod);
    
    List<Payment> findByPaymentStatus(String paymentStatus);
    
}
