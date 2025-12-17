-- ============================================
-- SCRIPT KIỂM TRA VÀ THÊM DỮ LIỆU MẪU
-- ============================================

-- 1. KIỂM TRA CATEGORIES
-- ============================================
SELECT '=== CHECKING CATEGORIES ===' AS Info;

-- Kiểm tra tất cả categories
SELECT 'Total Categories:' AS Info, COUNT(*) AS Count FROM Category;

-- Kiểm tra categories active
SELECT 'Active Categories:' AS Info, COUNT(*) AS Count 
FROM Category 
WHERE IsActive IS NULL OR IsActive = 1;

-- Kiểm tra Level 1 categories (ParentCategoryID = NULL VÀ Level = 1) - ĐÚNG THEO YÊU CẦU
SELECT 'Level 1 Categories (ParentCategoryID = NULL AND Level = 1):' AS Info, COUNT(*) AS Count 
FROM Category 
WHERE ParentCategoryID IS NULL 
AND Level = 1
AND (IsActive IS NULL OR IsActive = 1);

-- Hiển thị Level 1 categories - ĐÚNG THEO YÊU CẦU
SELECT Category_ID, Category_Name, Level, ParentCategoryID, IsActive, SortOrder
FROM Category 
WHERE ParentCategoryID IS NULL 
AND Level = 1
AND (IsActive IS NULL OR IsActive = 1)
ORDER BY SortOrder;

-- Hiển thị tất cả categories với thông tin level
SELECT 
    c.Category_ID,
    c.Category_Name,
    c.Level,
    c.ParentCategoryID,
    p.Category_Name AS ParentName,
    c.IsActive,
    c.SortOrder
FROM Category c
LEFT JOIN Category p ON c.ParentCategoryID = p.Category_ID
WHERE c.IsActive IS NULL OR c.IsActive = 1
ORDER BY c.Level, c.SortOrder;

-- ============================================
-- 2. KIỂM TRA PRODUCTS
-- ============================================
SELECT '=== CHECKING PRODUCTS ===' AS Info;

-- Kiểm tra tất cả products
SELECT 'Total Products:' AS Info, COUNT(*) AS Count FROM Product;

-- Kiểm tra products active
SELECT 'Active Products:' AS Info, COUNT(*) AS Count 
FROM Product 
WHERE Status = 'Active' OR Status = '1' OR Status IS NULL;

-- Hiển thị products với category info
SELECT 
    p.Product_ID,
    p.Name,
    p.Status,
    p.UnitPrice,
    p.Stock,
    p.CategoryID,
    c.Category_Name,
    c.Level AS CategoryLevel
FROM Product p
LEFT JOIN Category c ON p.CategoryID = c.Category_ID
WHERE p.Status = 'Active' OR p.Status = '1' OR p.Status IS NULL
ORDER BY p.Product_ID;

-- Kiểm tra products có CategoryID
SELECT 'Products with CategoryID:' AS Info, COUNT(*) AS Count 
FROM Product 
WHERE CategoryID IS NOT NULL 
AND (Status = 'Active' OR Status = '1' OR Status IS NULL);

-- Kiểm tra products KHÔNG có CategoryID
SELECT 'Products WITHOUT CategoryID:' AS Info, COUNT(*) AS Count 
FROM Product 
WHERE CategoryID IS NULL 
AND (Status = 'Active' OR Status = '1' OR Status IS NULL);

-- ============================================
-- 3. KIỂM TRA CẤU TRÚC 3 CẤP
-- ============================================
SELECT '=== CHECKING 3-LEVEL STRUCTURE ===' AS Info;

-- Level 1 (ParentCategoryID = NULL)
SELECT 'Level 1 Categories:' AS Info, COUNT(*) AS Count 
FROM Category 
WHERE ParentCategoryID IS NULL 
AND (IsActive IS NULL OR IsActive = 1);

-- Level 2 (Parent là Level 1)
SELECT 'Level 2 Categories:' AS Info, COUNT(*) AS Count 
FROM Category c
INNER JOIN Category p ON c.ParentCategoryID = p.Category_ID
WHERE (p.ParentCategoryID IS NULL OR p.Level = 1)
AND (c.IsActive IS NULL OR c.IsActive = 1);

-- Level 3 (Parent là Level 2)
SELECT 'Level 3 Categories:' AS Info, COUNT(*) AS Count 
FROM Category c
INNER JOIN Category p ON c.ParentCategoryID = p.Category_ID
WHERE (p.ParentCategoryID IS NOT NULL OR p.Level = 2)
AND (c.IsActive IS NULL OR c.IsActive = 1);

-- Products trong Level 3 categories
SELECT 'Products in Level 3:' AS Info, COUNT(*) AS Count 
FROM Product p
INNER JOIN Category c ON p.CategoryID = c.Category_ID
INNER JOIN Category p2 ON c.ParentCategoryID = p2.Category_ID
WHERE (p2.ParentCategoryID IS NOT NULL OR p2.Level = 2)
AND (p.Status = 'Active' OR p.Status = '1' OR p.Status IS NULL)
AND (c.IsActive IS NULL OR c.IsActive = 1);

-- ============================================
-- 4. THÊM DỮ LIỆU MẪU (NẾU CẦN)
-- ============================================
-- Chỉ chạy nếu database trống!

/*
-- Thêm Level 1 Categories
INSERT INTO Category (Category_Name, Level, IsActive, SortOrder, CreatedAt)
VALUES 
    ('THỊT, CÁ, TRỨNG, HẢI SẢN', 1, 1, 1, GETDATE()),
    ('RAU, CỦ, NẤM, TRÁI CÂY', 1, 1, 2, GETDATE()),
    ('DẦU ĂN, NƯỚC CHẤM, GIA VỊ', 1, 1, 3, GETDATE()),
    ('GẠO, BỘT, ĐỒ KHÔ', 1, 1, 4, GETDATE());

-- Lấy ID của Level 1 vừa tạo (giả sử ID = 1 cho "THỊT, CÁ, TRỨNG, HẢI SẢN")
-- Thêm Level 2 Categories
INSERT INTO Category (Category_Name, ParentCategoryID, Level, IsActive, SortOrder, CreatedAt)
VALUES 
    ('Thịt heo', 1, 2, 1, 1, GETDATE()),
    ('Thịt bò', 1, 2, 1, 2, GETDATE()),
    ('Thịt gà, vịt', 1, 2, 1, 3, GETDATE());

-- Lấy ID của Level 2 vừa tạo (giả sử ID = 5 cho "Thịt heo")
-- Thêm Level 3 Categories
INSERT INTO Category (Category_Name, ParentCategoryID, Level, IsActive, SortOrder, CreatedAt)
VALUES 
    ('Ba rọi', 5, 3, 1, 1, GETDATE()),
    ('Sườn', 5, 3, 1, 2, GETDATE()),
    ('Bắp, chân giò', 5, 3, 1, 3, GETDATE()),
    ('Xương', 5, 3, 1, 4, GETDATE()),
    ('Thịt xay', 5, 3, 1, 5, GETDATE());

-- Thêm Products mẫu (gán vào Level 3 category)
-- Lấy ID của Level 3 vừa tạo (giả sử ID = 8 cho "Ba rọi")
INSERT INTO Product (Name, Description, UnitPrice, Status, Stock, CategoryID, CreatedAt)
VALUES 
    ('Ba rọi tươi', 'Ba rọi tươi ngon', 150000, 'Active', 100, 8, GETDATE()),
    ('Ba rọi nhập khẩu', 'Ba rọi nhập khẩu chất lượng', 200000, 'Active', 50, 8, GETDATE());
*/

-- ============================================
-- 5. SỬA LỖI THƯỜNG GẶP
-- ============================================

-- Sửa categories không có IsActive
UPDATE Category SET IsActive = 1 WHERE IsActive IS NULL;

-- Sửa products không có Status
UPDATE Product SET Status = 'Active' WHERE Status IS NULL OR Status = '';

-- Sửa Level cho categories (nếu chưa có)
-- Level 1: ParentCategoryID = NULL
UPDATE Category SET Level = 1 WHERE ParentCategoryID IS NULL AND Level IS NULL;

-- Level 2: Parent có Level = 1
UPDATE c SET c.Level = 2
FROM Category c
INNER JOIN Category p ON c.ParentCategoryID = p.Category_ID
WHERE p.Level = 1 AND c.Level IS NULL;

-- Level 3: Parent có Level = 2
UPDATE c SET c.Level = 3
FROM Category c
INNER JOIN Category p ON c.ParentCategoryID = p.Category_ID
WHERE p.Level = 2 AND c.Level IS NULL;

