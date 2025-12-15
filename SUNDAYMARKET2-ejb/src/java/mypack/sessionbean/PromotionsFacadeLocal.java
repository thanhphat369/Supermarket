/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Promotions;

/**
 *
 * @author MC
 */
@Local
public interface PromotionsFacadeLocal {

    void create(Promotions promotions);

    void edit(Promotions promotions);

    void remove(Promotions promotions);

    Promotions find(Object id);

    List<Promotions> findAll();

    List<Promotions> findRange(int[] range);

    int count();
    
}
