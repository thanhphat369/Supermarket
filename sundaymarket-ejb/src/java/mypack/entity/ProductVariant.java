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
 * @author My PC
 */
@Entity
@Table(name = "ProductVariant")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ProductVariant.findAll", query = "SELECT p FROM ProductVariant p"),
    @NamedQuery(name = "ProductVariant.findByVariantID", query = "SELECT p FROM ProductVariant p WHERE p.variantID = :variantID"),
    @NamedQuery(name = "ProductVariant.findByVariantName", query = "SELECT p FROM ProductVariant p WHERE p.variantName = :variantName"),
    @NamedQuery(name = "ProductVariant.findByUnit", query = "SELECT p FROM ProductVariant p WHERE p.unit = :unit"),
    @NamedQuery(name = "ProductVariant.findByQuantity", query = "SELECT p FROM ProductVariant p WHERE p.quantity = :quantity"),
    @NamedQuery(name = "ProductVariant.findByUnitPrice", query = "SELECT p FROM ProductVariant p WHERE p.unitPrice = :unitPrice"),
    @NamedQuery(name = "ProductVariant.findByStock", query = "SELECT p FROM ProductVariant p WHERE p.stock = :stock"),
    @NamedQuery(name = "ProductVariant.findByIsDefault", query = "SELECT p FROM ProductVariant p WHERE p.isDefault = :isDefault"),
    @NamedQuery(name = "ProductVariant.findByStatus", query = "SELECT p FROM ProductVariant p WHERE p.status = :status"),
    @NamedQuery(name = "ProductVariant.findByCreatedAt", query = "SELECT p FROM ProductVariant p WHERE p.createdAt = :createdAt")})
public class ProductVariant implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Variant_ID")
    private Integer variantID;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "VariantName")
    private String variantName;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "Unit")
    private String unit;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private BigDecimal quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UnitPrice")
    private BigDecimal unitPrice;
    @Column(name = "Stock")
    private Integer stock;
    @Column(name = "IsDefault")
    private Boolean isDefault;
    @Column(name = "Status")
    private Boolean status;
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "Product_ID", referencedColumnName = "Product_ID")
    @ManyToOne(optional = false)
    private Product productID;

    public ProductVariant() {
    }

    public ProductVariant(Integer variantID) {
        this.variantID = variantID;
    }

    public ProductVariant(Integer variantID, String variantName, String unit, BigDecimal quantity, BigDecimal unitPrice) {
        this.variantID = variantID;
        this.variantName = variantName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Integer getVariantID() {
        return variantID;
    }

    public void setVariantID(Integer variantID) {
        this.variantID = variantID;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
        hash += (variantID != null ? variantID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ProductVariant)) {
            return false;
        }
        ProductVariant other = (ProductVariant) object;
        if ((this.variantID == null && other.variantID != null) || (this.variantID != null && !this.variantID.equals(other.variantID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.ProductVariant[ variantID=" + variantID + " ]";
    }
    
}
