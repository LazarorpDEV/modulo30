package br.com.rpires.dao;
import br.com.rpires.domain.Estoque;
import br.com.rpires.exceptions.DAOException;
public interface IEstoqueDAO {
    Estoque consultar(String codigoProduto) throws DAOException;
    void adicionar(String codigoProduto, int quantidade) throws DAOException;
}
