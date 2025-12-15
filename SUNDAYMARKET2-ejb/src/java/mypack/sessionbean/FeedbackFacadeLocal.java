/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.Feedback;

/**
 *
 * @author MC
 */
@Local
public interface FeedbackFacadeLocal {

    void create(Feedback feedback);

    void edit(Feedback feedback);

    void remove(Feedback feedback);

    Feedback find(Object id);

    List<Feedback> findAll();

    List<Feedback> findRange(int[] range);

    int count();
    
}
