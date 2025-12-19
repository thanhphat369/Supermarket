package com.mypack.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Servlet để hiển thị hình ảnh avatar từ thư mục upload
 * URL: /images/avatar/*
 */
@WebServlet("/images/avatar/*")
public class AvatarImageServlet extends HttpServlet {

    private static final String BASE_DIR =
            System.getProperty("user.home")
            + File.separator + "sundaymarket"
            + File.separator + "uploads"
            + File.separator + "avatar";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String fileName = req.getPathInfo();
        if (fileName == null || fileName.length() <= 1) {
            resp.sendError(404);
            return;
        }

        fileName = fileName.substring(1);
        File file = new File(BASE_DIR, fileName);

        if (!file.exists()) {
            resp.sendError(404);
            return;
        }

        resp.setContentType(Files.probeContentType(file.toPath()));
        resp.setContentLengthLong(file.length());

        try (InputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            in.transferTo(out);
        }
    }
}
