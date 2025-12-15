/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.OrderPromotion;

/**
 *
 * @author MC
 */
@Local
public interface OrderPromotionFacadeLocal {

    void create(OrderPromotion orderPromotion);

    void edit(OrderPromotion orderPromotion);

    void remove(OrderPromotion orderPromotion);

    OrderPromotion find(Object id);

    List<OrderPromotion> findAll();

    List<OrderPromotion> findRange(int[] range);

    int count();
    
}
