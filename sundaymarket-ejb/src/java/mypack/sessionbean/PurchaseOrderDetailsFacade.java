///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package mypack.sessionbean;
//
//import jakarta.ejb.Stateless;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import mypack.entity.PurchaseOrderDetails;
//
///**
// *
// * @author MC
// */
//@Stateless
//public class PurchaseOrderDetailsFacade extends AbstractFacade<PurchaseOrderDetails> implements PurchaseOrderDetailsFacadeLocal {
//
//    @PersistenceContext(unitName = "sundaymarket-ejbPU")
//    private EntityManager em;
//
//    @Override
//    protected EntityManager getEntityManager() {
//        return em;
//    }
//
//    public PurchaseOrderDetailsFacade() {
//        super(PurchaseOrderDetails.class);
//    }
//    
//}
