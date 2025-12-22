/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import mypack.entity.Order1;
import mypack.entity.Payment;

/**
 *
 * @author MC
 */
@Stateless
public class PaymentFacade extends AbstractFacade<Payment> implements PaymentFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentFacade() {
        super(Payment.class);
    }
    
    @Override
    public Payment findByOrder(Order1 order) {
        try {
            TypedQuery<Payment> query = em.createNamedQuery("Payment.findByOrderID", Payment.class);
            query.setParameter("orderID", order);
            List<Payment> results = query.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public List<Payment> findByPaymentMethod(String paymentMethod) {
        try {
            TypedQuery<Payment> query = em.createNamedQuery("Payment.findByPaymentMethod", Payment.class);
            query.setParameter("paymentMethod", paymentMethod);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public List<Payment> findByPaymentStatus(String paymentStatus) {
        try {
            TypedQuery<Payment> query = em.createNamedQuery("Payment.findByPaymentStatus", Payment.class);
            query.setParameter("paymentStatus", paymentStatus);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
}
