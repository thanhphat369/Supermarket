/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mypack.entity.OrderPromotion;

/**
 *
 * @author MC
 */
@Stateless
public class OrderPromotionFacade extends AbstractFacade<OrderPromotion> implements OrderPromotionFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public OrderPromotionFacade() {
        super(OrderPromotion.class);
    }
    
}
