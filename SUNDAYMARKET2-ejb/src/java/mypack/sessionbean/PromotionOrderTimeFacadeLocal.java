/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.PromotionOrderTime;

/**
 *
 * @author MC
 */
@Local
public interface PromotionOrderTimeFacadeLocal {

    void create(PromotionOrderTime promotionOrderTime);

    void edit(PromotionOrderTime promotionOrderTime);

    void remove(PromotionOrderTime promotionOrderTime);

    PromotionOrderTime find(Object id);

    List<PromotionOrderTime> findAll();

    List<PromotionOrderTime> findRange(int[] range);

    int count();
    
}
