///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package mypack.sessionbean;
//
//import jakarta.ejb.Stateless;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import mypack.entity.PurchaseOrder;
//
///**
// *
// * @author MC
// */
//@Stateless
//public class PurchaseOrderFacade extends AbstractFacade<PurchaseOrder> implements PurchaseOrderFacadeLocal {
//
//    @PersistenceContext(unitName = "sundaymarket-ejbPU")
//    private EntityManager em;
//
//    @Override
//    protected EntityManager getEntityManager() {
//        return em;
//    }
//
//    public PurchaseOrderFacade() {
//        super(PurchaseOrder.class);
//    }
//    
//}
