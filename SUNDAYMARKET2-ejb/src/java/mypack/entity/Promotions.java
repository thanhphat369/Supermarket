/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author MC
 */
@Entity
@Table(name = "Promotions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Promotions.findAll", query = "SELECT p FROM Promotions p"),
    @NamedQuery(name = "Promotions.findByPromotionID", query = "SELECT p FROM Promotions p WHERE p.promotionID = :promotionID"),
    @NamedQuery(name = "Promotions.findByCode", query = "SELECT p FROM Promotions p WHERE p.code = :code"),
    @NamedQuery(name = "Promotions.findByDescription", query = "SELECT p FROM Promotions p WHERE p.description = :description"),
    @NamedQuery(name = "Promotions.findByDiscountType", query = "SELECT p FROM Promotions p WHERE p.discountType = :discountType"),
    @NamedQuery(name = "Promotions.findByDiscountValue", query = "SELECT p FROM Promotions p WHERE p.discountValue = :discountValue"),
    @NamedQuery(name = "Promotions.findByMaxDiscountAmount", query = "SELECT p FROM Promotions p WHERE p.maxDiscountAmount = :maxDiscountAmount"),
    @NamedQuery(name = "Promotions.findByMinOrderValue", query = "SELECT p FROM Promotions p WHERE p.minOrderValue = :minOrderValue"),
    @NamedQuery(name = "Promotions.findByStartDate", query = "SELECT p FROM Promotions p WHERE p.startDate = :startDate"),
    @NamedQuery(name = "Promotions.findByEndDate", query = "SELECT p FROM Promotions p WHERE p.endDate = :endDate"),
    @NamedQuery(name = "Promotions.findByUsageLimit", query = "SELECT p FROM Promotions p WHERE p.usageLimit = :usageLimit"),
    @NamedQuery(name = "Promotions.findByUsageCount", query = "SELECT p FROM Promotions p WHERE p.usageCount = :usageCount"),
    @NamedQuery(name = "Promotions.findByLimitPerUser", query = "SELECT p FROM Promotions p WHERE p.limitPerUser = :limitPerUser"),
    @NamedQuery(name = "Promotions.findByIsActive", query = "SELECT p FROM Promotions p WHERE p.isActive = :isActive"),
    @NamedQuery(name = "Promotions.findByCreatedAt", query = "SELECT p FROM Promotions p WHERE p.createdAt = :createdAt")})
public class Promotions implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "Promotion_ID")
    private Integer promotionID;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "Code")
    private String code;
    @Size(max = 255)
    @Column(name = "Description")
    private String description;
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
    @Column(name = "Max_Discount_Amount")
    private BigDecimal maxDiscountAmount;
    @Column(name = "Min_Order_Value")
    private BigDecimal minOrderValue;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Start_Date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;
    @Basic(optional = false)
    @NotNull
    @Column(name = "End_Date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;
    @Column(name = "Usage_Limit")
    private Integer usageLimit;
    @Column(name = "Usage_Count")
    private Integer usageCount;
    @Column(name = "Limit_Per_User")
    private Integer limitPerUser;
    @Column(name = "Is_Active")
    private Boolean isActive;
    @Column(name = "Created_At")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "promotionID")
    private Collection<OrderPromotion> orderPromotionCollection;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "promotions")
    private PromotionOrderTime promotionOrderTime;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "promotions")
    private Collection<PromotionProduct> promotionProductCollection;

    public Promotions() {
    }

    public Promotions(Integer promotionID) {
        this.promotionID = promotionID;
    }

    public Promotions(Integer promotionID, String code, String discountType, BigDecimal discountValue, Date startDate, Date endDate) {
        this.promotionID = promotionID;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Integer getPromotionID() {
        return promotionID;
    }

    public void setPromotionID(Integer promotionID) {
        this.promotionID = promotionID;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Integer getLimitPerUser() {
        return limitPerUser;
    }

    public void setLimitPerUser(Integer limitPerUser) {
        this.limitPerUser = limitPerUser;
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

    @XmlTransient
    public Collection<OrderPromotion> getOrderPromotionCollection() {
        return orderPromotionCollection;
    }

    public void setOrderPromotionCollection(Collection<OrderPromotion> orderPromotionCollection) {
        this.orderPromotionCollection = orderPromotionCollection;
    }

    public PromotionOrderTime getPromotionOrderTime() {
        return promotionOrderTime;
    }

    public void setPromotionOrderTime(PromotionOrderTime promotionOrderTime) {
        this.promotionOrderTime = promotionOrderTime;
    }

    @XmlTransient
    public Collection<PromotionProduct> getPromotionProductCollection() {
        return promotionProductCollection;
    }

    public void setPromotionProductCollection(Collection<PromotionProduct> promotionProductCollection) {
        this.promotionProductCollection = promotionProductCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (promotionID != null ? promotionID.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Promotions)) {
            return false;
        }
        Promotions other = (Promotions) object;
        if ((this.promotionID == null && other.promotionID != null) || (this.promotionID != null && !this.promotionID.equals(other.promotionID))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "mypack.entity.Promotions[ promotionID=" + promotionID + " ]";
    }
    
}
