-- Categories
INSERT INTO categories (name, sort_order, is_active) VALUES
    ('Trios Marinos',    1, TRUE),
    ('Dúos Marinos',     2, TRUE),
    ('Frituras',         3, TRUE),
    ('Platos Solos',     4, TRUE),
    ('Sopas',            5, TRUE),
    ('Chicharrones',     6, TRUE),
    ('Fuentes Marinas',  7, TRUE),
    ('Bebidas',          8, TRUE);

-- Trios Marinos
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Arroz c/ marisco, ceviche y chicharrón de pota',    20.00, TRUE, (SELECT id FROM categories WHERE name = 'Trios Marinos')),
    ('Arroz c/ marisco, ceviche y chicharrón de pescado', 25.00, TRUE, (SELECT id FROM categories WHERE name = 'Trios Marinos')),
    ('Arroz c/ chaufa, ceviche y chicharrón de pota',     20.00, TRUE, (SELECT id FROM categories WHERE name = 'Trios Marinos')),
    ('Arroz c/ chaufa, ceviche y chicharrón de pescado',  25.00, TRUE, (SELECT id FROM categories WHERE name = 'Trios Marinos'));

-- Dúos Marinos
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Ceviche c/ chicharrón de pota',              25.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Ceviche c/ chicharrón de pescado',           30.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Ceviche c/ arroz con mariscos',              25.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Ceviche c/ arroz chaufa',                    25.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Arroz c/ mariscos y chicharrón de pota',     25.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Arroz c/ mariscos y chicharrón de pescado',  30.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Arroz chaufa y chicharrón de pota',          25.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos')),
    ('Arroz chaufa y chicharrón de pescado',       30.00, TRUE, (SELECT id FROM categories WHERE name = 'Dúos Marinos'));

-- Frituras
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Trucha frita c/ arroz, yuca  y ensalada', 15.00, TRUE, (SELECT id FROM categories WHERE name = 'Frituras')),
    ('Pescado frito c/ arroz, yuca y ensalada',  10.00, TRUE, (SELECT id FROM categories WHERE name = 'Frituras'));

-- Platos Solos
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Ceviche solo',       20.00, TRUE, (SELECT id FROM categories WHERE name = 'Platos Solos')),
    ('Leche de Tigre',     15.00, TRUE, (SELECT id FROM categories WHERE name = 'Platos Solos')),
    ('Arroz con Mariscos', 18.00, TRUE, (SELECT id FROM categories WHERE name = 'Platos Solos')),
    ('Chaufa de Mariscos', 18.00, TRUE, (SELECT id FROM categories WHERE name = 'Platos Solos')),
    ('Chaufa de Pescado',  25.00, TRUE, (SELECT id FROM categories WHERE name = 'Platos Solos')),
    ('Causa Acevichada',   15.00, TRUE, (SELECT id FROM categories WHERE name = 'Platos Solos'));

-- Sopas
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Chilcano Especial',     12.00, TRUE, (SELECT id FROM categories WHERE name = 'Sopas')),
    ('Sudado',                15.00, TRUE, (SELECT id FROM categories WHERE name = 'Sopas')),
    ('Parihuela',             20.00, TRUE, (SELECT id FROM categories WHERE name = 'Sopas')),
    ('Chupe de Langostino',   15.00, TRUE, (SELECT id FROM categories WHERE name = 'Sopas')),
    ('Chupe de Pescado',      15.00, TRUE, (SELECT id FROM categories WHERE name = 'Sopas')),
    ('Chupe Acevichado',      20.00, TRUE, (SELECT id FROM categories WHERE name = 'Sopas'));

-- Chicharrones
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Chicharrón de Pota',    20.00, TRUE, (SELECT id FROM categories WHERE name = 'Chicharrones')),
    ('Chicharrón de Pescado', 30.00, TRUE, (SELECT id FROM categories WHERE name = 'Chicharrones')),
    ('Jalea Mixta',           30.00, TRUE, (SELECT id FROM categories WHERE name = 'Chicharrones'));

-- Fuentes Marinas
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Ceviche, arroz con mariscos /chaufa y chicharrón de pota / pescado.', 65.00, TRUE, (SELECT id FROM categories WHERE name = 'Fuentes Marinas')),
    ('Ceviche solo o con Chicharrón',                                        65.00, TRUE, (SELECT id FROM categories WHERE name = 'Fuentes Marinas')),
    ('Arroz con mariscos o chaufa de mariscos  Chicharrón',                  65.00, TRUE, (SELECT id FROM categories WHERE name = 'Fuentes Marinas'));

-- Bebidas
INSERT INTO products (name, price, is_available, category_id) VALUES
    ('Gaseosa Personal',  2.00,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Agua Personal',     2.00,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Gordita',           5.00,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('1 Lt. Coca / Inca', 6.50,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('1 1/2 Coca / Inca', 8.50,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('3 Lt Coca / Inca',  15.00, TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Maracuya 1 Lt.',    6.00,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Chicha 1 Lt.',      6.00,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Cerveza Pilsen',    8.00,  TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Cerveza Negrita',   10.00, TRUE, (SELECT id FROM categories WHERE name = 'Bebidas')),
    ('Cerveza de Trigo',  10.00, TRUE, (SELECT id FROM categories WHERE name = 'Bebidas'));
