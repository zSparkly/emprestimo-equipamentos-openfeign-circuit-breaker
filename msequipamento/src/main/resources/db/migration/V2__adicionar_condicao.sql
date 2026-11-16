ALTER TABLE tb_equipamentos ADD COLUMN condicao VARCHAR(20) NOT NULL DEFAULT 'BOM';
ALTER TABLE tb_equipamentos ADD CONSTRAINT chk_condicao CHECK (condicao IN ('BOM', 'AVARIADO'));
