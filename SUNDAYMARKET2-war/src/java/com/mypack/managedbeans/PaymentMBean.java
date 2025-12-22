package com.mypack.managedbeans;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mypack.entity.Order1;
import mypack.entity.OrderDetails;
import mypack.entity.Payment;
import mypack.entity.ShoppingCart;
import mypack.entity.User;
import mypack.sessionbean.Order1FacadeLocal;
import mypack.sessionbean.OrderDetailsFacadeLocal;
import mypack.sessionbean.PaymentFacadeLocal;
import mypack.sessionbean.ShoppingCartFacadeLocal;

@Named(value = "paymentMBean")
@SessionScoped
public class PaymentMBean implements Serializable {

    @EJB
    private Order1FacadeLocal orderFacade;
    
    @EJB
    private OrderDetailsFacadeLocal orderDetailsFacade;
    
    @EJB
    private ShoppingCartFacadeLocal shoppingCartFacade;
    
    @EJB
    private PaymentFacadeLocal paymentFacade;
    
    private Order1 pendingOrder;
    private Order1 processingOrder; // Order being processed from QR code
    private int totalAmount;
    private String paymentMethod = "cod"; // cod, online (default to COD)
    private String qrCodeUrl;
    private String paymentUrl;
    private boolean paymentSuccess = false;
    
    // Parameters from URL
    private Integer orderIdParam;
    private Integer amountParam;
    private String methodParam;

    // Demo Mode - For project demonstration (no real payment integration needed)
    private static final boolean DEMO_MODE = true;
    
    // VNPay Configuration (Demo - for project only, not real integration)
    private static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNPAY_TMN_CODE = "DEMO_TMN_CODE"; // Demo only
    private static final String VNPAY_HASH_SECRET = "DEMO_HASH_SECRET"; // Demo only
    private static final String VNPAY_RETURN_URL = "http://localhost:8080/sundaymarket-war/payment-callback.xhtml";

    public PaymentMBean() {
    }
    
    // Load order from URL parameters (when accessed via QR code)
    public void loadOrderFromParam() {
        try {
            if (orderIdParam != null) {
                processingOrder = orderFacade.find(orderIdParam);
                if (processingOrder != null && processingOrder.getStatus() != null && 
                    ("pending".equals(processingOrder.getStatus().toLowerCase()) || 
                     "processing".equals(processingOrder.getStatus().toLowerCase()))) {
                    // Order is valid for payment
                    System.out.println("Loaded order from param: Order ID = " + processingOrder.getOrderID());
                } else {
                    System.out.println("Order status is not valid for payment: " + 
                        (processingOrder != null ? processingOrder.getStatus() : "null"));
                    processingOrder = null;
                }
            } else {
                System.out.println("orderIdParam is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            processingOrder = null;
        }
    }
    
    // Process payment from QR code scan (Virtual Invoice Payment)
    public String processPaymentFromQR() {
        try {
            System.out.println("=== Processing Virtual Invoice Payment from QR Code ===");
            
            if (processingOrder == null) {
                System.out.println("ERROR: Processing order is null");
                addErr("⚠️ Order not found!");
                return null;
            }
            
            System.out.println("Processing payment for Order ID: " + processingOrder.getOrderID());
            System.out.println("Amount: " + (amountParam != null ? amountParam : processingOrder.getTotalAmount()));
            System.out.println("Payment Method: " + methodParam);
            
            // Simulate payment processing delay (for demo effect)
            try {
                Thread.sleep(800); // Simulate payment processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Update order status to processing
            processingOrder.setStatus("processing");
            orderFacade.edit(processingOrder);
            System.out.println("Order status updated to: processing");
            
            // Update payment status to paid (only for online payments, COD stays pending until delivery)
            Payment payment = paymentFacade.findByOrder(processingOrder);
            if (payment != null) {
                if (methodParam != null && !"cod".equalsIgnoreCase(methodParam)) {
                    // Online payment: mark as paid immediately
                    payment.setPaymentStatus("paid");
                    payment.setPaymentDate(new Date());
                    payment.setTransactionID("TXN_" + processingOrder.getOrderID() + "_" + System.currentTimeMillis());
                    System.out.println("Payment status updated to: paid (online payment)");
                } else {
                    // COD: keep as pending, will be marked as paid when shipper delivers
                    System.out.println("Payment status remains pending (COD - will be paid on delivery)");
                }
                payment.setUpdatedAt(new Date());
                paymentFacade.edit(payment);
            }
            
            // Clear cart for this user
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
                LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
                
                if (loginMBean != null && loginMBean.getCurrentUser() != null) {
                    jakarta.el.ValueExpression veCart = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                    ShoppingCartMBean cartMBean = (ShoppingCartMBean) veCart.getValue(elContext);
                    
                    if (cartMBean != null) {
                        List<ShoppingCart> items = cartMBean.getCartItems();
                        int removedCount = 0;
                        for (ShoppingCart cart : items) {
                            if (cart.getUserID() != null && 
                                cart.getUserID().getUserID().equals(loginMBean.getCurrentUser().getUserID())) {
                                shoppingCartFacade.remove(cart);
                                removedCount++;
                            }
                        }
                        System.out.println("Removed " + removedCount + " items from cart");
                    }
                }
            }
            
            paymentSuccess = true;
            addInfo("✅ Payment successful! Order #" + processingOrder.getOrderID() + " is being processed.");
            
            processingOrder = null;
            orderIdParam = null;
            amountParam = null;
            methodParam = null;
            
            System.out.println("=== Virtual Invoice Payment completed successfully ===");
            return "customerorder?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in processPaymentFromQR: " + e.getMessage());
            addErr("❌ Payment processing failed: " + e.getMessage());
            return null;
        }
    }
    
    // Cancel payment from QR code
    public String cancelPaymentFromQR() {
        processingOrder = null;
        orderIdParam = null;
        amountParam = null;
        methodParam = null;
        return "index?faces-redirect=true";
    }
    
    // Initialize payment
    public String initPayment(int amount, String shipAddress) {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.el.ELContext elContext = facesContext.getELContext();
            jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
            jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{loginMBean}", LoginMBean.class);
            LoginMBean loginMBean = (LoginMBean) ve.getValue(elContext);
            
            if (loginMBean == null || loginMBean.getCurrentUser() == null) {
                addErr("⚠️ Please login to proceed with payment!");
                return null;
            }
            
            User currentUser = loginMBean.getCurrentUser();
            
            // Get cart items
            jakarta.el.ValueExpression veCart = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
            ShoppingCartMBean cartMBean = (ShoppingCartMBean) veCart.getValue(elContext);
            
            if (cartMBean == null) {
                addErr("⚠️ Cart not found!");
                return null;
            }
            
            List<ShoppingCart> items = cartMBean.getCartItems();
            if (items == null || items.isEmpty()) {
                addErr("⚠️ Cart is empty!");
                return null;
            }
            
            // Calculate shipping fee from system settings
            int shippingFee = getDefaultShippingFee();
            int totalAmountWithShipping = amount + shippingFee;
            
            // Create Order first
            Order1 order = new Order1();
            order.setUserID(currentUser);
            order.setOrderDate(new Date());
            order.setShippingFee(shippingFee); // Set default shipping fee
            order.setTotalAmount(totalAmountWithShipping); // Total = subtotal + shipping fee
            order.setStatus("pending");
            orderFacade.create(order);
            
            // Create OrderDetails
            for (ShoppingCart cart : items) {
                OrderDetails orderDetail = new OrderDetails();
                orderDetail.setOrderID(order);
                orderDetail.setProductID(cart.getProductID());
                orderDetail.setQuantity(cart.getQuantity());
                orderDetail.setUnitPrice(cart.getProductID().getUnitPrice());
                orderDetail.setShipAddress(shipAddress);
                orderDetailsFacade.create(orderDetail);
            }
            
            // Create Payment record
            Payment payment = new Payment();
            payment.setOrderID(order);
            // Normalize payment method: only COD or ONLINE
            // Use current paymentMethod if set, otherwise default to COD
            String normalizedMethod = paymentMethod != null ? paymentMethod.toUpperCase() : "COD";
            if ("COD".equals(normalizedMethod)) {
                payment.setPaymentMethod("COD");
            } else {
                // All other methods (online, vnpay, momo, bank_transfer) → ONLINE
                payment.setPaymentMethod("ONLINE");
            }
            payment.setPaymentStatus("pending"); // pending, paid, failed, refunded
            payment.setAmount(totalAmountWithShipping); // Payment amount includes shipping fee
            payment.setCreatedAt(new Date());
            payment.setUpdatedAt(new Date());
            paymentFacade.create(payment);
            
            // Keep paymentMethod as is (don't override with saved value)
            // User will select payment method on payment page, and updatePaymentInfo() will update the Payment record
            
            pendingOrder = order;
            totalAmount = totalAmountWithShipping;
            
            // Generate payment URL and QR code based on method
            updatePaymentInfo();
            
            return "payment?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Payment initialization failed: " + e.getMessage());
            return null;
        }
    }
    
    // Update payment info based on selected method
    public void updatePaymentInfo() {
        if (pendingOrder == null) {
            System.out.println("updatePaymentInfo: pendingOrder is null");
            return;
        }
        
        try {
            System.out.println("updatePaymentInfo: paymentMethod = " + paymentMethod);
            
            // Update Payment record in database with selected payment method
            Payment payment = paymentFacade.findByOrder(pendingOrder);
            if (payment != null && paymentMethod != null) {
                // Normalize payment method: only COD or ONLINE
                String normalizedMethod = paymentMethod.toUpperCase();
                if ("COD".equals(normalizedMethod)) {
                    payment.setPaymentMethod("COD");
                } else {
                    // All other methods (online, vnpay, momo, bank_transfer) → ONLINE
                    payment.setPaymentMethod("ONLINE");
                }
                payment.setUpdatedAt(new Date());
                paymentFacade.edit(payment);
                System.out.println("Payment method updated to: " + payment.getPaymentMethod() + " (from UI selection: " + paymentMethod + ")");
            }
            
            FacesContext facesContext = FacesContext.getCurrentInstance();
            String baseUrl = "";
            if (facesContext != null) {
                jakarta.servlet.http.HttpServletRequest request = 
                    (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = facesContext.getExternalContext().getRequestContextPath();
                baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
            }
            
            // Generate payment URL based on method
            if ("cod".equalsIgnoreCase(paymentMethod)) {
                // COD: No QR code or payment URL needed
                qrCodeUrl = null;
                paymentUrl = null;
                System.out.println("updatePaymentInfo: COD selected, cleared QR and URL");
            } else if ("online".equalsIgnoreCase(paymentMethod)) {
                // ONLINE: Default to VNPay, but can be changed to MoMo or Bank Transfer
                // For simplicity, we'll use VNPay as default online payment
                paymentUrl = generateVNPayUrl(pendingOrder, totalAmount);
                qrCodeUrl = generateQRCode(paymentUrl);
                System.out.println("updatePaymentInfo: ONLINE selected, generated QR: " + (qrCodeUrl != null ? "YES" : "NO") + ", URL: " + (paymentUrl != null ? "YES" : "NO"));
            } else {
                // Legacy support for old payment methods (vnpay, momo, bank_transfer)
                // These will be normalized to "ONLINE" when saved
                if ("vnpay".equals(paymentMethod)) {
                    paymentUrl = generateVNPayUrl(pendingOrder, totalAmount);
                    qrCodeUrl = generateQRCode(paymentUrl);
                } else if ("momo".equals(paymentMethod)) {
                    paymentUrl = generateMoMoUrl(pendingOrder, totalAmount);
                    qrCodeUrl = generateQRCode(paymentUrl);
                } else if ("bank_transfer".equals(paymentMethod)) {
                    String paymentPageUrl = baseUrl + "/payment-process.xhtml?orderId=" + pendingOrder.getOrderID() + "&amount=" + totalAmount + "&method=bank";
                    qrCodeUrl = generateBankQRCode(pendingOrder, totalAmount, paymentPageUrl);
                    paymentUrl = null;
                }
            }
        } catch (Exception e) {
            System.err.println("Error in updatePaymentInfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Generate VNPay payment URL (Demo mode - virtual VNPay page for project)
    private String generateVNPayUrl(Order1 order, int amount) {
        if (DEMO_MODE) {
            // Demo mode: Generate URL to virtual VNPay payment page
            try {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                String baseUrl = "";
                if (facesContext != null) {
                    jakarta.servlet.http.HttpServletRequest request = 
                        (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                    String scheme = request.getScheme();
                    String serverName = request.getServerName();
                    int serverPort = request.getServerPort();
                    String contextPath = facesContext.getExternalContext().getRequestContextPath();
                    baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
                }
                return baseUrl + "/vnpay-payment.xhtml?orderId=" + order.getOrderID() + "&amount=" + amount;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        // Real integration code (for production)
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TmnCode = VNPAY_TMN_CODE;
            String vnp_Amount = String.valueOf(amount * 100); // VNPay uses cents
            String vnp_CurrCode = "VND";
            String vnp_TxnRef = String.valueOf(order.getOrderID()) + "_" + System.currentTimeMillis();
            String vnp_OrderInfo = "Payment for Order #" + order.getOrderID();
            String vnp_OrderType = "other";
            String vnp_Locale = "vn";
            String vnp_ReturnUrl = VNPAY_RETURN_URL;
            String vnp_IpAddr = getClientIpAddress();
            
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(new Date());
            
            Calendar cld = Calendar.getInstance();
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            
            Map<String, String> vnp_Params = new TreeMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            StringBuilder queryUrl = new StringBuilder();
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                hashData.append(entry.getKey());
                hashData.append('=');
                hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                queryUrl.append('=');
                queryUrl.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    queryUrl.append('&');
                    hashData.append('&');
                }
            }
            
            String vnp_SecureHash = hmacSHA512(VNPAY_HASH_SECRET, hashData.toString());
            queryUrl.append("vnp_SecureHash=").append(vnp_SecureHash);
            
            return VNPAY_URL + "?" + queryUrl.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Generate MoMo payment URL (Demo mode for project)
    private String generateMoMoUrl(Order1 order, int amount) {
        // Demo mode: Generate URL to virtual MoMo payment page
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            String baseUrl = "";
            if (facesContext != null) {
                jakarta.servlet.http.HttpServletRequest request = 
                    (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = facesContext.getExternalContext().getRequestContextPath();
                baseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
            }
            return baseUrl + "/momo-payment.xhtml?orderId=" + order.getOrderID() + "&amount=" + amount;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Generate Bank Transfer QR Code
    private String generateBankQRCode(Order1 order, int amount, String paymentPageUrl) {
        try {
            // Generate QR code for bank transfer using VietQR format
            // Format: Bank account info + amount + order ID + payment URL
            String bankAccount = "1234567890";
            String bankName = "Vietcombank";
            String accountName = "SUNDAY MARKET";
            String content = "Order #" + order.getOrderID();
            
            // VietQR format: bank|account|name|amount|content|paymentUrl
            String qrData = String.format("%s|%s|%s|%d|%s|%s",
                bankName, bankAccount, accountName, amount, content, paymentPageUrl);
            
            return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + 
                   URLEncoder.encode(qrData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Generate QR Code from URL
    private String generateQRCode(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + 
                   URLEncoder.encode(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // HMAC SHA512 for VNPay
    private String hmacSHA512(String key, String data) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Get client IP address
    private String getClientIpAddress() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            jakarta.servlet.http.HttpServletRequest request = 
                (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }
        return "127.0.0.1";
    }
    
    // Confirm payment (virtual payment for testing)
    public String confirmPayment() {
        try {
            System.out.println("=== PaymentMBean.confirmPayment() ===");
            
            if (pendingOrder == null) {
                addErr("⚠️ No pending order found!");
                return null;
            }
            
            System.out.println("Processing payment for Order ID: " + pendingOrder.getOrderID());
            System.out.println("Amount: " + totalAmount);
            System.out.println("Payment Method: " + paymentMethod);
            
            // Simulate payment processing delay (for demo)
            try {
                Thread.sleep(500); // Simulate payment processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Update order status to processing
            pendingOrder.setStatus("processing");
            orderFacade.edit(pendingOrder);
            System.out.println("Order status updated to: processing");
            
            // Update payment status to paid (only for online payments, COD stays pending until delivery)
            Payment payment = paymentFacade.findByOrder(pendingOrder);
            if (payment != null) {
                // Update payment method from UI selection (in case it changed)
                String normalizedMethod = paymentMethod != null ? paymentMethod.toUpperCase() : "COD";
                if ("COD".equals(normalizedMethod)) {
                    payment.setPaymentMethod("COD");
                } else {
                    payment.setPaymentMethod("ONLINE");
                }
                
                if (!"cod".equalsIgnoreCase(paymentMethod)) {
                    // Online payment: mark as paid immediately
                    payment.setPaymentStatus("paid");
                    payment.setPaymentDate(new Date());
                    payment.setTransactionID("TXN_" + pendingOrder.getOrderID() + "_" + System.currentTimeMillis());
                    System.out.println("Payment status updated to: paid (online payment), method: " + payment.getPaymentMethod());
                } else {
                    // COD: keep as pending, will be marked as paid when shipper delivers
                    System.out.println("Payment status remains pending (COD - will be paid on delivery)");
                }
                payment.setUpdatedAt(new Date());
                paymentFacade.edit(payment);
            }
            
            // Clear cart
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                ShoppingCartMBean cartMBean = (ShoppingCartMBean) ve.getValue(elContext);
                
                if (cartMBean != null) {
                    List<ShoppingCart> items = cartMBean.getCartItems();
                    int removedCount = 0;
                    for (ShoppingCart cart : items) {
                        shoppingCartFacade.remove(cart);
                        removedCount++;
                    }
                    System.out.println("Removed " + removedCount + " items from cart");
                }
            }
            
            paymentSuccess = true;
            addInfo("✅ Payment successful! Order #" + pendingOrder.getOrderID() + " is being processed.");
            
            pendingOrder = null;
            qrCodeUrl = null;
            paymentUrl = null;
            
            System.out.println("=== Payment confirmed successfully ===");
            return "customerorder?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in confirmPayment: " + e.getMessage());
            addErr("❌ Payment confirmation failed: " + e.getMessage());
            return null;
        }
    }
    
    // Cancel payment
    public String cancelPayment() {
        try {
            if (pendingOrder != null) {
                pendingOrder.setStatus("cancelled");
                orderFacade.edit(pendingOrder);
            }
            
            pendingOrder = null;
            qrCodeUrl = null;
            paymentUrl = null;
            
            addInfo("ℹ️ Payment cancelled. You can try again later.");
            return "shoppingcart?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Error cancelling payment: " + e.getMessage());
            return null;
        }
    }
    
    // Handle payment callback (from VNPay/MoMo)
    public String handlePaymentCallback() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext == null) {
                return null;
            }
            
            jakarta.servlet.http.HttpServletRequest request = 
                (jakarta.servlet.http.HttpServletRequest) facesContext.getExternalContext().getRequest();
            
            Map<String, String> params = new TreeMap<>();
            for (String paramName : request.getParameterMap().keySet()) {
                params.put(paramName, request.getParameter(paramName));
            }
            
            // Verify payment (simplified - should verify with VNPay)
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            
            if ("00".equals(vnp_ResponseCode) && pendingOrder != null) {
                // Payment successful
                pendingOrder.setStatus("processing");
                orderFacade.edit(pendingOrder);
                
                // Update payment status to paid
                Payment payment = paymentFacade.findByOrder(pendingOrder);
                if (payment != null) {
                    payment.setPaymentStatus("paid");
                    payment.setPaymentDate(new Date());
                    payment.setUpdatedAt(new Date());
                    payment.setTransactionID(params.get("vnp_TxnRef"));
                    paymentFacade.edit(payment);
                }
                
                // Clear cart
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{shoppingCartMBean}", ShoppingCartMBean.class);
                ShoppingCartMBean cartMBean = (ShoppingCartMBean) ve.getValue(elContext);
                
                if (cartMBean != null) {
                    List<ShoppingCart> items = cartMBean.getCartItems();
                    for (ShoppingCart cart : items) {
                        shoppingCartFacade.remove(cart);
                    }
                }
                
                paymentSuccess = true;
                addInfo("✅ Payment successful! Order #" + pendingOrder.getOrderID() + " is being processed.");
                
                pendingOrder = null;
                return "customerorder?faces-redirect=true";
            } else {
                // Payment failed
                if (pendingOrder != null) {
                    pendingOrder.setStatus("cancelled");
                    orderFacade.edit(pendingOrder);
                    
                    // Update payment status to failed
                    Payment payment = paymentFacade.findByOrder(pendingOrder);
                    if (payment != null) {
                        payment.setPaymentStatus("failed");
                        payment.setUpdatedAt(new Date());
                        paymentFacade.edit(payment);
                    }
                }
                addErr("❌ Payment failed. Please try again.");
                return "payment?faces-redirect=true";
            }
        } catch (Exception e) {
            e.printStackTrace();
            addErr("❌ Payment processing error: " + e.getMessage());
            return null;
        }
    }
    
    // Get default shipping fee - load directly from database for fresh value
    private int getDefaultShippingFee() {
        try {
            // Try to load from database directly for fresh value
            // Access AdminFinanceMBean instance to reload from database
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null) {
                jakarta.el.ELContext elContext = facesContext.getELContext();
                jakarta.el.ExpressionFactory factory = facesContext.getApplication().getExpressionFactory();
                jakarta.el.ValueExpression ve = factory.createValueExpression(elContext, "#{adminFinanceMBean}", AdminFinanceMBean.class);
                AdminFinanceMBean adminFinanceMBean = (AdminFinanceMBean) ve.getValue(elContext);
                if (adminFinanceMBean != null) {
                    // Reload config from database to get latest value
                    adminFinanceMBean.reloadConfigurationFromDatabase();
                    return adminFinanceMBean.getDefaultShippingFee();
                }
            }
            // Fallback to static method if can't access instance
            return AdminFinanceMBean.getDefaultShippingFeeStatic();
        } catch (Exception e) {
            System.err.println("Error getting default shipping fee: " + e.getMessage());
            e.printStackTrace();
            // Fallback to default value if error
            return 3000; // 3,000 VND (default)
        }
    }
    
    // Format amount
    public String formatAmount(int amount) {
        return String.format("%,d", amount) + " VND";
    }
    
    // Get order details for pending order
    public List<OrderDetails> getOrderDetails(Order1 order) {
        if (order == null) {
            return new ArrayList<>();
        }
        try {
            List<OrderDetails> allDetails = orderDetailsFacade.findAll();
            List<OrderDetails> result = new ArrayList<>();
            for (OrderDetails detail : allDetails) {
                if (detail.getOrderID() != null && detail.getOrderID().getOrderID().equals(order.getOrderID())) {
                    result.add(detail);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // Calculate subtotal (sum of all product prices)
    public int getSubtotal(Order1 order) {
        if (order == null) {
            return 0;
        }
        try {
            List<OrderDetails> details = getOrderDetails(order);
            int subtotal = 0;
            for (OrderDetails detail : details) {
                subtotal += detail.getUnitPrice() * detail.getQuantity();
            }
            return subtotal;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // Get shipping fee for order
    public int getShippingFee(Order1 order) {
        if (order == null) {
            return getDefaultShippingFee();
        }
        return order.getShippingFee() != null ? order.getShippingFee() : getDefaultShippingFee();
    }
    
    // Calculate total (subtotal + shipping fee)
    public int calculateTotal(Order1 order) {
        return getSubtotal(order) + getShippingFee(order);
    }
    
    // Check if order has payment
    public boolean hasPayment(Order1 order) {
        if (order == null) {
            return false;
        }
        try {
            Payment payment = paymentFacade.findByOrder(order);
            return payment != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Get payment for an order
    public Payment getPayment(Order1 order) {
        if (order == null) {
            return null;
        }
        try {
            return paymentFacade.findByOrder(order);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Get payment method for an order
    public String getPaymentMethodForOrder(Order1 order) {
        Payment payment = getPayment(order);
        if (payment != null && payment.getPaymentMethod() != null) {
            return payment.getPaymentMethod();
        }
        return "N/A";
    }
    
    // Get payment status for an order
    public String getPaymentStatusForOrder(Order1 order) {
        Payment payment = getPayment(order);
        if (payment != null && payment.getPaymentStatus() != null) {
            return payment.getPaymentStatus();
        }
        return "N/A";
    }
    
    // Check if order is COD
    public boolean isCODOrder(Order1 order) {
        String method = getPaymentMethodForOrder(order);
        return "COD".equalsIgnoreCase(method) || "cod".equalsIgnoreCase(method);
    }
    
    // Get payment method display name
    public String getPaymentMethodDisplay(Order1 order) {
        String method = getPaymentMethodForOrder(order);
        if (method == null || "N/A".equals(method)) {
            return "Chưa có";
        }
        switch (method.toUpperCase()) {
            case "COD":
                return "💰 COD (Thu tiền khi giao)";
            case "ONLINE":
                return "💳 Thanh toán Online";
            default:
                return method;
        }
    }
    
    // Get payment status display
    public String getPaymentStatusDisplay(Order1 order) {
        String status = getPaymentStatusForOrder(order);
        if (status == null || "N/A".equals(status)) {
            return "Chưa có";
        }
        switch (status.toUpperCase()) {
            case "PENDING":
                return "⏳ Chờ thanh toán";
            case "PAID":
                return "✅ Đã thanh toán";
            case "FAILED":
                return "❌ Thanh toán thất bại";
            case "REFUNDED":
                return "↩️ Đã hoàn tiền";
            default:
                return status;
        }
    }
    
    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErr(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    // Getters and Setters
    public Order1 getPendingOrder() {
        return pendingOrder;
    }

    public void setPendingOrder(Order1 pendingOrder) {
        this.pendingOrder = pendingOrder;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public boolean isPaymentSuccess() {
        return paymentSuccess;
    }

    public void setPaymentSuccess(boolean paymentSuccess) {
        this.paymentSuccess = paymentSuccess;
    }

    public Order1 getProcessingOrder() {
        return processingOrder;
    }

    public void setProcessingOrder(Order1 processingOrder) {
        this.processingOrder = processingOrder;
    }

    public Integer getOrderIdParam() {
        return orderIdParam;
    }

    public void setOrderIdParam(Integer orderIdParam) {
        this.orderIdParam = orderIdParam;
    }

    public Integer getAmountParam() {
        return amountParam;
    }

    public void setAmountParam(Integer amountParam) {
        this.amountParam = amountParam;
    }

    public String getMethodParam() {
        return methodParam;
    }

    public void setMethodParam(String methodParam) {
        this.methodParam = methodParam;
    }
}

