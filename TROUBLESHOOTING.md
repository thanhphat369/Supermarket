# Hướng dẫn Troubleshooting - Hiển thị Danh mục và Sản phẩm

## Vấn đề: Danh mục và sản phẩm không hiển thị

### Bước 1: Kiểm tra Database

Chạy script SQL sau để kiểm tra database:

```sql
-- Kiểm tra Level 1 categories (ParentCategoryID = NULL AND Level = 1)
SELECT Category_ID, Category_Name, Level, ParentCategoryID, IsActive
FROM Category 
WHERE ParentCategoryID IS NULL 
AND Level = 1
AND (IsActive IS NULL OR IsActive = 1)
ORDER BY SortOrder;

-- Kiểm tra Products active
SELECT Product_ID, Name, Status, CategoryID
FROM Product 
WHERE (Status = 'Active' OR Status = '1' OR Status IS NULL)
LIMIT 10;
```

**Kết quả mong đợi:**
- Phải có ít nhất 2 Level 1 categories (ví dụ: "Thịt, cá, trứng, hải sản", "Rau, củ, nấm, trái cây")
- Phải có ít nhất một số products với Status = 'Active'

### Bước 2: Kiểm tra Server Log

Khi load trang ProductPage, kiểm tra server console log:

**Log từ CategoryMBean:**
```
CategoryMBean.init() - Starting initialization...
CategoryMBean.init() - categoryFacade injected successfully
CategoryMBean.init() - findLevel1() returned: X categories
CategoryMBean.init() - Final Level 1 categories count: X
```

**Log từ CustomerProductMBean:**
```
CustomerProductMBean.getProductsWithCache() - Starting...
CustomerProductMBean.getProductsWithCache() - Cached X active products
CustomerProductMBean.getProducts() - Final result: X products
```

**Nếu thấy lỗi:**
- `categoryFacade is NULL` → EJB injection failed
- `findLevel1() returned null` → Query có vấn đề
- `No Level 1 categories found` → Database không có dữ liệu đúng

### Bước 3: Kiểm tra Trang Web

Trên trang ProductPage, xem phần **Debug Info**:

1. **Bean Status:**
   - `categoryMBean exists: true` → Bean được inject
   - `customerProductMBean exists: true` → Bean được inject

2. **Categories:**
   - `Level 1 Categories: X` → Số lượng Level 1 categories
   - Nếu = 0 → Không có categories được load

3. **Products:**
   - `Total Products: X` → Số lượng products
   - Nếu = 0 → Không có products được load

### Bước 4: Các vấn đề thường gặp và cách sửa

#### Vấn đề 1: Level 1 Categories = 0

**Nguyên nhân:**
- Database không có categories với `Level = 1 AND ParentCategoryID IS NULL`
- Query không đúng

**Cách sửa:**
```sql
-- Kiểm tra và sửa Level cho categories
UPDATE Category SET Level = 1 WHERE ParentCategoryID IS NULL AND Level IS NULL;

-- Kiểm tra lại
SELECT Category_ID, Category_Name, Level, ParentCategoryID
FROM Category 
WHERE ParentCategoryID IS NULL AND Level = 1;
```

#### Vấn đề 2: Products = 0

**Nguyên nhân:**
- Products không có Status = 'Active'
- Products không có CategoryID

**Cách sửa:**
```sql
-- Sửa Status cho products
UPDATE Product SET Status = 'Active' WHERE Status IS NULL OR Status = '';

-- Kiểm tra lại
SELECT Product_ID, Name, Status 
FROM Product 
WHERE Status = 'Active' OR Status = '1';
```

#### Vấn đề 3: Bean không được inject

**Nguyên nhân:**
- ViewScoped bean không được khởi tạo
- CDI không hoạt động

**Cách sửa:**
- Kiểm tra `WEB-INF/beans.xml` có tồn tại không
- Kiểm tra server log có lỗi CDI không
- Thử restart server

#### Vấn đề 4: Query trả về null

**Nguyên nhân:**
- NamedQuery có vấn đề
- Database connection có vấn đề

**Cách sửa:**
- Kiểm tra persistence.xml
- Kiểm tra database connection
- Xem server log để tìm lỗi cụ thể

### Bước 5: Test thủ công

1. **Test Query trực tiếp:**
```sql
-- Test query Level 1
SELECT DISTINCT c.* 
FROM Category c 
WHERE c.Level = 1 
AND c.ParentCategoryID IS NULL 
AND (c.IsActive IS NULL OR c.IsActive = 1) 
ORDER BY c.SortOrder ASC;
```

2. **Test trong Java:**
   - Thêm breakpoint trong `CategoryMBean.init()`
   - Kiểm tra giá trị của `level1Categories` sau khi gọi `findLevel1()`

### Bước 6: Kiểm tra JSF Expression

Trong XHTML, đảm bảo:
- `#{categoryMBean.level1Categories}` → Gọi `getLevel1Categories()`
- `#{customerProductMBean.products}` → Gọi `getProducts()`

Nếu vẫn không hoạt động, thử dùng getter method rõ ràng:
- `#{categoryMBean.getLevel1Categories()}`

### Liên hệ

Nếu vẫn không giải quyết được, cung cấp:
1. Server log đầy đủ
2. Kết quả chạy SQL queries
3. Screenshot của Debug Info trên trang web
4. Thông tin về server (GlassFish/Payara version, Java version)



