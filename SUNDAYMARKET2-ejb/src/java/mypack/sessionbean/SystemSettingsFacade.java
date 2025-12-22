/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mypack.entity.SystemSettings;

/**
 *
 * @author MC
 */
@Stateless
public class SystemSettingsFacade extends AbstractFacade<SystemSettings> implements SystemSettingsFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SystemSettingsFacade() {
        super(SystemSettings.class);
    }
    
    @Override
    public SystemSettings findBySettingKey(String settingKey) {
        try {
            return em.createQuery(
                "SELECT s FROM SystemSettings s WHERE s.settingKey = :settingKey",
                SystemSettings.class)
            .setParameter("settingKey", settingKey)
            .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
    
}
