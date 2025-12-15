/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mypack.entity.Inventory;

/**
 *
 * @author MC
 */
@Stateless
public class InventoryFacade extends AbstractFacade<Inventory> implements InventoryFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public InventoryFacade() {
        super(Inventory.class);
    }
     public Inventory findByProductId(Integer productId) {
        try {
            return getEntityManager().createNamedQuery("Inventory.findByProductID", Inventory.class)
                    .setParameter("productID", productId)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
