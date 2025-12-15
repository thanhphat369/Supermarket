/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "Promotion_Product")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PromotionProduct.findAll", query = "SELECT p FROM PromotionProduct p"),
    @NamedQuery(name = "PromotionProduct.findByPromotionID", query = "SELECT p FROM PromotionProduct p WHERE p.promotionProductPK.promotionID = :promotionID"),
    @NamedQuery(name = "PromotionProduct.findByProductID", query = "SELECT p FROM PromotionProduct p WHERE p.promotionProductPK.productID = :productID"),
    @NamedQuery(name = "PromotionProduct.findByDiscountType", query = "SELECT p FROM PromotionProduct p WHERE p.discountType = :discountType"),
    @NamedQuery(name = "PromotionProduct.findByDiscountValue", query = "SELECT p FROM PromotionProduct p WHERE p.discountValue = :discountValue"),
    @NamedQuery(name = "PromotionProduct.findByMaxDiscount", query = "SELECT p FROM PromotionProduct p WHERE p.maxDiscount = :maxDiscount")})
public class PromotionProduct implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected PromotionProductPK promotionProductPK;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Discount_Type")
    private String discountType;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "Discount_Value")
    private BigDecimal discountValue;
    @Column(name = "Max_Discount")
    private BigDecimal maxDiscount;
    @JoinColumn(name = "Product_ID", referencedColumnName = "Product_ID", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Product product;
    @JoinColumn(name = "Promotion_ID", referencedColumnName = "Promotion_ID", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Promotions promotions;

    public PromotionProduct() {
    }

    public PromotionProduct(PromotionProductPK promotionProductPK) {
        this.promotionProductPK = promotionProductPK;
    }

    public PromotionProduct(PromotionProductPK promotionProductPK, String discountType, BigDecimal discountValue) {
        this.promotionProductPK = promotionProductPK;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    public PromotionProduct(int promotionID, int productID) {
        this.promotionProductPK = new PromotionProductPK(promotionID, productID);
    }

    public PromotionProductPK getPromotionProductPK() {
        return promotionProductPK;
    }

    public void setPromotionProductPK(PromotionProductPK promotionProductPK) {
        this.promotionProductPK = promotionProductPK;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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
        hash += (promotionProductPK != null ? promotionProductPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PromotionProduct)) {
            return false;
        }
        PromotionProduct other = (PromotionProduct) object;
        if ((this.promotionProductPK == null && other.promotionProductPK != null) || (this.promotionProductPK != null && !this.promotionProductPK.equals(other.promotionProductPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.PromotionProduct[ promotionProductPK=" + promotionProductPK + " ]";
    }
    
}
