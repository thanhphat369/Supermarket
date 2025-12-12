/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.PurchaseOrderDetails;

/**
 *
 * @author MC
 */
@Local
public interface PurchaseOrderDetailsFacadeLocal {

    void create(PurchaseOrderDetails purchaseOrderDetails);

    void edit(PurchaseOrderDetails purchaseOrderDetails);

    void remove(PurchaseOrderDetails purchaseOrderDetails);

    PurchaseOrderDetails find(Object id);

    List<PurchaseOrderDetails> findAll();

    List<PurchaseOrderDetails> findRange(int[] range);

    int count();
    
}
