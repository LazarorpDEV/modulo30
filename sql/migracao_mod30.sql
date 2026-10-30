-- Somente para banco ja criado com o schema ORIGINAL do professor do modulo 30.
-- Nao aplicar ao banco simplificado do modulo 29 (tb_cliente_2).
BEGIN;
ALTER TABLE tb_cliente ADD COLUMN IF NOT EXISTS email VARCHAR(150);
ALTER TABLE tb_produto ADD COLUMN IF NOT EXISTS categoria VARCHAR(100);
CREATE TABLE IF NOT EXISTS tb_estoque (
 id_produto_fk BIGINT PRIMARY KEY REFERENCES tb_produto(id) ON DELETE CASCADE,
 quantidade INTEGER NOT NULL CHECK (quantidade >= 0)
);
INSERT INTO tb_estoque (id_produto_fk,quantidade)
 SELECT id,0 FROM tb_produto ON CONFLICT (id_produto_fk) DO NOTHING;
COMMIT;
-- As quantidades de produtos antigos iniciam em zero: registre as entradas reais pelo EstoqueDAO.
