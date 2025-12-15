/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Notifications;

/**
 *
 * @author MC
 */
@Local
public interface NotificationsFacadeLocal {

    void create(Notifications notifications);

    void edit(Notifications notifications);

    void remove(Notifications notifications);

    Notifications find(Object id);

    List<Notifications> findAll();

    List<Notifications> findRange(int[] range);

    int count();
    
}
