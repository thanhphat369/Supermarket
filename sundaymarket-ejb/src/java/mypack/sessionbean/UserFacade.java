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

    @PersistenceContext(unitName = "sundaymarket-ejbPU")
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
            System.out.println("=== UserFacade.checkLoginUser ===");
            System.out.println("Username: " + username);
            System.out.println("Password input: " + (password != null ? password : "null"));
            System.out.println("Password length: " + (password != null ? password.length() : 0));
            
            // Tìm user theo username trước
            User user = findByUsername(username);
            if (user == null) {
                System.out.println("User not found with username: " + username);
                return null;
            }
            
            System.out.println("User found in DB:");
            System.out.println("  - UserID: " + user.getUserID());
            System.out.println("  - UserName: " + user.getUserName());
            System.out.println("  - Password in DB: " + (user.getPassword() != null ? user.getPassword() : "null"));
            System.out.println("  - Password length in DB: " + (user.getPassword() != null ? user.getPassword().length() : 0));
            System.out.println("  - IsActive: " + user.getIsActive());
            
            // Kiểm tra isActive
            if (!user.getIsActive()) {
                System.out.println("User is not active!");
                return null;
            }
            
            // So sánh password (trim để loại bỏ khoảng trắng thừa)
            String dbPassword = user.getPassword() != null ? user.getPassword().trim() : "";
            String inputPassword = password != null ? password.trim() : "";
            
            System.out.println("Comparing passwords:");
            System.out.println("  - DB password (trimmed): [" + dbPassword + "]");
            System.out.println("  - Input password (trimmed): [" + inputPassword + "]");
            System.out.println("  - Are equal: " + dbPassword.equals(inputPassword));
            
            if (!dbPassword.equals(inputPassword)) {
                System.out.println("Password mismatch!");
                return null;
            }
            
            System.out.println("Login successful for user: " + user.getUserID());
            return user;
        } catch (Exception e) {
            System.err.println("Error in checkLoginUser: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public User findByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.userName = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
