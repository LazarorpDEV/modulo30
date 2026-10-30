CREATE SEQUENCE IF NOT EXISTS sq_cliente START WITH 1;
CREATE SEQUENCE IF NOT EXISTS sq_produto START WITH 1;
CREATE SEQUENCE IF NOT EXISTS sq_venda START WITH 1;
CREATE SEQUENCE IF NOT EXISTS sq_produto_quantidade START WITH 1;
CREATE TABLE IF NOT EXISTS tb_cliente (
 id BIGINT DEFAULT nextval('sq_cliente') PRIMARY KEY,
 nome VARCHAR(100) NOT NULL, cpf BIGINT NOT NULL UNIQUE, tel BIGINT NOT NULL,
 endereco VARCHAR(100) NOT NULL, numero INTEGER NOT NULL, cidade VARCHAR(100) NOT NULL,
 estado VARCHAR(50) NOT NULL, email VARCHAR(150)
);
CREATE TABLE IF NOT EXISTS tb_produto (
 id BIGINT DEFAULT nextval('sq_produto') PRIMARY KEY,
 codigo VARCHAR(50) NOT NULL UNIQUE, nome VARCHAR(100) NOT NULL,
 descricao VARCHAR(255) NOT NULL, valor NUMERIC(12,2) NOT NULL CHECK (valor >= 0), categoria VARCHAR(100)
);
CREATE TABLE IF NOT EXISTS tb_estoque (
 id_produto_fk BIGINT PRIMARY KEY REFERENCES tb_produto(id) ON DELETE CASCADE,
 quantidade INTEGER NOT NULL CHECK (quantidade >= 0)
);
CREATE TABLE IF NOT EXISTS tb_venda (
 id BIGINT DEFAULT nextval('sq_venda') PRIMARY KEY, codigo VARCHAR(50) NOT NULL UNIQUE,
 id_cliente_fk BIGINT NOT NULL REFERENCES tb_cliente(id), valor_total NUMERIC(12,2) NOT NULL CHECK (valor_total >= 0),
 data_venda TIMESTAMP WITH TIME ZONE NOT NULL,
 status_venda VARCHAR(20) NOT NULL CHECK (status_venda IN ('INICIADA','CONCLUIDA','CANCELADA'))
);
CREATE TABLE IF NOT EXISTS tb_produto_quantidade (
 id BIGINT DEFAULT nextval('sq_produto_quantidade') PRIMARY KEY,
 id_produto_fk BIGINT NOT NULL REFERENCES tb_produto(id),
 id_venda_fk BIGINT NOT NULL REFERENCES tb_venda(id), quantidade INTEGER NOT NULL CHECK (quantidade > 0),
 valor_total NUMERIC(12,2) NOT NULL CHECK (valor_total >= 0), UNIQUE (id_venda_fk,id_produto_fk)
);
