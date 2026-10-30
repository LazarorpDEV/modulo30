/**
 * 
 */
package br.com.rpires.dao;

import br.com.rpires.dao.generic.IGenericDAO;
import br.com.rpires.domain.Produto;

/**
 * @author rodrigo.pires
 *
 */
public interface IProdutoDAO extends IGenericDAO<Produto, String>{
    Boolean cadastrar(Produto produto, int quantidadeInicial) throws br.com.rpires.exceptions.TipoChaveNaoEncontradaException, br.com.rpires.exceptions.DAOException;


}
