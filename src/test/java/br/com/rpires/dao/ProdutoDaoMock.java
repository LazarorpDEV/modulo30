package br.com.rpires.dao;
import java.util.*;
import br.com.rpires.domain.Produto;
public class ProdutoDaoMock implements IProdutoDAO {
    private final Map<String,Produto> itens = new HashMap<>();
    @Override public Boolean cadastrar(Produto item) { itens.put(item.getCodigo(),item); return true; }
    @Override public void excluir(String chave) { itens.remove(chave); }
    @Override public void alterar(Produto item) { itens.put(item.getCodigo(),item); }
    @Override public Produto consultar(String chave) { return itens.get(chave); }
    @Override public Collection<Produto> buscarTodos() { return new ArrayList<>(itens.values()); }
    @Override public Boolean cadastrar(Produto item, int quantidadeInicial) { return cadastrar(item); }
}
