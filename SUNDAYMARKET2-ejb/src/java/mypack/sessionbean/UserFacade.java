/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mypack.entity.User;

/**
 *
 * @author MC
 */
@Stateless
public class UserFacade extends AbstractFacade<User> implements UserFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UserFacade() {
        super(User.class);
    }
     @Override
    public User checkLoginUser(String username, String password) {
        try {
            // Trim username và password để loại bỏ khoảng trắng
            if (username != null) {
                username = username.trim();
            }
            if (password != null) {
                password = password.trim();
            }
            
            // isActive là boolean, dùng = true thay vì = 1
            return em.createQuery(
                "SELECT u FROM User u WHERE u.userName = :username AND u.password = :password AND u.isActive = true",
                User.class)
            .setParameter("username", username)
            .setParameter("password", password)
            .getSingleResult();
        } catch (Exception e) {
            e.printStackTrace(); // Log lỗi để debug
            return null;  // Sai user/pass
        }
    }

    @Override
    public User findByUsername(String username) {
        try {
            return em.createQuery(
                "SELECT u FROM User u WHERE u.userName = :username",
                User.class)
            .setParameter("username", username)
            .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public User findByEmail(String email) {
        try {
            return em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email",
                User.class)
            .setParameter("email", email)
            .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
