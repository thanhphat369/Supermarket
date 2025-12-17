# Hướng dẫn kiểm tra Server Log

## Cách xem Server Log trong NetBeans:

1. **Mở GlassFish Server Log:**
   - Trong NetBeans, mở tab **Services** (Window → Services)
   - Mở rộng **Servers** → **GlassFish Server**
   - Click chuột phải vào server → **View Server Log**

2. **Hoặc xem log file trực tiếp:**
   - Đường dẫn thường là: `C:\Users\[YourUser]\GlassFish_Server_7\glassfish\domains\domain1\logs\server.log`

## Các dòng log cần tìm:

### 1. CategoryMBean Initialization:
```
CategoryMBean.init() - Starting initialization...
CategoryMBean.init() - categoryFacade injected successfully
CategoryMBean.init() - Calling categoryFacade.findActive()...
CategoryMBean.init() - findActive() returned: X categories
CategoryMBean.init() - Calling categoryFacade.findLevel1()...
CategoryMBean.init() - findLevel1() returned: X categories
```

### 2. CustomerProductMBean Cache:
```
CustomerProductMBean.getCategoriesWithCache() - Starting...
CustomerProductMBean.getCategoriesWithCache() - categoryFacade injected successfully
CustomerProductMBean.getCategoriesWithCache() - Calling categoryFacade.findAll()...
CustomerProductMBean.getCategoriesWithCache() - findAll() returned: X categories
CustomerProductMBean.getCategoriesWithCache() - Cached X active categories

CustomerProductMBean.getProductsWithCache() - Starting...
CustomerProductMBean.getProductsWithCache() - productFacade injected successfully
CustomerProductMBean.getProductsWithCache() - Calling productFacade.findAll()...
CustomerProductMBean.getProductsWithCache() - findAll() returned: X products
CustomerProductMBean.getProductsWithCache() - Cached X active products
```

## Các lỗi thường gặp:

### ❌ EJB Injection Failed:
```
ERROR: categoryFacade is NULL! EJB injection failed!
```
**Nguyên nhân:** EJB không được inject đúng
**Giải pháp:** 
- Kiểm tra deployment có thành công không
- Kiểm tra EJB module có được deploy không
- Restart server

### ❌ Method returned null:
```
ERROR: findAll() returned null!
```
**Nguyên nhân:** 
- Database connection không hoạt động
- Persistence unit không đúng
- EntityManager không được inject

**Giải pháp:**
- Kiểm tra database connection pool
- Kiểm tra persistence.xml
- Kiểm tra datasource configuration

### ❌ Method returned empty:
```
WARNING: findAll() returned empty list!
```
**Nguyên nhân:** Database không có dữ liệu
**Giải pháp:** Thêm dữ liệu vào database

### ❌ Exception:
```
EXCEPTION: [error message]
```
**Nguyên nhân:** Có lỗi trong quá trình thực thi
**Giải pháp:** Xem stack trace để biết lỗi cụ thể

## Các bước kiểm tra:

1. **Rebuild và Redeploy:**
   - Right-click project → Clean and Build
   - Right-click project → Deploy

2. **Mở trang ProductPage:**
   - Truy cập trang ProductPage trong browser
   - Điều này sẽ trigger @PostConstruct methods

3. **Xem Server Log:**
   - Mở server log như hướng dẫn trên
   - Tìm các dòng log bắt đầu với `CategoryMBean.init()` và `CustomerProductMBean.getCategoriesWithCache()`

4. **Kiểm tra kết quả:**
   - Nếu thấy "injected successfully" → EJB OK
   - Nếu thấy "returned: X categories/products" → Database có dữ liệu
   - Nếu thấy "returned: 0" → Database trống
   - Nếu thấy "returned: null" → Có lỗi database connection

## Copy log và gửi cho tôi nếu cần hỗ trợ!



