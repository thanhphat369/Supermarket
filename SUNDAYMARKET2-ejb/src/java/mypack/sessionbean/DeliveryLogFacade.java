/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mypack.entity.Delivery;
import mypack.entity.DeliveryLog;
import mypack.entity.User;

/**
 *
 * @author MC
 */
@Stateless
public class DeliveryLogFacade extends AbstractFacade<DeliveryLog> implements DeliveryLogFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public DeliveryLogFacade() {
        super(DeliveryLog.class);
    }
    
    @Override
    public List<DeliveryLog> findByDelivery(Delivery delivery) {
        try {
            if (delivery == null || delivery.getDeliveryID() == null) {
                return new ArrayList<>();
            }
            return em.createQuery("SELECT d FROM DeliveryLog d WHERE d.deliveryID.deliveryID = :deliveryId ORDER BY d.createdAt DESC", DeliveryLog.class)
                    .setParameter("deliveryId", delivery.getDeliveryID())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<DeliveryLog> findByShipper(User shipper) {
        try {
            if (shipper == null || shipper.getUserID() == null) {
                return new ArrayList<>();
            }
            return em.createQuery("SELECT d FROM DeliveryLog d WHERE d.deliveryID.userID.userID = :shipperId ORDER BY d.createdAt DESC", DeliveryLog.class)
                    .setParameter("shipperId", shipper.getUserID())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<DeliveryLog> findByEventType(String eventType) {
        try {
            if (eventType == null || eventType.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return em.createQuery("SELECT d FROM DeliveryLog d WHERE UPPER(d.eventType) = UPPER(:eventType) ORDER BY d.createdAt DESC", DeliveryLog.class)
                    .setParameter("eventType", eventType.trim())
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public DeliveryLog createLogEntry(Delivery delivery, String eventType, String description, String imagePath) {
        try {
            if (delivery == null || eventType == null) {
                return null;
            }
            
            DeliveryLog log = new DeliveryLog();
            log.setDeliveryID(delivery);
            log.setEventType(eventType);
            log.setDescription(description);
            log.setImagePath(imagePath);
            log.setCreatedAt(new Date());
            
            em.persist(log);
            return log;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
