/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.PromotionProduct;

/**
 *
 * @author MC
 */
@Local
public interface PromotionProductFacadeLocal {

    void create(PromotionProduct promotionProduct);

    void edit(PromotionProduct promotionProduct);

    void remove(PromotionProduct promotionProduct);

    PromotionProduct find(Object id);

    List<PromotionProduct> findAll();

    List<PromotionProduct> findRange(int[] range);

    int count();
    
}
