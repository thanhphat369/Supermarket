package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import mypack.entity.Role;
import mypack.entity.User;
import mypack.sessionbean.RoleFacadeLocal;
import mypack.sessionbean.UserFacadeLocal;

@Named(value = "userMBean")
@SessionScoped
public class UserMBean implements Serializable {

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private RoleFacadeLocal roleFacade;
    private Part uploadedFile;
    private Integer selectedRoleId;
    private User selected = new User();
    private boolean editMode = false;
    private boolean showForm = false; // Control form visibility
    private String previewImageBase64;
    private String searchKeyword;
    private int currentPage = 1;
    private int pageSize = 5;
    
    // Lấy danh sách người dùng
    public List<User> getItems() {
        try {
            List<User> all = userFacade.findAll();
            
            if (all == null) {
                System.out.println("UserMBean.getItems() - userFacade.findAll() returned null");
                return new java.util.ArrayList<>();
            }
            
            System.out.println("UserMBean.getItems() - Total users from DB: " + all.size());
            
            // Áp dụng tìm kiếm nếu có keyword
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keyword = searchKeyword.trim().toLowerCase();
                List<User> filtered = all.stream()
                        .filter(user -> 
                            (user.getFullName() != null && user.getFullName().toLowerCase().contains(keyword)) ||
                            (user.getUserName() != null && user.getUserName().toLowerCase().contains(keyword)) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(keyword)) ||
                            (user.getPhone() != null && user.getPhone().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
                System.out.println("UserMBean.getItems() - Filtered users: " + filtered.size());
                return filtered;
            }
            
            return all;
        } catch (Exception e) {
            System.err.println("UserMBean.getItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Lấy danh sách người dùng phân trang
    public List<User> getPagedItems() {
        try {
            List<User> base = getItems();
            
            if (base == null || base.isEmpty()) {
                System.out.println("UserMBean.getPagedItems() - Base list is null or empty");
                return new java.util.ArrayList<>();
            }
            
            int start = (currentPage - 1) * pageSize;
            int end = Math.min(start + pageSize, base.size());
            
            if (start >= base.size()) {
                currentPage = 1;
                start = 0;
                end = Math.min(pageSize, base.size());
            }
            
            if (start < 0 || start >= end || end > base.size()) {
                System.out.println("UserMBean.getPagedItems() - Invalid range: start=" + start + ", end=" + end + ", base.size()=" + base.size());
                return new java.util.ArrayList<>();
            }
            
            List<User> result = base.subList(start, end);
            System.out.println("UserMBean.getPagedItems() - Page " + currentPage + ": " + result.size() + " items (start=" + start + ", end=" + end + ")");
            return result;
        } catch (Exception e) {
            System.err.println("UserMBean.getPagedItems() - Error: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    // Tìm kiếm
    public void performSearch() {
        currentPage = 1; // Reset to first page when searching
    }
    
    public void clearSearch() {
        searchKeyword = null;
        currentPage = 1;
    }
    
    // Tổng số trang
    public int getTotalPages() {
        int total = getTotalItems();
        if (total == 0) {
            return 1; // Ít nhất 1 trang để hiển thị
        }
        return (int) Math.ceil((double) total / pageSize);
    }
    
    // Tổng số items
    public int getTotalItems() {
        try {
            List<User> items = getItems();
            return items != null ? items.size() : 0;
        } catch (Exception e) {
            System.err.println("UserMBean.getTotalItems() - Error: " + e.getMessage());
            return 0;
        }
    }
    
    // Tạo mới
    public void prepareCreate() {
        selected = new User();
        selectedRoleId = null;
        editMode = false;
        showForm = true; // Show form when adding new user
        previewImageBase64 = null;
        uploadedFile = null;
    }
    
    // Chỉnh sửa
    public void prepareEdit(User u) {
        // Load user từ database để có đầy đủ thông tin, bao gồm password và avatar
        if (u != null && u.getUserID() != null) {
            selected = userFacade.find(u.getUserID());
            if (selected == null) {
                selected = u; // Fallback nếu không tìm thấy
            }
        } else {
            selected = u;
        }
        
        // Đảm bảo password được load từ database để hiển thị trong form
        if (selected != null && selected.getUserID() != null) {
            User fullUser = userFacade.find(selected.getUserID());
            if (fullUser != null) {
                if (fullUser.getPassword() != null) {
                    // Set password để form có thể hiển thị
                    selected.setPassword(fullUser.getPassword());
                    System.out.println("UserMBean.prepareEdit() - Password loaded for user: " + selected.getUserName());
                }
                // Đảm bảo avatar được load từ database
                if (fullUser.getAvatar() != null) {
                    selected.setAvatar(fullUser.getAvatar());
                    System.out.println("UserMBean.prepareEdit() - Avatar loaded for user: " + selected.getUserName() + " -> " + selected.getAvatar());
                } else {
                    System.out.println("UserMBean.prepareEdit() - No avatar found for user: " + selected.getUserName());
                }
            }
        }
        
        selectedRoleId = (selected.getRoleID() != null) ? selected.getRoleID().getRoleID() : null;
        editMode = true;
        showForm = true; // Show form when editing
        previewImageBase64 = null;
        
        // QUAN TRỌNG: Password đã được load từ database
        // Nếu user không sửa password (để trống hoặc giữ nguyên), password cũ sẽ được giữ nguyên trong save()
    }
    
    // Cancel form (close form)
    public void cancelForm() {
        showForm = false;
        selected = new User();
        selectedRoleId = null;
        editMode = false;
        previewImageBase64 = null;
        uploadedFile = null;
    }
    
    // Getter/Setter for showForm
    public boolean isShowForm() {
        return showForm;
    }
    
    public void setShowForm(boolean showForm) {
        this.showForm = showForm;
    }
    
    // Lưu
    public void save() {
        try {
            // Validate required fields
            if (selected.getUserName() == null || selected.getUserName().trim().isEmpty()) {
                addErr("⚠️ Please enter username!");
                return;
            }
            
            boolean isNewUser = selected.getUserID() == null;
            
            // Xử lý password
            if (isNewUser) {
                // User mới: bắt buộc phải có password
                String password = selected.getPassword();
                if (password == null || password.trim().isEmpty()) {
                    addErr("⚠️ Please enter password!");
                    return;
                }
            } else {
                // Update user: lấy password từ form
                String passwordFromForm = selected.getPassword();
                
                // Nếu password từ form trống hoặc null, lấy password cũ từ database
                if (passwordFromForm == null || passwordFromForm.trim().isEmpty()) {
                    User existingUser = userFacade.find(selected.getUserID());
                    if (existingUser != null && existingUser.getPassword() != null) {
                        // Giữ nguyên password cũ từ database
                        selected.setPassword(existingUser.getPassword());
                    }
                }
                // Nếu có password mới (không trống), dùng password mới (đã được set từ form)
            }
            
            // Set role for user (REQUIRED)
            if (selectedRoleId != null) {
                Role role = roleFacade.find(selectedRoleId);
                if (role != null) {
                    selected.setRoleID(role);
                } else {
                    addErr("⚠️ Invalid role!");
                    return;
                }
            } else {
                // If new user and no role, get default role
                if (isNewUser && selected.getRoleID() == null) {
                    // Find Customer role as default
                    List<Role> allRoles = roleFacade.findAll();
                    for (Role r : allRoles) {
                        if (r.getRoleName() != null && r.getRoleName().equalsIgnoreCase("customer")) {
                            selected.setRoleID(r);
                            break;
                        }
                    }
                    // If no Customer, get first role
                    if (selected.getRoleID() == null && !allRoles.isEmpty()) {
                        selected.setRoleID(allRoles.get(0));
                    }
                }
                
                if (selected.getRoleID() == null) {
                    addErr("⚠️ Please select role!");
                    return;
                }
            }
            
            // Ensure isActive is set (default true if not set)
            // isActive is boolean primitive so no need to check null
            
            // ✅ Lưu flag xem có upload avatar không (trước khi reset uploadedFile)
            boolean hasUploadedAvatar = uploadedFile != null && uploadedFile.getSize() > 0;
            
            // isNewUser đã được khai báo ở đầu method
            if (isNewUser) {
                userFacade.create(selected);
            } else {
                userFacade.edit(selected);
            }
            
            // ✅ Upload avatar nếu có (trước khi refresh)
            String avatarFileName = null;
            if (hasUploadedAvatar) {
                avatarFileName = uploadAvatarFileSilent(); // Upload không hiện thông báo riêng
            }

            // ✅ QUAN TRỌNG: Refresh lại user từ DB để có avatar mới nhất và password đầy đủ
            if (selected.getUserID() != null) {
                User refreshedUser = userFacade.find(selected.getUserID());
                if (refreshedUser != null) {
                    selected = refreshedUser;
                    selectedRoleId = (selected.getRoleID() != null) ? selected.getRoleID().getRoleID() : null;
                    System.out.println("UserMBean.save() - Refreshed user avatar: " + selected.getAvatar());
                    System.out.println("UserMBean.save() - Password kept in form for future edits: " + (selected.getPassword() != null ? "Yes" : "No"));
                }
            }

            // ✅ Clear password field chỉ khi tạo mới user (để form sạch cho user mới tiếp theo)
            // Khi update, giữ nguyên password để có thể chỉnh sửa tiếp mà không cần nhập lại
            if (isNewUser) {
                selected.setPassword("");
            }

            // ✅ Hiển thị thông báo tổng hợp (chuyên nghiệp)
            if (isNewUser) {
                if (hasUploadedAvatar && avatarFileName != null) {
                    addInfo("✅ User created successfully with avatar!");
                } else {
                    addInfo("✅ User created successfully!");
                }
            } else {
                if (hasUploadedAvatar && avatarFileName != null) {
                    addInfo("✅ User information and avatar updated successfully!");
                } else {
                    addInfo("✅ User information updated successfully!");
                }
            }

            // ✅ Close form after successful save
            showForm = false;
            
            // ✅ Only reset when adding new and NO avatar (to keep avatar if uploaded)
            if (isNewUser && !hasUploadedAvatar) {
                selected = new User();
                selectedRoleId = null;
                editMode = false;
                previewImageBase64 = null;
                uploadedFile = null;
            }

            previewImageBase64 = null;
            uploadedFile = null;
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Save failed: " + e.getMessage());
        }
    }

    /**
     * Lấy đường dẫn upload vào thư mục bên ngoài source code
     * 
     * @return Đường dẫn tuyệt đối đến thư mục upload avatar
     */
    private String getUploadDirectory() {
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
    
    // Upload avatar file (returns filename)
    private String uploadAvatarFile() {
        if (uploadedFile == null) {
            System.out.println("UserMBean.uploadAvatarFile() - uploadedFile is NULL!");
            return null;
        }
        
        try {
            System.out.println("UserMBean.uploadAvatarFile() - Starting upload...");
            System.out.println("UserMBean.uploadAvatarFile() - File size: " + uploadedFile.getSize());
            System.out.println("UserMBean.uploadAvatarFile() - File name: " + uploadedFile.getSubmittedFileName());
            
            String uploadDir = getUploadDirectory();
            System.out.println("UserMBean.uploadAvatarFile() - Upload directory: " + uploadDir);
            
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("UserMBean.uploadAvatarFile() - Directory created: " + created + " at: " + dir.getAbsolutePath());
            } else {
                System.out.println("UserMBean.uploadAvatarFile() - Directory already exists: " + dir.getAbsolutePath());
            }
            
            // Check write permission
            if (!dir.canWrite()) {
                System.err.println("UserMBean.uploadAvatarFile() - ERROR: Cannot write to directory: " + dir.getAbsolutePath());
                addErr("❌ No write permission to avatars directory!");
                return null;
            }
            
            String originalFileName = uploadedFile.getSubmittedFileName();
            if (originalFileName == null || originalFileName.isEmpty()) {
                System.out.println("UserMBean.uploadAvatarFile() - No filename provided");
                addErr("❌ Invalid filename!");
                return null;
            }
            
            // Sanitize filename to avoid special characters
            String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                                                   .replaceAll("_{2,}", "_")
                                                   .replaceAll("^_|_$", "");
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            String userName = selected.getUserName() != null ? selected.getUserName() : "user";
            // ✅ SAVE ONLY FILENAME (NOT ABSOLUTE PATH)
            String fileName = "user_" + userName + "_" + timestamp + "_" + sanitizedName;
            File file = new File(dir, fileName);
            
            System.out.println("UserMBean.uploadAvatarFile() - Target file: " + file.getAbsolutePath());
            
            // Upload file to resources/avatars directory
            try (InputStream in = uploadedFile.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                long totalBytes = 0;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    totalBytes += len;
                }
                System.out.println("UserMBean.uploadAvatarFile() - Written " + totalBytes + " bytes to file");
            }
            
            // Verify file was written
            if (!file.exists()) {
                System.err.println("UserMBean.uploadAvatarFile() - ERROR: File was not created!");
                addErr("❌ File was not created successfully!");
                return null;
            }
            
            if (file.length() == 0) {
                System.err.println("UserMBean.uploadAvatarFile() - ERROR: File is empty!");
                addErr("❌ File is empty!");
                return null;
            }
            
            System.out.println("UserMBean.uploadAvatarFile() - ✅ File saved successfully to: " + file.getAbsolutePath());
            System.out.println("UserMBean.uploadAvatarFile() - File size on disk: " + file.length() + " bytes");
            System.out.println("UserMBean.uploadAvatarFile() - File name only (will save to DB): " + fileName);
            
            // ✅ IMPORTANT: SAVE ONLY FILENAME TO DB (NOT ABSOLUTE PATH)
            // Ensure only filename is saved, no path
            // fileName is already just filename, no path
            selected.setAvatar(fileName);
            userFacade.edit(selected);
            
            System.out.println("UserMBean.uploadAvatarFile() - Avatar saved to DB (filename only): " + fileName);
            
            // Verify: Kiểm tra xem DB có lưu đúng tên file không
            User verifyUser = userFacade.find(selected.getUserID());
            if (verifyUser != null) {
                String savedAvatar = verifyUser.getAvatar();
                System.out.println("UserMBean.uploadAvatarFile() - Avatar retrieved from DB: " + savedAvatar);
                
                // Nếu vẫn có đường dẫn (dữ liệu cũ), fix ngay
                if (savedAvatar != null && (savedAvatar.contains("\\") || savedAvatar.contains("/"))) {
                    System.err.println("WARNING: Avatar contains path! Extracting filename only.");
                    // Fix: Extract chỉ tên file
                    String fixedFileName = new File(savedAvatar).getName();
                    verifyUser.setAvatar(fixedFileName);
                    userFacade.edit(verifyUser);
                    System.out.println("UserMBean.uploadAvatarFile() - Fixed avatar to filename only: " + fixedFileName);
                    // Update selected để đồng bộ
                    selected.setAvatar(fixedFileName);
                }
            }

            uploadedFile = null;
            // Không hiện thông báo ở đây, để save() hiện thông báo tổng hợp
            return fileName; // Return filename để save() biết đã upload thành công
        } catch (Exception e) {
            System.err.println("UserMBean.uploadAvatarFile() - EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            addErr("❌ Error uploading image: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Upload avatar file silently (không hiện thông báo riêng)
     * @return filename nếu thành công, null nếu lỗi
     */
    private String uploadAvatarFileSilent() {
        if (uploadedFile == null) {
            System.out.println("UserMBean.uploadAvatarFileSilent() - uploadedFile is NULL!");
            return null;
        }
        
        try {
            System.out.println("UserMBean.uploadAvatarFileSilent() - Starting upload...");
            System.out.println("UserMBean.uploadAvatarFileSilent() - File size: " + uploadedFile.getSize());
            System.out.println("UserMBean.uploadAvatarFileSilent() - File name: " + uploadedFile.getSubmittedFileName());
            
            String uploadDir = getUploadDirectory();
            System.out.println("UserMBean.uploadAvatarFileSilent() - Upload directory: " + uploadDir);
            
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("UserMBean.uploadAvatarFileSilent() - Directory created: " + created + " at: " + dir.getAbsolutePath());
            } else {
                System.out.println("UserMBean.uploadAvatarFileSilent() - Directory already exists: " + dir.getAbsolutePath());
            }
            
            // Check write permission
            if (!dir.canWrite()) {
                System.err.println("UserMBean.uploadAvatarFileSilent() - ERROR: Cannot write to directory: " + dir.getAbsolutePath());
                return null;
            }
            
            String originalFileName = uploadedFile.getSubmittedFileName();
            if (originalFileName == null || originalFileName.isEmpty()) {
                System.out.println("UserMBean.uploadAvatarFileSilent() - No filename provided");
                return null;
            }
            
            // Sanitize filename
            String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                                                   .replaceAll("_{2,}", "_")
                                                   .replaceAll("^_|_$", "");
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            String userName = selected.getUserName() != null ? selected.getUserName() : "user";
            String fileName = "user_" + userName + "_" + timestamp + "_" + sanitizedName;
            File file = new File(dir, fileName);
            
            System.out.println("UserMBean.uploadAvatarFileSilent() - Target file: " + file.getAbsolutePath());
            
            // Upload file
            long totalBytes = 0;
            try (InputStream in = uploadedFile.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    totalBytes += len;
                }
                System.out.println("UserMBean.uploadAvatarFileSilent() - Written " + totalBytes + " bytes to file");
            }
            
            // Verify file was written
            if (!file.exists()) {
                System.err.println("UserMBean.uploadAvatarFileSilent() - ERROR: File was not created!");
                return null;
            }
            
            if (file.length() == 0) {
                System.err.println("UserMBean.uploadAvatarFileSilent() - ERROR: File is empty!");
                return null;
            }
            
            System.out.println("UserMBean.uploadAvatarFileSilent() - ✅ File saved successfully to: " + file.getAbsolutePath());
            System.out.println("UserMBean.uploadAvatarFileSilent() - File size on disk: " + file.length() + " bytes");
            System.out.println("UserMBean.uploadAvatarFileSilent() - File name only (will save to DB): " + fileName);
            
            // Save filename to DB (chỉ lưu tên file, không lưu đường dẫn)
            selected.setAvatar(fileName);
            userFacade.edit(selected);
            
            System.out.println("UserMBean.uploadAvatarFileSilent() - Avatar saved to DB (filename only): " + fileName);
            
            // Verify và fix nếu cần (kiểm tra xem DB có lưu đúng tên file không)
            User verifyUser = userFacade.find(selected.getUserID());
            if (verifyUser != null) {
                String savedAvatar = verifyUser.getAvatar();
                System.out.println("UserMBean.uploadAvatarFileSilent() - Avatar retrieved from DB: " + savedAvatar);
                
                // Nếu vẫn có đường dẫn (dữ liệu cũ), fix ngay
                if (savedAvatar != null && (savedAvatar.contains("\\") || savedAvatar.contains("/"))) {
                    System.err.println("WARNING: Avatar contains path! Extracting filename only.");
                    String fixedFileName = new File(savedAvatar).getName();
                    verifyUser.setAvatar(fixedFileName);
                    userFacade.edit(verifyUser);
                    selected.setAvatar(fixedFileName);
                    System.out.println("UserMBean.uploadAvatarFileSilent() - Fixed avatar to filename only: " + fixedFileName);
                    return fixedFileName;
                }
            }
            
            // Reset uploadedFile sau khi upload thành công
            uploadedFile = null;
            return fileName;
        } catch (Exception e) {
            System.err.println("UserMBean.uploadAvatarFileSilent() - EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Delete
    public void delete(User u) {
        try {
            userFacade.remove(u);
            addInfo("🗑️ User deleted: " + u.getFullName());
            if (getPagedItems().isEmpty() && currentPage > 1) {
                currentPage--;
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Cannot delete: " + e.getMessage());
        }
    }

    // Lấy tên file từ avatar
    private String extractFileName(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }
        
        // ✅ Avatar trong DB CHỈ LÀ TÊN FILE (không phải đường dẫn tuyệt đối)
        // Nếu có đường dẫn (dữ liệu cũ), extract chỉ tên file
        if (avatar.contains("\\") || avatar.contains("/")) {
            // Dữ liệu cũ có đường dẫn tuyệt đối -> extract tên file
            File file = new File(avatar);
            String fileName = file.getName();
            System.out.println("UserMBean.extractFileName() - Extracted filename from path: " + fileName);
            return fileName;
        } else {
            // Dữ liệu mới chỉ là tên file -> dùng luôn
            return avatar;
        }
    }

    // Lấy URL avatar cho selected user
    public String getAvatarUrl() {
        if (selected == null) {
            System.out.println("UserMBean.getAvatarUrl() - selected is null");
            return null;
        }
        
        String avatar = selected.getAvatar();
        if (avatar == null || avatar.trim().isEmpty()) {
            System.out.println("UserMBean.getAvatarUrl() - selected.avatar is null or empty");
            return null;
        }
        
        String fileName = extractFileName(avatar);
        
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("UserMBean.getAvatarUrl() - fileName is null or empty after extraction");
            return null;
        }
        
        // ✅ Dùng servlet để hiển thị avatar
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            String url = contextPath + "/images/avatar/" + fileName + "?v=" + getAvatarCacheBuster();
            System.out.println("UserMBean.getAvatarUrl() - Generated URL: " + url);
            return url;
        }
        
        System.out.println("UserMBean.getAvatarUrl() - FacesContext is null");
        return null;
    }
    
    private long getAvatarCacheBuster() {
        return System.currentTimeMillis() % 1000000;
    }

    // Lấy URL avatar cho user trong danh sách
    public String getAvatarUrlForUser(User user) {
        if (user == null || user.getAvatar() == null || user.getAvatar().isEmpty()) {
            System.out.println("UserMBean.getAvatarUrlForUser() - User or avatar is null");
            return null;
        }

        String avatar = user.getAvatar();
        String fileName;
        // ✅ Avatar trong DB giờ chỉ là tên file (không phải đường dẫn tuyệt đối)
        // Nếu có đường dẫn (dữ liệu cũ), extract tên file
        if (avatar.contains("\\") || avatar.contains("/")) {
            // Dữ liệu cũ có đường dẫn tuyệt đối
            File file = new File(avatar);
            fileName = file.getName();
            System.out.println("UserMBean.getAvatarUrlForUser() - Extracted filename from path: " + fileName);
        } else {
            // Dữ liệu mới chỉ là tên file
            fileName = avatar;
        }

        if (fileName == null || fileName.isEmpty()) {
            System.out.println("UserMBean.getAvatarUrlForUser() - FileName is null or empty");
            return null;
        }

        // ✅ Dùng servlet để hiển thị avatar
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String url;
        if (facesContext != null) {
            String contextPath = facesContext.getExternalContext().getRequestContextPath();
            url = contextPath + "/images/avatar/" + fileName;
            System.out.println("UserMBean.getAvatarUrlForUser() - Generated URL: " + url + " for user: " + user.getUserName());
        } else {
            url = "/images/avatar/" + fileName;
            System.out.println("UserMBean.getAvatarUrlForUser() - FacesContext is null, using: " + url);
        }
        // Thêm cache buster để tránh browser cache (dùng timestamp để force reload)
        long cacheBuster = System.currentTimeMillis();
        String finalUrl = url + "?v=" + cacheBuster;
        System.out.println("UserMBean.getAvatarUrlForUser() - Final URL with cache buster: " + finalUrl);
        return finalUrl;
    }

    
    // Lấy danh sách roles
    public List<Role> getAllRoles() {
        return roleFacade.findAll();
    }
    
    // Phân trang
    public void firstPage() {
        currentPage = 1;
    }
    
    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
        }
    }
    
    public void nextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
        }
    }
    
    public void lastPage() {
        currentPage = getTotalPages();
    }
    
    // Thông báo
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }
    
    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Getter và Setter
    public UserFacadeLocal getUserFacade() {
        return userFacade;
    }
    
    public void setUserFacade(UserFacadeLocal userFacade) {
        this.userFacade = userFacade;
    }
    
    public RoleFacadeLocal getRoleFacade() {
        return roleFacade;
    }
    
    public void setRoleFacade(RoleFacadeLocal roleFacade) {
        this.roleFacade = roleFacade;
    }
    
    public Part getUploadedFile() {
        return uploadedFile;
    }
    
    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
    
    public Integer getSelectedRoleId() {
        return selectedRoleId;
    }
    
    public void setSelectedRoleId(Integer selectedRoleId) {
        this.selectedRoleId = selectedRoleId;
    }
    
    public User getSelected() {
        return selected;
    }
    
    public void setSelected(User selected) {
        this.selected = selected;
    }
    
    public boolean isEditMode() {
        return editMode;
    }
    
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
    
    public String getUploadDir() {
        return getUploadDirectory();
    }
    
    public void setUploadDir(String uploadDir) {
    }
    
    public String getPreviewImageBase64() {
        return previewImageBase64;
    }
    
    public void setPreviewImageBase64(String previewImageBase64) {
        this.previewImageBase64 = previewImageBase64;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public String getSearchKeyword() {
        return searchKeyword;
    }
    
    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }
}
