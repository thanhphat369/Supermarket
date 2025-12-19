package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import java.io.File;
import java.io.Serializable;
import mypack.entity.User;
import mypack.sessionbean.UserFacadeLocal;


@Named(value = "loginMBean")
@SessionScoped
public class LoginMBean implements Serializable {

    @EJB
    private UserFacadeLocal userFacade;

    private String username;
    private String password;
    private String message;
    private User currentUser;

    private String newPassword;
    private String email;
    private boolean userFound = false;
    private User userToReset;

    public LoginMBean() {
    }
    
    // Clear message khi trang được load lại (GET request)
    public void init() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && !facesContext.isPostback()) {
            // Chỉ clear message khi là GET request (F5, reload trang)
            // Không clear khi là POST request (submit form)
            if (message != null && !message.isEmpty()) {
                // Kiểm tra xem có phải là message từ redirect không
                // Nếu không phải redirect, clear message
                String viewId = facesContext.getViewRoot() != null ? 
                    facesContext.getViewRoot().getViewId() : "";
                if (viewId != null && (viewId.contains("login") || viewId.contains("forgotpassword"))) {
                    // Chỉ clear message cũ khi vào trang login/forgotpassword
                    // Giữ lại message mới từ action
                }
            }
        }
    }

    // Đăng nhập
    public String checkUser() {
        try {
            System.out.println("=== LoginMBean.checkUser ===");
            System.out.println("Username: " + username);
            System.out.println("Password: " + (password != null ? "***" : "null"));
            
            // ✅ Tìm user bằng username và password
            User u = userFacade.checkLoginUser(username, password);
            
            if (u == null) {
                System.out.println("User not found or password incorrect");
                message = "❌ Incorrect username or password!";
                return "login";
            }
            
            System.out.println("User found: ID=" + u.getUserID() + ", Name=" + u.getFullName());
            System.out.println("IsActive: " + u.getIsActive());
            
            currentUser = u;
            String roleName = u.getRoleID() != null ? u.getRoleID().getRoleName() : "User";

            // Navigate by role (sundaymarket: admin, customer, shipper)
            switch (roleName.toLowerCase()) {
                case "admin":
                    message = "✅ Welcome Admin!";
                    return "admin-dashboard?faces-redirect=true"; // Admin dashboard
                case "customer":
                    message = "🛒 Welcome Customer!";
                    return "index?faces-redirect=true";
                case "shipper":
                    message = "🚚 Welcome Shipper!";
                    return "shipper-dashboard?faces-redirect=true";
                default:
                    message = "✅ Welcome " + roleName + "!";
                    return "index?faces-redirect=true";
            }
        } catch (Exception e) {
            message = "❌ Login error: " + e.getMessage();
            e.printStackTrace();
            return "login";
        }
    }

    // Đăng xuất
    public String logout() {
        try {
            System.out.println("=== Logout called ===");
            currentUser = null;
            username = "";
            password = "";
            return "/login?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            currentUser = null;
            username = "";
            password = "";
            return "/login?faces-redirect=true";
        }
    }

    // Kiểm tra username và email
    public void checkUsername() {
        try {
            userFound = false;
            userToReset = null;
            message = "";
            
            // Check username
            if (username == null || username.trim().isEmpty()) {
                message = "⚠️ Please enter username!";
                return;
            }
            
            // Check email
            if (email == null || email.trim().isEmpty()) {
                message = "⚠️ Please enter email!";
                return;
            }
            
            // Find user by username
            User u = userFacade.findByUsername(username.trim());
            if (u == null) {
                message = "❌ Username does not exist!";
                return;
            }
            
            // Check if email matches
            String userEmail = u.getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                message = "❌ This account has no registered email!";
                return;
            }
            
            // Compare email (case insensitive)
            if (!userEmail.trim().equalsIgnoreCase(email.trim())) {
                message = "❌ Email does not match the account! Please check again.";
                return;
            }
            
            // Authentication successful
            userFound = true;
            userToReset = u;
            message = "✅ Authentication successful! Please enter new password.";
        } catch (Exception e) {
            e.printStackTrace();
            message = "❌ Error checking: " + e.getMessage();
        }
    }
    
    // Reset password
    public String resetPassword() {
        try {
            if (!userFound || userToReset == null) {
                message = "⚠️ Please verify account first!";
                return null;
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                message = "⚠️ Please enter new password!";
                return null;
            }
            
            userToReset.setPassword(newPassword);
            userFacade.edit(userToReset);
            
            message = "✅ Password reset successful! Please login again.";
            
            // Reset form
            username = "";
            email = "";
            newPassword = "";
            userFound = false;
            userToReset = null;
            
            return "login?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            message = "❌ Error resetting password: " + e.getMessage();
            return null;
        }
    }

    // Hỗ trợ phân quyền
    public boolean isAdmin() {
        return currentUser != null
                && currentUser.getRoleID() != null
                && "admin".equalsIgnoreCase(currentUser.getRoleID().getRoleName());
    }

    public boolean isCustomer() {
        return currentUser != null
                && currentUser.getRoleID() != null
                && "customer".equalsIgnoreCase(currentUser.getRoleID().getRoleName());
    }

    public boolean isShipper() {
        return currentUser != null
                && currentUser.getRoleID() != null
                && "shipper".equalsIgnoreCase(currentUser.getRoleID().getRoleName());
    }
    
    // Kiểm tra xem có phải trang login/register không
    public boolean isLoginOrRegisterPage() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null && facesContext.getViewRoot() != null) {
                String viewId = facesContext.getViewRoot().getViewId();
                return viewId != null && (viewId.contains("/login") || viewId.contains("/register"));
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    // Getter và Setter
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMessage() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String msg = message;
        
        // Nếu là GET request (F5, reload), clear message sau khi đọc
        if (facesContext != null && !facesContext.isPostback() && msg != null && !msg.isEmpty()) {
            // Clear message sau khi đã đọc để không hiển thị lại khi F5
            message = "";
        }
        
        return msg;
    }
    
    // Method để clear message (có thể gọi từ XHTML)
    public void clearMessage() {
        this.message = "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public boolean isUserFound() {
        return userFound;
    }

    public void setUserFound(boolean userFound) {
        this.userFound = userFound;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    // Lấy avatar URL cho current user
    public String getCurrentUserAvatarUrl() {
        if (currentUser == null || currentUser.getAvatar() == null || currentUser.getAvatar().isEmpty()) {
            return null;
        }
        
        String avatar = currentUser.getAvatar();
        String fileName;
        // Nếu là URL tuyệt đối (dữ liệu cũ), extract tên file
        if (avatar.contains("\\") || avatar.contains("/")) {
            File file = new File(avatar);
            fileName = file.getName();
        } else {
            // Dữ liệu mới chỉ là tên file
            fileName = avatar;
        }
        
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        // ✅ Dùng servlet để hiển thị avatar
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            return contextPath + "/images/avatar/" + fileName + "?v=" + (System.currentTimeMillis() % 1000000);
        }
        return "/images/avatar/" + fileName;
    }
}
