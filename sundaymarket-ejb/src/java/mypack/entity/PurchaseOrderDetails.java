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
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "PurchaseOrder_Details")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PurchaseOrderDetails.findAll", query = "SELECT p FROM PurchaseOrderDetails p"),
    @NamedQuery(name = "PurchaseOrderDetails.findByPodId", query = "SELECT p FROM PurchaseOrderDetails p WHERE p.podId = :podId"),
    @NamedQuery(name = "PurchaseOrderDetails.findByQuantity", query = "SELECT p FROM PurchaseOrderDetails p WHERE p.quantity = :quantity"),
    @NamedQuery(name = "PurchaseOrderDetails.findByUnitCost", query = "SELECT p FROM PurchaseOrderDetails p WHERE p.unitCost = :unitCost")})
public class PurchaseOrderDetails implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "POD_ID")
    private Integer podId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private int quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UnitCost")
    private int unitCost;
    @JoinColumn(name = "Product_ID", referencedColumnName = "Product_ID")
    @ManyToOne(optional = false)
    private Product productID;
    @JoinColumn(name = "PO_ID", referencedColumnName = "PO_ID")
    @ManyToOne(optional = false)
    private PurchaseOrder poId;

    public PurchaseOrderDetails() {
    }

    public PurchaseOrderDetails(Integer podId) {
        this.podId = podId;
    }

    public PurchaseOrderDetails(Integer podId, int quantity, int unitCost) {
        this.podId = podId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public Integer getPodId() {
        return podId;
    }

    public void setPodId(Integer podId) {
        this.podId = podId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(int unitCost) {
        this.unitCost = unitCost;
    }

    public Product getProductID() {
        return productID;
    }

    public void setProductID(Product productID) {
        this.productID = productID;
    }

    public PurchaseOrder getPoId() {
        return poId;
    }

    public void setPoId(PurchaseOrder poId) {
        this.poId = poId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (podId != null ? podId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PurchaseOrderDetails)) {
            return false;
        }
        PurchaseOrderDetails other = (PurchaseOrderDetails) object;
        if ((this.podId == null && other.podId != null) || (this.podId != null && !this.podId.equals(other.podId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.PurchaseOrderDetails[ podId=" + podId + " ]";
    }
    
}
