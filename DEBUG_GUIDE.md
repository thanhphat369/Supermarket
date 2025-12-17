# Hướng dẫn kiểm tra lỗi không hiển thị sản phẩm và danh mục

## Các bước kiểm tra:

### 1. Kiểm tra Server Log
Mở GlassFish Server Log và tìm các dòng log:
- `CategoryMBean.init() - Loaded X Level 1 categories`
- `CustomerProductMBean - Cached X active categories`
- `CustomerProductMBean - Cached X active products`
- `CustomerProductMBean.getProducts() - Total products in cache: X`

### 2. Kiểm tra trên trang web
Trang ProductPage sẽ hiển thị một box "Debug Info" màu xanh với thông tin:
- Số lượng Level 1 Categories
- Số lượng sản phẩm
- Category ID đang được chọn
- Thông tin category đã chọn

### 3. Các nguyên nhân có thể:

#### A. Không có Categories trong database
**Triệu chứng:** Debug Info hiển thị "Level 1 Categories: 0"

**Giải pháp:**
- Kiểm tra database có dữ liệu Category không
- Đảm bảo có ít nhất 1 category với `ParentCategoryID = NULL` hoặc `Level = 1`
- Đảm bảo `IsActive = 1` (true)

#### B. Không có Products trong database
**Triệu chứng:** Debug Info hiển thị "Total Products: 0"

**Giải pháp:**
- Kiểm tra database có dữ liệu Product không
- Đảm bảo `Status = 'Active'` hoặc `Status = '1'`

#### C. Products không được gán vào Level 3 Categories
**Triệu chứng:** 
- Có categories nhưng không có products
- Khi click Level 3 category, không hiển thị sản phẩm

**Giải pháp:**
- Đảm bảo tất cả Products phải có `CategoryID` trỏ đến Level 3 categories
- Kiểm tra trong database: Products phải có `CategoryID` là ID của Level 3 category

#### D. Logic filter quá strict
**Triệu chứng:** 
- Có products nhưng không hiển thị khi chọn category
- Server log hiển thị "Category is not Level 3"

**Giải pháp:**
- Chỉ click vào Level 3 categories để filter
- Level 1 và Level 2 không filter sản phẩm (theo thiết kế)

### 4. Kiểm tra Database

Chạy các query sau để kiểm tra:

```sql
-- Kiểm tra Level 1 Categories
SELECT * FROM Category WHERE ParentCategoryID IS NULL OR Level = 1;

-- Kiểm tra Level 2 Categories
SELECT c.* FROM Category c 
INNER JOIN Category p ON c.ParentCategoryID = p.CategoryID 
WHERE p.ParentCategoryID IS NULL OR p.Level = 1;

-- Kiểm tra Level 3 Categories
SELECT c.* FROM Category c 
INNER JOIN Category p ON c.ParentCategoryID = p.CategoryID 
WHERE (p.ParentCategoryID IS NOT NULL OR p.Level = 2);

-- Kiểm tra Products
SELECT * FROM Product WHERE Status = 'Active' OR Status = '1';

-- Kiểm tra Products có CategoryID
SELECT p.*, c.CategoryName, c.Level 
FROM Product p 
LEFT JOIN Category c ON p.CategoryID = c.CategoryID;

-- Kiểm tra Products trong Level 3
SELECT p.*, c.CategoryName, c.Level 
FROM Product p 
INNER JOIN Category c ON p.CategoryID = c.CategoryID 
WHERE c.Level = 3 OR (c.ParentCategoryID IS NOT NULL AND 
    EXISTS (SELECT 1 FROM Category p2 WHERE p2.CategoryID = c.ParentCategoryID 
            AND (p2.ParentCategoryID IS NOT NULL OR p2.Level = 2)));
```

### 5. Kiểm tra Server Log chi tiết

Tìm các dòng log sau trong server log:

```
CategoryMBean.init() - Loaded X Level 1 categories
CategoryMBean.init() - Loaded X total active categories
CustomerProductMBean - Cached X active categories
CustomerProductMBean - Cached X active products
CustomerProductMBean.getProducts() - Total products in cache: X
CustomerProductMBean.getProducts() - No category filter, showing all products
CustomerProductMBean.filterProductsByCategory() - Starting filter for categoryId: X
CustomerProductMBean.filterProductsByCategory() - Category level: X
CustomerProductMBean.filterProductsByCategory() - Filtered products count: X
```

### 6. Các lỗi thường gặp:

1. **"Category not found"**: Category ID không tồn tại trong database
2. **"Category is not Level 3"**: Đang cố filter bằng Level 1 hoặc Level 2
3. **"No products found"**: Không có sản phẩm nào match với filter
4. **"Cached 0 active categories"**: Không có categories active trong database
5. **"Cached 0 active products"**: Không có products active trong database

### 7. Cách sửa nhanh:

1. **Nếu không có categories**: Thêm dữ liệu Category vào database
2. **Nếu không có products**: Thêm dữ liệu Product vào database
3. **Nếu products không hiển thị**: Đảm bảo Products có CategoryID trỏ đến Level 3 categories
4. **Nếu click category không filter**: Chỉ click vào Level 3 categories (cấp cuối cùng)



