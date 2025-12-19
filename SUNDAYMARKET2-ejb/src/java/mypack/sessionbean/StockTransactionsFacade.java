/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import mypack.entity.StockTransactions;

/**
 *
 * @author MC
 */
@Stateless
public class StockTransactionsFacade extends AbstractFacade<StockTransactions> implements StockTransactionsFacadeLocal {

    @PersistenceContext(unitName = "SUNDAYMARKET2-ejbPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public StockTransactionsFacade() {
        super(StockTransactions.class);
    }
    
    // Lấy giao dịch IMPORT gần nhất theo sản phẩm
    @Override
    public StockTransactions findLastImportByProduct(int productId) {
        try {
            return em.createQuery(
                "SELECT s FROM StockTransactions s " +
                "WHERE s.productID.productID = :pid AND s.type = 'Import' " +
                "ORDER BY s.createdAt DESC",
                StockTransactions.class
            )
            .setParameter("pid", productId)
            .setMaxResults(1)
            .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
    
}
