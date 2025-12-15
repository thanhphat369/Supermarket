package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import mypack.entity.Role;
import mypack.entity.User;
import mypack.sessionbean.RoleFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "userBean")
@RequestScoped
public class RegisterMBean implements Serializable {

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private RoleFacadeLocal roleFacade;

    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String password;
    private String confirmPassword;
    private String role;
    private String message;

    public RegisterMBean() {
    }

    public String register() {
        try {
            System.out.println("=== RegisterMBean.register() START ===");
            
            // Validate
            if (username == null || username.trim().isEmpty()) {
                addError("⚠️ Please enter username!");
                return null;
            }

            if (password == null || password.trim().isEmpty()) {
                addError("⚠️ Please enter password!");
                return null;
            }
            
            if (password.length() < 6) {
                addError("⚠️ Password must be at least 6 characters!");
                return null;
            }

            if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
                addError("⚠️ Please confirm password!");
                return null;
            }
            
            if (!password.equals(confirmPassword)) {
                addError("❌ Password confirmation does not match!");
                return null;
            }

            // Check if username already exists
            System.out.println("Checking if username exists: " + username.trim());
            User existingUser = userFacade.findByUsername(username.trim());
            if (existingUser != null) {
                System.out.println("Username already exists!");
                addError("❌ Username already exists! Please choose another.");
                return null;
            }

            // Tạo user mới
            User newUser = new User();
            newUser.setUserName(username.trim());
            newUser.setPassword(password.trim()); // Trim password để loại bỏ khoảng trắng
            newUser.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : null);
            newUser.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
            newUser.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
            newUser.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
            newUser.setIsActive(true); // Mặc định active

            System.out.println("User created: " + newUser.getUserName());
            System.out.println("Password length: " + (newUser.getPassword() != null ? newUser.getPassword().length() : 0));

            // Set role
            Role userRole = null;
            System.out.println("Role from form: " + role);
            
            if (role != null && !role.isEmpty()) {
                // Tìm role theo tên (customer, shipper, admin)
                java.util.List<Role> allRoles = roleFacade.findAll();
                System.out.println("Total roles in DB: " + allRoles.size());
                
                for (Role r : allRoles) {
                    System.out.println("  - Role ID: " + r.getRoleID() + ", Name: " + r.getRoleName());
                    if (r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(role)) {
                        userRole = r;
                        System.out.println("Found role by name: " + r.getRoleName());
                        break;
                    }
                }
                
                // Nếu không tìm thấy, tìm role có ID tương ứng
                if (userRole == null) {
                    try {
                        Integer roleId = Integer.parseInt(role);
                        userRole = roleFacade.find(roleId);
                        if (userRole != null) {
                            System.out.println("Found role by ID: " + roleId);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }

            // Nếu không tìm thấy role, mặc định là Customer
            if (userRole == null) {
                System.out.println("Role not found, trying to find Customer role...");
                java.util.List<Role> allRoles = roleFacade.findAll();
                for (Role r : allRoles) {
                    if (r.getRoleName() != null && r.getRoleName().equalsIgnoreCase("customer")) {
                        userRole = r;
                        System.out.println("Found Customer role: " + r.getRoleID());
                        break;
                    }
                }
                // Nếu vẫn không có, lấy role đầu tiên
                if (userRole == null && !allRoles.isEmpty()) {
                    userRole = allRoles.get(0);
                    System.out.println("Using first role: " + userRole.getRoleID() + " - " + userRole.getRoleName());
                }
            }

            if (userRole == null) {
                System.out.println("ERROR: No role found in database!");
                addError("❌ No suitable role found! Please contact administrator.");
                return null;
            }

            newUser.setRoleID(userRole);
            System.out.println("User role set: " + userRole.getRoleID() + " - " + userRole.getRoleName());

            // Validate user entity before saving
            if (newUser.getUserName() == null || newUser.getUserName().trim().isEmpty()) {
                addError("❌ Invalid username!");
                return null;
            }
            if (newUser.getPassword() == null || newUser.getPassword().trim().isEmpty()) {
                addError("❌ Invalid password!");
                return null;
            }
            if (newUser.getRoleID() == null) {
                addError("❌ Invalid role!");
                return null;
            }

            System.out.println("Attempting to persist user...");
            System.out.println("User details before persist:");
            System.out.println("  - UserName: " + newUser.getUserName());
            System.out.println("  - Password: " + (newUser.getPassword() != null ? "***" : "null"));
            System.out.println("  - FullName: " + newUser.getFullName());
            System.out.println("  - Email: " + newUser.getEmail());
            System.out.println("  - Phone: " + newUser.getPhone());
            System.out.println("  - Address: " + newUser.getAddress());
            System.out.println("  - IsActive: " + newUser.getIsActive());
            System.out.println("  - RoleID: " + (newUser.getRoleID() != null ? newUser.getRoleID().getRoleID() + " - " + newUser.getRoleID().getRoleName() : "null"));
            
            // Double check username doesn't exist (avoid race condition)
            User duplicateCheck = userFacade.findByUsername(newUser.getUserName().trim());
            if (duplicateCheck != null) {
                System.out.println("ERROR: Username already exists (race condition detected)!");
                addError("❌ Username already exists! Please choose another.");
                return null;
            }
            
            // Save user
            try {
                userFacade.create(newUser);
                System.out.println("User persisted successfully!");
            } catch (jakarta.persistence.PersistenceException pe) {
                // If error is due to duplicate, check again
                Throwable cause = pe.getCause();
                if (cause != null && (cause.getMessage().contains("duplicate") || 
                                     cause.getMessage().contains("unique") ||
                                     cause.getMessage().contains("UNIQUE"))) {
                    addError("❌ Username already exists! Please choose another.");
                    return null;
                }
                throw pe; // Re-throw if not duplicate
            }

            addInfo("✅ Registration successful! Please login.");
            
            // Reset form
            username = null;
            fullName = null;
            email = null;
            phone = null;
            address = null;
            password = null;
            confirmPassword = null;
            role = null;

            System.out.println("=== RegisterMBean.register() SUCCESS ===");
            return "login?faces-redirect=true";
        } catch (jakarta.persistence.PersistenceException e) {
            System.err.println("=== PersistenceException ===");
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause != null) {
                System.err.println("Cause: " + cause.getMessage());
                if (cause.getCause() != null) {
                    System.err.println("Cause of cause: " + cause.getCause().getMessage());
                }
            }
            addError("❌ Registration error: " + (cause != null ? cause.getMessage() : e.getMessage()));
            return null;
        } catch (jakarta.validation.ConstraintViolationException e) {
            System.err.println("=== ConstraintViolationException ===");
            e.printStackTrace();
            StringBuilder msg = new StringBuilder("❌ Invalid data: ");
            for (jakarta.validation.ConstraintViolation<?> violation : e.getConstraintViolations()) {
                msg.append(violation.getMessage()).append("; ");
            }
            addError(msg.toString());
            return null;
        } catch (Exception e) {
            System.err.println("=== General Exception ===");
            e.printStackTrace();
            Throwable cause = e.getCause();
            String errorMsg = e.getMessage();
            if (cause != null) {
                errorMsg = cause.getMessage();
                System.err.println("Cause: " + errorMsg);
            }
            addError("❌ Registration error: " + errorMsg);
            return null;
        }
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
        this.message = msg;
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
        this.message = msg;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

