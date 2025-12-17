/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 * @author My PC
 */
@Embeddable
public class PromotionProductPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "Promotion_ID")
    private int promotionID;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Product_ID")
    private int productID;

    public PromotionProductPK() {
    }

    public PromotionProductPK(int promotionID, int productID) {
        this.promotionID = promotionID;
        this.productID = productID;
    }

    public int getPromotionID() {
        return promotionID;
    }

    public void setPromotionID(int promotionID) {
        this.promotionID = promotionID;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) promotionID;
        hash += (int) productID;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PromotionProductPK)) {
            return false;
        }
        PromotionProductPK other = (PromotionProductPK) object;
        if (this.promotionID != other.promotionID) {
            return false;
        }
        if (this.productID != other.productID) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.PromotionProductPK[ promotionID=" + promotionID + ", productID=" + productID + " ]";
    }
    
}
