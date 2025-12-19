/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package mypack.sessionbean;

import jakarta.ejb.Local;
import java.util.List;
import mypack.entity.StockTransactions;

/**
 *
 * @author MC
 */
@Local
public interface StockTransactionsFacadeLocal {

    void create(StockTransactions stockTransactions);

    void edit(StockTransactions stockTransactions);

    void remove(StockTransactions stockTransactions);

    StockTransactions find(Object id);

    List<StockTransactions> findAll();

    List<StockTransactions> findRange(int[] range);

    int count();
    
    // Lấy giao dịch IMPORT gần nhất theo sản phẩm
    StockTransactions findLastImportByProduct(int productId);
    
}
