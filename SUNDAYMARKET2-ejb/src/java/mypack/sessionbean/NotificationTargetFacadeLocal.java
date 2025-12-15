/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.NotificationTarget;

/**
 *
 * @author MC
 */
@Local
public interface NotificationTargetFacadeLocal {

    void create(NotificationTarget notificationTarget);

    void edit(NotificationTarget notificationTarget);

    void remove(NotificationTarget notificationTarget);

    NotificationTarget find(Object id);

    List<NotificationTarget> findAll();

    List<NotificationTarget> findRange(int[] range);

    int count();
    
}
