package br.com.rpires.dao;
import java.util.*;
import br.com.rpires.domain.Cliente;
public class ClienteDaoMock implements IClienteDAO {
    private final Map<Long,Cliente> itens = new HashMap<>();
    @Override public Boolean cadastrar(Cliente item) { itens.put(item.getCpf(),item); return true; }
    @Override public void excluir(Long chave) { itens.remove(chave); }
    @Override public void alterar(Cliente item) { itens.put(item.getCpf(),item); }
    @Override public Cliente consultar(Long chave) { return itens.get(chave); }
    @Override public Collection<Cliente> buscarTodos() { return new ArrayList<>(itens.values()); }
}
