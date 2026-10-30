package br.com.rpires;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.Instant;
import java.sql.*;
import java.util.concurrent.*;
import br.com.rpires.dao.*;
import br.com.rpires.dao.generic.jdbc.ConnectionFactory;
import br.com.rpires.domain.*;
import br.com.rpires.domain.Venda.Status;
import br.com.rpires.exceptions.DAOException;

public class EstoqueTest extends BancoTeste {
    private final IProdutoDAO produtos = new ProdutoDAO();
    private final IEstoqueDAO estoque = new EstoqueDAO();
    private final IVendaDAO vendas = new VendaDAO();
    private Cliente criarCliente() throws Exception {
        Cliente c = NovosCamposTest.cliente(); new ClienteDAO().cadastrar(c); return c;
    }
    private Produto criarProduto(String codigo, int qtd) throws Exception {
        Produto p = NovosCamposTest.produto(codigo); produtos.cadastrar(p,qtd); return p;
    }
    private Venda venda(String codigo, Cliente c, Produto p, int qtd) {
        Venda v = new Venda(); v.setCodigo(codigo); v.setCliente(c); v.setDataVenda(Instant.now());
        v.setStatus(Status.INICIADA); v.adicionarProduto(p,qtd); return v;
    }
    @Test public void produtoCadastraQuantidadeInicial() throws Exception {
        Produto p = criarProduto("P1",10); assertEquals(p.getId(), estoque.consultar("P1").getIdProduto());
        assertEquals(Integer.valueOf(10), estoque.consultar("P1").getQuantidade());
    }
    @Test public void cadastroSemQuantidadeCriaSaldoZero() throws Exception {
        produtos.cadastrar(NovosCamposTest.produto("P1"));
        assertEquals(Integer.valueOf(0), estoque.consultar("P1").getQuantidade());
    }
    @Test public void entradaSomaSaldo() throws Exception {
        criarProduto("P1",10); estoque.adicionar("P1",3);
        assertEquals(Integer.valueOf(13), estoque.consultar("P1").getQuantidade());
    }
    @Test public void vendaBaixaECarregaCamposNovos() throws Exception {
        Cliente c = criarCliente(); Produto p = criarProduto("P1",10); Venda v = venda("V1",c,p,3);
        vendas.cadastrar(v);
        assertEquals(Integer.valueOf(7), estoque.consultar("P1").getQuantidade());
        Venda salvo = vendas.consultar("V1");
        assertEquals(c.getEmail(), salvo.getCliente().getEmail());
        assertEquals(p.getCategoria(), salvo.getProdutos().iterator().next().getProduto().getCategoria());
        assertEquals(1, vendas.buscarTodos().size());
        assertEquals("Cozinha", vendas.buscarTodos().iterator().next().getProdutos().iterator().next().getProduto().getCategoria());
    }
    @Test public void insuficienteNaoSalvaVendaNemItens() throws Exception {
        Venda v = venda("V1", criarCliente(), criarProduto("P1",1),2);
        assertThrows(DAOException.class, () -> vendas.cadastrar(v));
        assertNull(v.getId()); assertNull(vendas.consultar("V1"));
        assertEquals(Integer.valueOf(1), estoque.consultar("P1").getQuantidade());
        assertEquals(0, contar("tb_produto_quantidade"));
    }
    @Test public void falhaNoSegundoItemDesfazPrimeiraBaixa() throws Exception {
        Cliente c = criarCliente(); Produto p1 = criarProduto("P1",10); Produto p2 = criarProduto("P2",0);
        Venda v = venda("V1",c,p1,3); v.adicionarProduto(p2,1);
        assertThrows(DAOException.class, () -> vendas.cadastrar(v));
        assertNull(vendas.consultar("V1")); assertEquals(0,contar("tb_produto_quantidade"));
        assertEquals(Integer.valueOf(10), estoque.consultar("P1").getQuantidade());
    }
    @Test public void cancelarDevolveUmaUnicaVezMesmoComObjetoAntigo() throws Exception {
        Venda v = venda("V1",criarCliente(),criarProduto("P1",10),3); vendas.cadastrar(v);
        Venda antigo = vendas.consultar("V1"); vendas.cancelarVenda(v); vendas.cancelarVenda(antigo);
        assertEquals(Integer.valueOf(10), estoque.consultar("P1").getQuantidade());
        assertEquals(Status.CANCELADA, vendas.consultar("V1").getStatus());
        assertThrows(DAOException.class, () -> vendas.finalizarVenda(v));
    }
    @Test public void finalizarNaoBaixaDuasVezes() throws Exception {
        Venda v = venda("V1",criarCliente(),criarProduto("P1",10),3); vendas.cadastrar(v);
        vendas.finalizarVenda(v); vendas.finalizarVenda(v);
        assertEquals(Integer.valueOf(7), estoque.consultar("P1").getQuantidade());
        assertEquals(Status.CONCLUIDA, vendas.consultar("V1").getStatus());
        assertThrows(DAOException.class, () -> vendas.cancelarVenda(v));
    }
    @Test public void quantidadeNegativaNaoCadastraProduto() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> produtos.cadastrar(NovosCamposTest.produto("P1"),-1));
        assertNull(produtos.consultar("P1")); assertNull(estoque.consultar("P1"));
    }
    @Test public void produtoEEstoqueSaoAtomicos() throws Exception {
        // Uma falha de constraint no estoque ocorre DEPOIS do INSERT do produto.
        try (Connection c = ConnectionFactory.getConnection(); Statement s = c.createStatement()) {
            s.execute("ALTER TABLE tb_estoque ADD CONSTRAINT limite_teste CHECK (quantidade<=5)");
        }
        try {
            Produto p = NovosCamposTest.produto("P1");
            assertThrows(DAOException.class, () -> produtos.cadastrar(p,6));
            assertNull(p.getId()); assertNull(produtos.consultar("P1")); assertNull(estoque.consultar("P1"));
        } finally {
            try (Connection c = ConnectionFactory.getConnection(); Statement s = c.createStatement()) {
                s.execute("ALTER TABLE tb_estoque DROP CONSTRAINT limite_teste");
            }
        }
    }
    @Test public void codigoDuplicadoNaoCriaOutroEstoque() throws Exception {
        criarProduto("P1",10);
        assertThrows(DAOException.class, () -> produtos.cadastrar(NovosCamposTest.produto("P1"),2));
        assertEquals(Integer.valueOf(10), estoque.consultar("P1").getQuantidade());
        assertEquals(1,contar("tb_produto")); assertEquals(1,contar("tb_estoque"));
    }
    @Test public void rejeitarEntradaNaoPositivaEProdutoAusente() throws Exception {
        criarProduto("P1",10);
        assertThrows(IllegalArgumentException.class, () -> estoque.adicionar("P1",0));
        assertThrows(IllegalArgumentException.class, () -> estoque.adicionar("P1",-1));
        assertThrows(DAOException.class, () -> estoque.adicionar("ausente",1));
        assertNull(estoque.consultar("ausente"));
    }
    @Test public void duasVendasDisputamUltimaUnidade() throws Exception {
        Cliente c = criarCliente(); Produto p = criarProduto("P1",1);
        ExecutorService pool = Executors.newFixedThreadPool(2); CountDownLatch largada = new CountDownLatch(1);
        Callable<Boolean> primeira = () -> tentarVender(venda("V1",c,p,1),largada);
        Callable<Boolean> segunda = () -> tentarVender(venda("V2",c,p,1),largada);
        try {
            Future<Boolean> a = pool.submit(primeira); Future<Boolean> b = pool.submit(segunda); largada.countDown();
            int sucessos = (a.get(20,TimeUnit.SECONDS)?1:0)+(b.get(20,TimeUnit.SECONDS)?1:0);
            assertEquals(1,sucessos); assertEquals(Integer.valueOf(0),estoque.consultar("P1").getQuantidade());
            assertEquals(1,contar("tb_venda")); assertEquals(1,contar("tb_produto_quantidade"));
        } finally { pool.shutdownNow(); }
    }
    private boolean tentarVender(Venda v, CountDownLatch largada) throws Exception {
        largada.await(); try { return new VendaDAO().cadastrar(v); } catch (DAOException ex) { return false; }
    }
    private int contar(String tabela) throws SQLException {
        try (Connection c = ConnectionFactory.getConnection(); Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT count(*) FROM "+tabela)) { rs.next(); return rs.getInt(1); }
    }
}
