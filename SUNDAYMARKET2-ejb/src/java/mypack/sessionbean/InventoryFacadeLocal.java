/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Inventory;

/**
 *
 * @author MC
 */
@Local
public interface InventoryFacadeLocal {

    void create(Inventory inventory);

    void edit(Inventory inventory);

    void remove(Inventory inventory);

    Inventory find(Object id);

    List<Inventory> findAll();

    List<Inventory> findRange(int[] range);

    int count();
    
    Inventory findByProductId(Integer productId);
}
