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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "Delivery_Log")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "DeliveryLog.findAll", query = "SELECT d FROM DeliveryLog d"),
    @NamedQuery(name = "DeliveryLog.findByLogID", query = "SELECT d FROM DeliveryLog d WHERE d.logID = :logID"),
    @NamedQuery(name = "DeliveryLog.findByEventType", query = "SELECT d FROM DeliveryLog d WHERE d.eventType = :eventType"),
    @NamedQuery(name = "DeliveryLog.findByDescription", query = "SELECT d FROM DeliveryLog d WHERE d.description = :description"),
    @NamedQuery(name = "DeliveryLog.findByImagePath", query = "SELECT d FROM DeliveryLog d WHERE d.imagePath = :imagePath"),
    @NamedQuery(name = "DeliveryLog.findByLocation", query = "SELECT d FROM DeliveryLog d WHERE d.location = :location"),
    @NamedQuery(name = "DeliveryLog.findByCreatedAt", query = "SELECT d FROM DeliveryLog d WHERE d.createdAt = :createdAt")})
public class DeliveryLog implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Log_ID")
    private Integer logID;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "Event_Type")
    private String eventType;
    @Size(max = 2147483647)
    @Column(name = "Description")
    private String description;
    @Size(max = 255)
    @Column(name = "Image_Path")
    private String imagePath;
    @Size(max = 255)
    @Column(name = "Location")
    private String location;
    @Column(name = "Created_At")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "Delivery_ID", referencedColumnName = "Delivery_ID")
    @ManyToOne(optional = false)
    private Delivery deliveryID;

    public DeliveryLog() {
    }

    public DeliveryLog(Integer logID) {
        this.logID = logID;
    }

    public DeliveryLog(Integer logID, String eventType) {
        this.logID = logID;
        this.eventType = eventType;
    }

    public Integer getLogID() {
        return logID;
    }

    public void setLogID(Integer logID) {
        this.logID = logID;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Delivery getDeliveryID() {
        return deliveryID;
    }

    public void setDeliveryID(Delivery deliveryID) {
        this.deliveryID = deliveryID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (logID != null ? logID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof DeliveryLog)) {
            return false;
        }
        DeliveryLog other = (DeliveryLog) object;
        if ((this.logID == null && other.logID != null) || (this.logID != null && !this.logID.equals(other.logID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.DeliveryLog[ logID=" + logID + " ]";
    }
    
    /**
     * Lấy tên hiển thị của loại sự kiện giao hàng
     * @return Tên loại sự kiện kèm icon
     */
    public String getEventTypeDisplay() {
        if (eventType == null) return "";
        switch (eventType.toUpperCase()) {
            case "RECEIVED": return "📋 Order Received";      // Nhận đơn hàng
            case "PICKED_UP": return "📦 Picked Up";          // Đã lấy hàng
            case "ARRIVED": return "📍 Arrived at Location";  // Đã tới địa chỉ
            case "DELIVERED": return "✅ Delivered";          // Giao thành công
            case "FAILED": return "❌ Delivery Failed";       // Giao thất bại
            case "NOTE": return "📝 Note";                    // Ghi chú
            default: return eventType;
        }
    }
    
    /**
     * Lấy icon tương ứng với loại sự kiện
     * @return Icon emoji của sự kiện
     */
    public String getEventIcon() {
        if (eventType == null) return "📋";
        switch (eventType.toUpperCase()) {
            case "RECEIVED": return "📋";    // Nhận đơn
            case "PICKED_UP": return "📦";   // Lấy hàng
            case "ARRIVED": return "📍";     // Tới nơi
            case "DELIVERED": return "✅";   // Giao xong
            case "FAILED": return "❌";      // Thất bại
            case "NOTE": return "📝";        // Ghi chú
            default: return "📋";
        }
    }
}
