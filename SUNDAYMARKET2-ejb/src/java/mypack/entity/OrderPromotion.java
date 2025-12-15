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
import jakarta.persistence.ManyToOne;
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
 * @author MC
 */
@Entity
@Table(name = "Order_Promotion")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "OrderPromotion.findAll", query = "SELECT o FROM OrderPromotion o"),
    @NamedQuery(name = "OrderPromotion.findByOrderID", query = "SELECT o FROM OrderPromotion o WHERE o.orderID = :orderID"),
    @NamedQuery(name = "OrderPromotion.findByAppliedAt", query = "SELECT o FROM OrderPromotion o WHERE o.appliedAt = :appliedAt")})
public class OrderPromotion implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Column(name = "Order_ID")
    private Integer orderID;
    @Column(name = "Applied_At")
    @Temporal(TemporalType.TIMESTAMP)
    private Date appliedAt;
    @JoinColumn(name = "Order_ID", referencedColumnName = "Order_ID", insertable = false, updatable = false)
    @OneToOne(optional = false)
    private Order1 order1;
    @JoinColumn(name = "Promotion_ID", referencedColumnName = "Promotion_ID")
    @ManyToOne(optional = false)
    private Promotions promotionID;

    public OrderPromotion() {
    }

    public OrderPromotion(Integer orderID) {
        this.orderID = orderID;
    }

    public Integer getOrderID() {
        return orderID;
    }

    public void setOrderID(Integer orderID) {
        this.orderID = orderID;
    }

    public Date getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Date appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Order1 getOrder1() {
        return order1;
    }

    public void setOrder1(Order1 order1) {
        this.order1 = order1;
    }

    public Promotions getPromotionID() {
        return promotionID;
    }

    public void setPromotionID(Promotions promotionID) {
        this.promotionID = promotionID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (orderID != null ? orderID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OrderPromotion)) {
            return false;
        }
        OrderPromotion other = (OrderPromotion) object;
        if ((this.orderID == null && other.orderID != null) || (this.orderID != null && !this.orderID.equals(other.orderID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.OrderPromotion[ orderID=" + orderID + " ]";
    }
    
}
