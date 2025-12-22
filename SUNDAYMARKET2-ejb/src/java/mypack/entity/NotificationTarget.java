/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "Notification_Target")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "NotificationTarget.findAll", query = "SELECT n FROM NotificationTarget n"),
    @NamedQuery(name = "NotificationTarget.findByTargetID", query = "SELECT n FROM NotificationTarget n WHERE n.targetID = :targetID")})
public class NotificationTarget implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Target_ID")
    private Integer targetID;
    @JoinColumn(name = "Notification_ID", referencedColumnName = "Notification_ID")
    @ManyToOne(optional = false)
    private Notifications notificationID;
    @JoinColumn(name = "User_ID", referencedColumnName = "User_ID")
    @ManyToOne(optional = false)
    private User userID;
    @Basic(optional = true)
    @Column(name = "IsRead", nullable = true)
    private Boolean isRead;

    public NotificationTarget() {
        this.isRead = false; // Default to unread
    }

    public NotificationTarget(Integer targetID) {
        this.targetID = targetID;
    }

    public Integer getTargetID() {
        return targetID;
    }

    public void setTargetID(Integer targetID) {
        this.targetID = targetID;
    }

    public Notifications getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(Notifications notificationID) {
        this.notificationID = notificationID;
    }

    public User getUserID() {
        return userID;
    }

    public void setUserID(User userID) {
        this.userID = userID;
    }

    public Boolean getIsRead() {
        return isRead != null ? isRead : false;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (targetID != null ? targetID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof NotificationTarget)) {
            return false;
        }
        NotificationTarget other = (NotificationTarget) object;
        if ((this.targetID == null && other.targetID != null) || (this.targetID != null && !this.targetID.equals(other.targetID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.NotificationTarget[ targetID=" + targetID + " ]";
    }
    
}
