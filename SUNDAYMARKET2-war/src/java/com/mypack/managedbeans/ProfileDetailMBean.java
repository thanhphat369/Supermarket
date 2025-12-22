package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import mypack.entity.User;
import mypack.sessionbean.RoleFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "profileDetailMBean")
@SessionScoped
public class ProfileDetailMBean implements Serializable {

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private RoleFacadeLocal roleFacade;

    @Inject
    private LoginMBean loginMBean;

    private boolean editMode = false;
    private Part uploadedFile;
    private Integer selectedRoleId;
    private String previewImageBase64;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
    private boolean changePasswordMode = false;
    
    // ============================================
    //             ĐỔI MẬT KHẨU
    // ============================================
    public void changePassword() {
        try {
            User user = loginMBean.getCurrentUser();
            if (user == null) {
                addErr("❌ User information not found!");
                return;
            }

            if (oldPassword == null || oldPassword.isEmpty()) {
                addErr("⚠️ Vui lòng nhập mật khẩu cũ!");
                return;
            }

            if (!oldPassword.equals(user.getPassword())) {
                addErr("❌ Old password is incorrect!");
                return;
            }

            if (newPassword == null || newPassword.length() < 6) {
                addErr("⚠️ Mật khẩu mới phải có ít nhất 6 ký tự!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                addErr("❌ Mật khẩu xác nhận không khớp!");
                return;
            }

            user.setPassword(newPassword);
            userFacade.edit(user);

            oldPassword = null;
            newPassword = null;
            confirmPassword = null;
            changePasswordMode = false;

            addInfo("✅ Password changed successfully!");
        } catch (Exception e) {
            addErr("❌ Lỗi khi đổi mật khẩu: " + e.getMessage());
        }
    }

    // ============================================
    //             UPLOAD AVATAR
    // ============================================
    /**
     * Lấy đường dẫn upload vào thư mục bên ngoài source code
     * 
     * @return Đường dẫn tuyệt đối đến thư mục upload avatar
     */
    private String getUploadDir() {
        String path = System.getProperty("user.home")
                + File.separator + "sundaymarket"
                + File.separator + "uploads"
                + File.separator + "avatar";

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        System.out.println("✅ Avatar upload dir: " + dir.getAbsolutePath());
        return dir.getAbsolutePath();
    }
    
    public void uploadAvatarFile() {
        if (uploadedFile == null) {
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - uploadedFile is NULL!");
            return;
        }

        try {
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - Starting upload...");
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - File size: " + uploadedFile.getSize());
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - File name: " + uploadedFile.getSubmittedFileName());
            
            User user = loginMBean.getCurrentUser();
            if (user == null) {
                addErr("❌ Không tìm thấy thông tin người dùng!");
                return;
            }

            String uploadDir = getUploadDir();
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - Upload directory: " + uploadDir);
            
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("ProfileDetailMBean.uploadAvatarFile() - Directory created: " + created + " at: " + dir.getAbsolutePath());
            } else {
                System.out.println("ProfileDetailMBean.uploadAvatarFile() - Directory already exists: " + dir.getAbsolutePath());
            }
            
            // Kiểm tra quyền ghi
            if (!dir.canWrite()) {
                System.err.println("ProfileDetailMBean.uploadAvatarFile() - ERROR: Cannot write to directory: " + dir.getAbsolutePath());
                addErr("❌ Không có quyền ghi vào thư mục avatars!");
                return;
            }

            String originalFileName = uploadedFile.getSubmittedFileName();
            if (originalFileName == null || originalFileName.isEmpty()) {
                System.out.println("ProfileDetailMBean.uploadAvatarFile() - No filename provided");
                addErr("❌ Tên file không hợp lệ!");
                return;
            }

            String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                                                   .replaceAll("_{2,}", "_")
                                                   .replaceAll("^_|_$", "");

            String timestamp = String.valueOf(System.currentTimeMillis());
            String userName = user.getUserName() != null ? user.getUserName() : "user";
            String fileName = "user_" + userName + "_" + timestamp + "_" + sanitizedName;
            File file = new File(dir, fileName);
            
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - Target file: " + file.getAbsolutePath());

            try (InputStream in = uploadedFile.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                long totalBytes = 0;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    totalBytes += len;
                }
                System.out.println("ProfileDetailMBean.uploadAvatarFile() - Written " + totalBytes + " bytes to file");
            }
            
            // Verify file was written
            if (!file.exists()) {
                System.err.println("ProfileDetailMBean.uploadAvatarFile() - ERROR: File was not created!");
                addErr("❌ File could not be created!");
                return;
            }
            
            if (file.length() == 0) {
                System.err.println("ProfileDetailMBean.uploadAvatarFile() - ERROR: File is empty!");
                addErr("❌ File is empty!");
                return;
            }

            user.setAvatar(fileName);
            userFacade.edit(user);

            System.out.println("ProfileDetailMBean.uploadAvatarFile() - ✅ File saved successfully to: " + file.getAbsolutePath());
            System.out.println("ProfileDetailMBean.uploadAvatarFile() - File size on disk: " + file.length() + " bytes");
            addInfo("📸 Profile image updated at: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("ProfileDetailMBean.uploadAvatarFile() - EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            addErr("❌ Error uploading image: " + e.getMessage());
        }
    }
public void saveProfile() {
    try {
        User user = loginMBean.getCurrentUser();
        if (user == null) {
            addErr("❌ User not found!");
            return;
        }

        // Xử lý đổi mật khẩu nếu có
        if (oldPassword != null && !oldPassword.trim().isEmpty()) {
            if (!oldPassword.equals(user.getPassword())) {
                addErr("❌ Old password is incorrect!");
                return;
            }
            
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (newPassword.length() < 6) {
                    addErr("⚠️ Mật khẩu mới phải có ít nhất 6 ký tự!");
                    return;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    addErr("❌ Mật khẩu xác nhận không khớp!");
                    return;
                }
                
                user.setPassword(newPassword);
                oldPassword = null;
                newPassword = null;
                confirmPassword = null;
            }
        }

        // Lưu thông tin người dùng (fullName, email, phone, address)
        userFacade.edit(user);

        // Nếu có file avatar mới → upload
        if (uploadedFile != null && uploadedFile.getSize() > 0) {
            uploadAvatarFile();
        }

        // Refresh user từ DB để có dữ liệu mới nhất
        if (user.getUserID() != null) {
            User refreshedUser = userFacade.find(user.getUserID());
            if (refreshedUser != null) {
                loginMBean.setCurrentUser(refreshedUser);
            }
        }

        this.editMode = false;
            addInfo("💾 Information saved successfully!");

    } catch (Exception e) {
        e.printStackTrace();
        addErr("❌ Lỗi khi lưu thông tin: " + e.getMessage());
    }
}

    // ============================================
    //             THÔNG BÁO
    // ============================================
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // ============================================
    //             GETTERS / SETTERS CHUNG
    // ============================================
    public void enableEdit() {
        this.editMode = true;
        // Tự động lấy mật khẩu hiện tại để điền vào trường "Mật khẩu cũ"
        User user = loginMBean.getCurrentUser();
        if (user != null && user.getUserID() != null) {
            // Refresh user từ database để đảm bảo có mật khẩu mới nhất
            User refreshedUser = userFacade.find(user.getUserID());
            if (refreshedUser != null && refreshedUser.getPassword() != null) {
                this.oldPassword = refreshedUser.getPassword();
            }
        }
        // Reset các trường mật khẩu mới
        this.newPassword = null;
        this.confirmPassword = null;
    }

public void disableEdit() {
    this.editMode = false;
}
public void cancelEdit() {
    this.editMode = false;
    // Reset các trường mật khẩu khi hủy
    this.oldPassword = null;
    this.newPassword = null;
    this.confirmPassword = null;
    this.uploadedFile = null;
}

    public UserFacadeLocal getUserFacade() { return userFacade; }
    public void setUserFacade(UserFacadeLocal userFacade) { this.userFacade = userFacade; }

    public RoleFacadeLocal getRoleFacade() { return roleFacade; }
    public void setRoleFacade(RoleFacadeLocal roleFacade) { this.roleFacade = roleFacade; }

    public boolean isEditMode() { return editMode; }
    public void setEditMode(boolean editMode) { this.editMode = editMode; }

    public Part getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(Part uploadedFile) { this.uploadedFile = uploadedFile; }

    public Integer getSelectedRoleId() { return selectedRoleId; }
    public void setSelectedRoleId(Integer selectedRoleId) { this.selectedRoleId = selectedRoleId; }

    public String getPreviewImageBase64() { return previewImageBase64; }
    public void setPreviewImageBase64(String previewImageBase64) { this.previewImageBase64 = previewImageBase64; }

    public LoginMBean getLoginMBean() { return loginMBean; }
    public void setLoginMBean(LoginMBean loginMBean) { this.loginMBean = loginMBean; }

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public boolean isChangePasswordMode() { return changePasswordMode; }
    public void setChangePasswordMode(boolean changePasswordMode) { this.changePasswordMode = changePasswordMode; }

    // Getter cho uploadDir (để tương thích với code cũ, nhưng sẽ trả về đường dẫn động)
    // Lưu ý: Method getUploadDir() private ở trên đã xử lý logic
    public String getUploadDirProperty() { 
        return getUploadDir();
    }

    // ============================================
    //         FIX: PROPERTY currentUserAvatarUrl
    // ============================================
    

    public String getCurrentUserAvatarUrl() {
        User user = loginMBean.getCurrentUser();
        if (user == null) {
            return null; // ✅ Trả về null thay vì default avatar
        }

        String avatar = user.getAvatar();
        if (avatar == null || avatar.isEmpty()) {
            return null; // ✅ Trả về null khi không có avatar
        }

        String fileName;

        if (avatar.contains("/") || avatar.contains("\\")) {
            fileName = new File(avatar).getName();
        } else {
            fileName = avatar;
        }

        if (fileName == null || fileName.isEmpty()) {
            return null; // ✅ Trả về null khi không có tên file
        }

        // ✅ Tạo URL đầy đủ với context path
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            String base = ctx.getExternalContext().getRequestContextPath();
            return base + "/images/avatar/" + fileName + "?v=" + (System.currentTimeMillis() % 1000000);
        }

        return "/images/avatar/" + fileName;
    }

}
