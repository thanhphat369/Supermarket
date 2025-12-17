/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author My PC
 */
@Entity
@Table(name = "Promotion_Order_Time")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PromotionOrderTime.findAll", query = "SELECT p FROM PromotionOrderTime p"),
    @NamedQuery(name = "PromotionOrderTime.findByPromotionID", query = "SELECT p FROM PromotionOrderTime p WHERE p.promotionID = :promotionID"),
    @NamedQuery(name = "PromotionOrderTime.findByStartTime", query = "SELECT p FROM PromotionOrderTime p WHERE p.startTime = :startTime"),
    @NamedQuery(name = "PromotionOrderTime.findByEndTime", query = "SELECT p FROM PromotionOrderTime p WHERE p.endTime = :endTime"),
    @NamedQuery(name = "PromotionOrderTime.findByIsActive", query = "SELECT p FROM PromotionOrderTime p WHERE p.isActive = :isActive"),
    @NamedQuery(name = "PromotionOrderTime.findByCreatedAt", query = "SELECT p FROM PromotionOrderTime p WHERE p.createdAt = :createdAt")})
public class PromotionOrderTime implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "Promotion_ID")
    private Integer promotionID;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Start_Time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;
    @Basic(optional = false)
    @NotNull
    @Column(name = "End_Time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;
    @Column(name = "Is_Active")
    private Boolean isActive;
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "Promotion_ID", referencedColumnName = "Promotion_ID", insertable = false, updatable = false)
    @OneToOne(optional = false)
    private Promotions promotions;

    public PromotionOrderTime() {
    }

    public PromotionOrderTime(Integer promotionID) {
        this.promotionID = promotionID;
    }

    public PromotionOrderTime(Integer promotionID, Date startTime, Date endTime) {
        this.promotionID = promotionID;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Integer getPromotionID() {
        return promotionID;
    }

    public void setPromotionID(Integer promotionID) {
        this.promotionID = promotionID;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Promotions getPromotions() {
        return promotions;
    }

    public void setPromotions(Promotions promotions) {
        this.promotions = promotions;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (promotionID != null ? promotionID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PromotionOrderTime)) {
            return false;
        }
        PromotionOrderTime other = (PromotionOrderTime) object;
        if ((this.promotionID == null && other.promotionID != null) || (this.promotionID != null && !this.promotionID.equals(other.promotionID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.PromotionOrderTime[ promotionID=" + promotionID + " ]";
    }
    
}
