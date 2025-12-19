/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.Order1;
import mypack.entity.User;

/**
 *
 * @author MC
 */
@Stateless
public class DeliveryFacade extends AbstractFacade<Delivery> implements DeliveryFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public DeliveryFacade() {
        super(Delivery.class);
    }
    @Override
    public List<Delivery> findByShipper(User shipper) {
        try {
            if (shipper == null || shipper.getUserID() == null) {
                return new ArrayList<>();
            }
            return em.createQuery("SELECT d FROM Delivery d WHERE d.userID.userID = :shipperId ORDER BY d.updatedAt DESC", Delivery.class)
                    .setParameter("shipperId", shipper.getUserID())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<Delivery> findByOrder(Order1 order) {
        try {
            if (order == null || order.getOrderID() == null) {
                return new ArrayList<>();
            }
            return em.createQuery("SELECT d FROM Delivery d WHERE d.orderID.orderID = :orderId ORDER BY d.updatedAt DESC", Delivery.class)
                    .setParameter("orderId", order.getOrderID())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public Delivery findByOrderAndShipper(Order1 order, User shipper) {
        try {
            if (order == null || shipper == null) {
                return null;
            }
            return em.createQuery("SELECT d FROM Delivery d WHERE d.orderID.orderID = :orderId AND d.userID.userID = :shipperId", Delivery.class)
                    .setParameter("orderId", order.getOrderID())
                    .setParameter("shipperId", shipper.getUserID())
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
