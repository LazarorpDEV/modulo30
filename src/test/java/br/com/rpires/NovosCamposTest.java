package br.com.rpires;
import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import br.com.rpires.dao.*;
import br.com.rpires.domain.*;

public class NovosCamposTest extends BancoTeste {
    static Cliente cliente() {
        Cliente c = new Cliente(); c.setCpf(12312312312L); c.setNome("Lazaro"); c.setTel(73999999999L);
        c.setEnd("Rua A"); c.setNumero(10); c.setCidade("Teixeira de Freitas"); c.setEstado("BA");
        c.setEmail("lazaro@exemplo.com"); return c;
    }
    static Produto produto(String codigo) {
        Produto p = new Produto(); p.setCodigo(codigo); p.setNome("Panela"); p.setDescricao("Panela de ferro");
        p.setValor(new BigDecimal("50.00")); p.setCategoria("Cozinha"); return p;
    }
    @Test public void cadastrarConsultarEmail() throws Exception {
        IClienteDAO dao = new ClienteDAO(); Cliente c = cliente(); assertTrue(dao.cadastrar(c));
        assertEquals(c.getEmail(), dao.consultar(c.getCpf()).getEmail());
    }
    @Test public void atualizarEmail() throws Exception {
        IClienteDAO dao = new ClienteDAO(); Cliente c = cliente(); dao.cadastrar(c);
        c.setEmail("novo@exemplo.com"); dao.alterar(c);
        assertEquals("novo@exemplo.com", dao.consultar(c.getCpf()).getEmail());
    }
    @Test public void listarEmail() throws Exception {
        IClienteDAO dao = new ClienteDAO(); dao.cadastrar(cliente());
        assertEquals("lazaro@exemplo.com", dao.buscarTodos().iterator().next().getEmail());
    }
    @Test public void cadastrarConsultarCategoria() throws Exception {
        IProdutoDAO dao = new ProdutoDAO(); Produto p = produto("P1"); assertTrue(dao.cadastrar(p));
        assertEquals("Cozinha", dao.consultar("P1").getCategoria());
    }
    @Test public void atualizarCategoria() throws Exception {
        IProdutoDAO dao = new ProdutoDAO(); Produto p = produto("P1"); dao.cadastrar(p);
        p.setCategoria("Utensilios"); dao.alterar(p);
        assertEquals("Utensilios", dao.consultar("P1").getCategoria());
    }
    @Test public void listarCategoria() throws Exception {
        IProdutoDAO dao = new ProdutoDAO(); dao.cadastrar(produto("P1"));
        assertEquals("Cozinha", dao.buscarTodos().iterator().next().getCategoria());
    }
    @Test public void atualizarDadosCopiaCamposNovos() {
        Cliente c = new Cliente(); new ClienteDAO().atualiarDados(cliente(), c);
        Produto p = new Produto(); new ProdutoDAO().atualiarDados(produto("P1"), p);
        assertEquals("lazaro@exemplo.com", c.getEmail()); assertEquals("Cozinha", p.getCategoria());
    }
}
