INSERT INTO menu_items (name, category, price) VALUES
('Ube + Sago', 'Pampalamigs', 195),
('Mango + Pomelo + Sago', 'Pampalamigs', 205),
('Buko + Pandan + Sago', 'Pampalamigs', 200),
('Choco-nut Shake', 'Pampalamigs', 225),
('Iced Coffee + Black Sago', 'Pampalamigs', 225),
('NamNam Halo-Halo', 'Pampalamigs', 205),
('Mais Con Hielo Con Queso', 'Pampalamigs', 195),

('!Popular House Crispy Sisig', 'Paboritos', 225),
('!Popular Sinigang na Beef Short Rib & Watermelon', 'Paboritos', 315),
('Kare-Kare with Oxtail', 'Paboritos', 515),
('Shiitake and Tokwa Kare-Kare', 'Paboritos', 375),
('Sinigang na Baboy sa Sampaloc', 'Paboritos', 225),
('Crispy Pata with Garlic Chili Bits', 'Paboritos', 415),
('Overloaded Garlicky Chicken & Pork', 'Paboritos', 225),
('House Squid Sisig', 'Paboritos', 235),

('Crispy Lumpiang Ubod', 'Pica-Pica', 160),
('Caramelized Patis Wings', 'Pica-Pica', 230),
('Fresh Lumpiang Ubod', 'Pica-Pica', 125),
('Beef Salpicao & Garlic', 'Pica-Pica', 305),
('Crunchy Salt & Pepper Squid', 'Pica-Pica', 210),
('Tokwa''t Baboy', 'Pica-Pica', 130),  -- escaped apostrophe

('Crispy Pancit Palabok', 'Pancit', 265),
('Pancit Bihon', 'Pancit', 215),
('Pancit Sisig', 'Pancit', 260),

('Chicken Inasal Skewers', 'Ulam lupa & dagat', 185),
('Crispy Pork Kawali', 'Ulam lupa & dagat', 275),
('Sizzling Corned Beef Belly Kansi', 'Ulam lupa & dagat', 345),
('Beefy Belly Bulalo', 'Ulam lupa & dagat', 345),
('Sinigang na Salmon Head sa Miso', 'Ulam lupa & dagat', 315),
('Sinigang na Hipon sa Green Mango', 'Ulam lupa & dagat', 305),
('Overloaded Garlic Bangus Belly', 'Ulam lupa & dagat', 245),
('Inihaw na Baboy', 'Ulam lupa & dagat', 270),

('White Rice', 'Kanin', 105),
('Brown Rice', 'Kanin', 140),
('Garlic Rice', 'Kanin', 115),
('Bagoong Rice', 'Kanin', 150);




ALTER TABLE orders 
ADD COLUMN notes TEXT,
ADD COLUMN pwd_name VARCHAR(100),
ADD COLUMN pwd_id VARCHAR(20),
ADD COLUMN payment_method VARCHAR(20),
ADD COLUMN cash_tendered DECIMAL(10,2),
ADD COLUMN change_amount DECIMAL(10,2);
