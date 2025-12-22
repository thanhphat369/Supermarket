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
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "System_Settings")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "SystemSettings.findAll", query = "SELECT s FROM SystemSettings s"),
    @NamedQuery(name = "SystemSettings.findBySettingID", query = "SELECT s FROM SystemSettings s WHERE s.settingID = :settingID"),
    @NamedQuery(name = "SystemSettings.findBySettingKey", query = "SELECT s FROM SystemSettings s WHERE s.settingKey = :settingKey"),
    @NamedQuery(name = "SystemSettings.findBySettingValue", query = "SELECT s FROM SystemSettings s WHERE s.settingValue = :settingValue"),
    @NamedQuery(name = "SystemSettings.findByUpdatedAt", query = "SELECT s FROM SystemSettings s WHERE s.updatedAt = :updatedAt")})
public class SystemSettings implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Setting_ID")
    private Integer settingID;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Setting_Key")
    private String settingKey;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Setting_Value")
    private BigDecimal settingValue;
    @Column(name = "Updated_At")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @JoinColumn(name = "Updated_By", referencedColumnName = "User_ID")
    @ManyToOne
    private User updatedBy;

    public SystemSettings() {
    }

    public SystemSettings(Integer settingID) {
        this.settingID = settingID;
    }

    public SystemSettings(Integer settingID, String settingKey, BigDecimal settingValue) {
        this.settingID = settingID;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    public Integer getSettingID() {
        return settingID;
    }

    public void setSettingID(Integer settingID) {
        this.settingID = settingID;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public BigDecimal getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(BigDecimal settingValue) {
        this.settingValue = settingValue;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (settingID != null ? settingID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SystemSettings)) {
            return false;
        }
        SystemSettings other = (SystemSettings) object;
        if ((this.settingID == null && other.settingID != null) || (this.settingID != null && !this.settingID.equals(other.settingID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.SystemSettings[ settingID=" + settingID + " ]";
    }
    
}
