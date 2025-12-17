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
 * @author My PC
 */
@Entity
@Table(name = "Stock_Transactions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "StockTransactions.findAll", query = "SELECT s FROM StockTransactions s"),
    @NamedQuery(name = "StockTransactions.findByTransactionID", query = "SELECT s FROM StockTransactions s WHERE s.transactionID = :transactionID"),
    @NamedQuery(name = "StockTransactions.findByType", query = "SELECT s FROM StockTransactions s WHERE s.type = :type"),
    @NamedQuery(name = "StockTransactions.findByQuantity", query = "SELECT s FROM StockTransactions s WHERE s.quantity = :quantity"),
    @NamedQuery(name = "StockTransactions.findByCreatedAt", query = "SELECT s FROM StockTransactions s WHERE s.createdAt = :createdAt"),
    @NamedQuery(name = "StockTransactions.findByNote", query = "SELECT s FROM StockTransactions s WHERE s.note = :note"),
    @NamedQuery(name = "StockTransactions.findByUnitCost", query = "SELECT s FROM StockTransactions s WHERE s.unitCost = :unitCost")})
public class StockTransactions implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Transaction_ID")
    private Integer transactionID;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Type")
    private String type;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private int quantity;
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Size(max = 255)
    @Column(name = "Note")
    private String note;
    @Column(name = "UnitCost")
    private Integer unitCost;
    @JoinColumn(name = "Product_ID", referencedColumnName = "Product_ID")
    @ManyToOne(optional = false)
    private Product productID;
    @JoinColumn(name = "Supplier_ID", referencedColumnName = "Supplier_ID")
    @ManyToOne
    private Supplier supplierID;

    public StockTransactions() {
    }

    public StockTransactions(Integer transactionID) {
        this.transactionID = transactionID;
    }

    public StockTransactions(Integer transactionID, String type, int quantity) {
        this.transactionID = transactionID;
        this.type = type;
        this.quantity = quantity;
    }

    public Integer getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(Integer transactionID) {
        this.transactionID = transactionID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Integer unitCost) {
        this.unitCost = unitCost;
    }

    public Product getProductID() {
        return productID;
    }

    public void setProductID(Product productID) {
        this.productID = productID;
    }

    public Supplier getSupplierID() {
        return supplierID;
    }

    public void setSupplierID(Supplier supplierID) {
        this.supplierID = supplierID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transactionID != null ? transactionID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof StockTransactions)) {
            return false;
        }
        StockTransactions other = (StockTransactions) object;
        if ((this.transactionID == null && other.transactionID != null) || (this.transactionID != null && !this.transactionID.equals(other.transactionID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.StockTransactions[ transactionID=" + transactionID + " ]";
    }
    
}
