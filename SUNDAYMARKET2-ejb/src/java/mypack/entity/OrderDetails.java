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
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "Order_Details")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "OrderDetails.findAll", query = "SELECT o FROM OrderDetails o"),
    @NamedQuery(name = "OrderDetails.findByODetailID", query = "SELECT o FROM OrderDetails o WHERE o.oDetailID = :oDetailID"),
    @NamedQuery(name = "OrderDetails.findByQuantity", query = "SELECT o FROM OrderDetails o WHERE o.quantity = :quantity"),
    @NamedQuery(name = "OrderDetails.findByUnitPrice", query = "SELECT o FROM OrderDetails o WHERE o.unitPrice = :unitPrice"),
    @NamedQuery(name = "OrderDetails.findByShipAddress", query = "SELECT o FROM OrderDetails o WHERE o.shipAddress = :shipAddress")})
public class OrderDetails implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ODetail_ID")
    private Integer oDetailID;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private int quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UnitPrice")
    private int unitPrice;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "ShipAddress")
    private String shipAddress;
    @JoinColumn(name = "Order_ID", referencedColumnName = "Order_ID")
    @ManyToOne(optional = false)
    private Order1 orderID;
    @JoinColumn(name = "Product_ID", referencedColumnName = "Product_ID")
    @ManyToOne(optional = false)
    private Product productID;

    public OrderDetails() {
    }

    public OrderDetails(Integer oDetailID) {
        this.oDetailID = oDetailID;
    }

    public OrderDetails(Integer oDetailID, int quantity, int unitPrice, String shipAddress) {
        this.oDetailID = oDetailID;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.shipAddress = shipAddress;
    }

    public Integer getODetailID() {
        return oDetailID;
    }

    public void setODetailID(Integer oDetailID) {
        this.oDetailID = oDetailID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getShipAddress() {
        return shipAddress;
    }

    public void setShipAddress(String shipAddress) {
        this.shipAddress = shipAddress;
    }

    public Order1 getOrderID() {
        return orderID;
    }

    public void setOrderID(Order1 orderID) {
        this.orderID = orderID;
    }

    public Product getProductID() {
        return productID;
    }

    public void setProductID(Product productID) {
        this.productID = productID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (oDetailID != null ? oDetailID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OrderDetails)) {
            return false;
        }
        OrderDetails other = (OrderDetails) object;
        if ((this.oDetailID == null && other.oDetailID != null) || (this.oDetailID != null && !this.oDetailID.equals(other.oDetailID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.OrderDetails[ oDetailID=" + oDetailID + " ]";
    }
    
}
