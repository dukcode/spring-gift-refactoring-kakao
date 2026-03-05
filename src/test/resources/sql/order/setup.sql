INSERT INTO member (email, password, point) VALUES ('rich@test.com', 'password', 10000000);
INSERT INTO member (email, password, point) VALUES ('poor@test.com', 'password', 100);
INSERT INTO category (name, color, image_url, description)
    VALUES ('식품', '#000000', 'https://example.com/food.jpg', '식품 카테고리');
INSERT INTO product (name, price, image_url, category_id)
    VALUES ('제주 감귤', 25000, 'https://example.com/jeju.jpg',
            (SELECT id FROM category WHERE name = '식품'));
INSERT INTO options (product_id, name, quantity)
    VALUES ((SELECT id FROM product WHERE name = '제주 감귤'), '일반 감귤', 10);
