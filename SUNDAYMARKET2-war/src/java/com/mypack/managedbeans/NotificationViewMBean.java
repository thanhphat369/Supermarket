package com.mypack.managedbeans;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import mypack.entity.NotificationTarget;
import mypack.entity.Notifications;
import mypack.entity.User;
import mypack.sessionbean.NotificationTargetFacadeLocal;

@Named(value = "notificationViewMBean")
@SessionScoped
public class NotificationViewMBean implements Serializable {

    @EJB
    private NotificationTargetFacadeLocal notificationTargetFacade;
    
    @Inject
    private LoginMBean loginMBean;
    
    private List<NotificationTarget> userNotifications;
    private boolean showNotificationPanel = false;
    
    public NotificationViewMBean() {
        // Ensure panel starts closed
        showNotificationPanel = false;
    }
    
    @jakarta.annotation.PostConstruct
    public void init() {
        // Ensure panel is closed on initialization
        showNotificationPanel = false;
    }
    
    public List<NotificationTarget> getUserNotifications() {
        if (userNotifications == null) {
            loadUserNotifications();
        }
        return userNotifications;
    }
    
    public void loadUserNotifications() {
        try {
            User currentUser = loginMBean.getCurrentUser();
            if (currentUser == null || currentUser.getUserID() == null) {
                userNotifications = new ArrayList<>();
                return;
            }
            
            // Lấy tất cả notification targets của user
            List<NotificationTarget> allTargets = notificationTargetFacade.findAll();
            userNotifications = new ArrayList<>();
            
            for (NotificationTarget target : allTargets) {
                if (target.getUserID() != null && 
                    target.getUserID().getUserID() != null &&
                    target.getUserID().getUserID().equals(currentUser.getUserID())) {
                    // Refresh from database to get latest isRead value
                    NotificationTarget refreshed = notificationTargetFacade.find(target.getTargetID());
                    if (refreshed != null) {
                        userNotifications.add(refreshed);
                    } else {
                        userNotifications.add(target);
                    }
                }
            }
            
            // Sắp xếp theo thời gian tạo (mới nhất trước)
            Collections.sort(userNotifications, new Comparator<NotificationTarget>() {
                @Override
                public int compare(NotificationTarget t1, NotificationTarget t2) {
                    Notifications n1 = t1.getNotificationID();
                    Notifications n2 = t2.getNotificationID();
                    if (n1 == null || n1.getCreatedAt() == null) return 1;
                    if (n2 == null || n2.getCreatedAt() == null) return -1;
                    return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            userNotifications = new ArrayList<>();
        }
    }
    
    public int getUnreadCount() {
        try {
            List<NotificationTarget> notifications = getUserNotifications();
            int unreadCount = 0;
            for (NotificationTarget target : notifications) {
                if (target.getIsRead() == null || !target.getIsRead()) {
                    unreadCount++;
                }
            }
            return unreadCount;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public void toggleNotificationPanel() {
        // Only toggle if user explicitly clicked (handled by JavaScript)
        // Always refresh notifications when toggling
        loadUserNotifications();
        
        // Set to true when toggling - JavaScript will handle closing for other actions
        showNotificationPanel = !showNotificationPanel;
        
        // Don't auto-mark as read - let user click to mark individually
    }
    
    /**
     * Reset panel state - called after AJAX requests to ensure panel doesn't auto-open
     */
    public void resetPanelState() {
        showNotificationPanel = false;
    }
    
    /**
     * Mark a specific notification as read when user clicks on it
     * Uses database column IsRead
     */
    public void markAsRead(NotificationTarget target) {
        if (target == null || target.getTargetID() == null) {
            return;
        }
        
        try {
            // Only mark as read if not already read
            if (target.getIsRead() == null || !target.getIsRead()) {
                // Refresh from database to get latest state
                NotificationTarget refreshed = notificationTargetFacade.find(target.getTargetID());
                if (refreshed != null) {
                    refreshed.setIsRead(true);
                    notificationTargetFacade.edit(refreshed);
                    // Force refresh to update UI immediately
                    userNotifications = null;
                    loadUserNotifications();
                    System.out.println("NotificationViewMBean.markAsRead() - Marked notification " + target.getTargetID() + " as read. Unread count: " + getUnreadCount());
                }
            }
        } catch (Exception e) {
            System.err.println("NotificationViewMBean.markAsRead() - Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Mark a specific notification as unread (for re-reading)
     * Uses database column IsRead
     */
    public void markAsUnread(NotificationTarget target) {
        if (target == null || target.getTargetID() == null) {
            return;
        }
        
        try {
            // Refresh from database to get latest state
            NotificationTarget refreshed = notificationTargetFacade.find(target.getTargetID());
            if (refreshed != null) {
                refreshed.setIsRead(false);
                notificationTargetFacade.edit(refreshed);
                // Force refresh to update UI immediately
                userNotifications = null;
                loadUserNotifications();
                System.out.println("NotificationViewMBean.markAsUnread() - Marked notification " + target.getTargetID() + " as unread. Unread count: " + getUnreadCount());
            }
        } catch (Exception e) {
            System.err.println("NotificationViewMBean.markAsUnread() - Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if notification is unread
     * Uses database column IsRead
     */
    public boolean isUnread(NotificationTarget target) {
        if (target == null) {
            return false;
        }
        return target.getIsRead() == null || !target.getIsRead();
    }
    
    public void closeNotificationPanel() {
        showNotificationPanel = false;
    }
    
    // Method để refresh notifications (có thể gọi từ AJAX polling)
    public void refreshNotifications() {
        userNotifications = null; // Reset để force reload
        loadUserNotifications();
    }
    
    public boolean isShowNotificationPanel() {
        return showNotificationPanel;
    }
    
    public void setShowNotificationPanel(boolean showNotificationPanel) {
        this.showNotificationPanel = showNotificationPanel;
    }
    
    public String formatNotificationTime(java.util.Date date) {
        if (date == null) {
            return "";
        }
        
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day(s) ago";
        } else if (hours > 0) {
            return hours + " hour(s) ago";
        } else if (minutes > 0) {
            return minutes + " minute(s) ago";
        } else {
            return "Just now";
        }
    }
    
    public String getProductImageUrl(Notifications notification) {
        if (notification == null || notification.getContent() == null) {
            return null;
        }
        
        String content = notification.getContent();
        // Look for [PRODUCT_IMAGE:...] pattern
        int startIndex = content.indexOf("[PRODUCT_IMAGE:");
        if (startIndex == -1) {
            return null;
        }
        
        int endIndex = content.indexOf("]", startIndex);
        if (endIndex == -1) {
            return null;
        }
        
        String imageUrl = content.substring(startIndex + "[PRODUCT_IMAGE:".length(), endIndex);
        return imageUrl.trim();
    }
    
    public String getFormattedContent(Notifications notification) {
        if (notification == null || notification.getContent() == null) {
            return "";
        }
        
        String content = notification.getContent();
        // Remove [PRODUCT_IMAGE:...] tag
        content = content.replaceAll("\\[PRODUCT_IMAGE:[^\\]]+\\]", "");
        // Remove [PRODUCT_ID:...] tag
        content = content.replaceAll("\\[PRODUCT_ID:[^\\]]+\\]", "");
        // Remove product info lines (📦 Product:, 🖼️/🏞️ Image:, 💰 Price:)
        // Handle both inline (no newline) and multiline formats
        content = content.replaceAll("\\s*📦\\s*Product:[^\\n]*", "");
        content = content.replaceAll("\\s*[🖼️🏞️]\\s*Image:[^\\n]*", "");
        content = content.replaceAll("\\s*💰\\s*Price:[^\\n]*", "");
        // Also handle newline-separated versions
        content = content.replaceAll("\\n\\s*📦\\s*Product:.*", "");
        content = content.replaceAll("\\n\\s*[🖼️🏞️]\\s*Image:.*", "");
        content = content.replaceAll("\\n\\s*💰\\s*Price:.*", "");
        // Clean up multiple newlines and spaces
        content = content.replaceAll("\\n{3,}", "\n\n");
        content = content.replaceAll("\\s{2,}", " ");
        return content.trim();
    }
}
