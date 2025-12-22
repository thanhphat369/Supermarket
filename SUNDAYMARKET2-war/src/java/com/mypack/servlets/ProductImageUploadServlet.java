package com.mypack.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet để upload hình ảnh product vào thư mục upload bên ngoài source code
 * Lưu vào: user.home/sundaymarket/uploads/product
 * URL: /upload/product-images
 */
@WebServlet(name = "ProductImageUploadServlet", urlPatterns = {"/upload/product-images"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,  // 1MB
    maxFileSize = 10 * 1024 * 1024,   // 10MB per file
    maxRequestSize = 50 * 1024 * 1024  // 50MB total
)
public class ProductImageUploadServlet extends HttpServlet {

    /**
     * Lấy đường dẫn upload vào thư mục bên ngoài source code
     */
    private String getUploadDirectory() {
        String path = System.getProperty("user.home")
                + File.separator + "sundaymarket"
                + File.separator + "uploads"
                + File.separator + "product";

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        System.out.println("✅ Product upload dir: " + dir.getAbsolutePath());
        return dir.getAbsolutePath();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        List<String> uploadedFileNames = new ArrayList<>();
        
        try {
            // Validate: 2-5 images
            List<Part> fileParts = new ArrayList<>();
            String productName = request.getParameter("productName");
            if (productName == null || productName.trim().isEmpty()) {
                productName = "product";
            }
            
            // Collect all file parts
            for (Part part : request.getParts()) {
                if (part != null && part.getSize() > 0) {
                    String submittedFileName = part.getSubmittedFileName();
                    String contentType = part.getContentType();
                    
                    // Only accept image files
                    if (submittedFileName != null && 
                        !submittedFileName.isEmpty() && 
                        contentType != null && 
                        contentType.startsWith("image/")) {
                        fileParts.add(part);
                    }
                }
            }
            
            // Validate count
            if (fileParts.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("[\"ERROR: No image files provided\"]");
                return;
            }
            
            // Check if editing existing product (has productId parameter)
            String productIdParam = request.getParameter("productId");
            boolean isEditing = productIdParam != null && !productIdParam.trim().isEmpty();
            
            // For new products, require at least 2 images
            // For editing, allow 1+ images (will be merged with existing)
            if (!isEditing && fileParts.size() < 2) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("[\"ERROR: Please select at least 2 images\"]");
                return;
            }
            
            if (fileParts.size() > 5) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("[\"ERROR: Maximum 5 images allowed\"]");
                return;
            }
            
            // Get upload directory
            String uploadDir = getUploadDirectory();
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            if (!dir.canWrite()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("[\"ERROR: No write permission to images directory\"]");
                return;
            }
            
            // Upload each file
            String timestamp = String.valueOf(System.currentTimeMillis());
            String sanitizedProductName = productName.replaceAll("[^a-zA-Z0-9]", "_");
            
            for (Part filePart : fileParts) {
                String originalFileName = filePart.getSubmittedFileName();
                
                // Sanitize filename
                String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                                                       .replaceAll("_{2,}", "_")
                                                       .replaceAll("^_|_$", "");
                
                String fileName = "product_" + sanitizedProductName + "_" + timestamp + "_" + System.nanoTime() + "_" + sanitizedName;
                File file = new File(dir, fileName);
                
                // Upload file
                try (InputStream in = filePart.getInputStream();
                     FileOutputStream outStream = new FileOutputStream(file)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        outStream.write(buf, 0, len);
                    }
                }
                
                if (file.exists() && file.length() > 0) {
                    uploadedFileNames.add(fileName);
                    System.out.println("ProductImageUploadServlet - ✅ Uploaded: " + fileName);
                }
            }
            
            if (uploadedFileNames.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("[\"ERROR: No files were uploaded successfully\"]");
                return;
            }
            
            // Return JSON array of file names
            StringBuilder jsonArray = new StringBuilder("[");
            for (int i = 0; i < uploadedFileNames.size(); i++) {
                if (i > 0) {
                    jsonArray.append(",");
                }
                // Escape quotes in filename and wrap in quotes
                String escaped = uploadedFileNames.get(i).replace("\"", "\\\"").replace("\\", "\\\\");
                jsonArray.append("\"").append(escaped).append("\"");
            }
            jsonArray.append("]");
            
            System.out.println("ProductImageUploadServlet - ✅ Uploaded " + uploadedFileNames.size() + " images, JSON: " + jsonArray.toString());
            out.print(jsonArray.toString());
            
        } catch (Exception e) {
            System.err.println("ProductImageUploadServlet - ERROR: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
            out.print("[\"ERROR: " + errorMsg + "\"]");
        }
    }
}
