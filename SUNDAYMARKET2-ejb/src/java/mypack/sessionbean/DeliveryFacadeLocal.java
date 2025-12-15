/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Delivery;

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
    
}
