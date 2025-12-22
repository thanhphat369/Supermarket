/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.SystemSettings;

/**
 *
 * @author MC
 */
@Local
public interface SystemSettingsFacadeLocal {

    void create(SystemSettings systemSettings);

    void edit(SystemSettings systemSettings);

    void remove(SystemSettings systemSettings);

    SystemSettings find(Object id);

    List<SystemSettings> findAll();

    List<SystemSettings> findRange(int[] range);

    int count();
    
    SystemSettings findBySettingKey(String settingKey);
    
}
