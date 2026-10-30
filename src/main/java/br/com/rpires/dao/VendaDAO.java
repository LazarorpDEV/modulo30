package br.com.rpires.dao;

import java.sql.*;
import java.util.*;
import br.com.rpires.dao.factory.*;
import br.com.rpires.dao.generic.GenericDAO;
import br.com.rpires.domain.*;
import br.com.rpires.domain.Venda.Status;
import br.com.rpires.exceptions.*;

public class VendaDAO extends GenericDAO<Venda, String> implements IVendaDAO {
    @Override public Class<Venda> getTipoClasse() { return Venda.class; }
    @Override public void atualiarDados(Venda a, Venda b) { b.setCodigo(a.getCodigo()); b.setStatus(a.getStatus()); }
    @Override public void excluir(String codigo) { throw new UnsupportedOperationException("Cancele a venda para devolver o estoque"); }

    @Override public Boolean cadastrar(Venda venda) throws TipoChaveNaoEncontradaException, DAOException {
        if (venda.getStatus() != Status.INICIADA || venda.getProdutos().isEmpty())
            throw new IllegalArgumentException("Venda deve estar iniciada e ter produtos");
        Long idAnterior = venda.getId();
        try {
            return Transacao.executar(con -> {
                venda.recalcularValorTotalVenda();
                if (!inserir(con, venda)) throw new SQLException("Venda nao cadastrada");
                // Ordem fixa evita bloqueios invertidos em vendas com varios produtos.
                List<ProdutoQuantidade> itens = new ArrayList<>(venda.getProdutos());
                itens.sort(Comparator.comparing(item -> item.getProduto().getId()));
                for (ProdutoQuantidade item : itens) {
                    EstoqueDAO.baixar(con, item.getProduto().getId(), item.getQuantidade());
                    try (PreparedStatement stm = con.prepareStatement(
                            "INSERT INTO tb_produto_quantidade (id,id_produto_fk,id_venda_fk,quantidade,valor_total) VALUES (nextval('sq_produto_quantidade'),?,?,?,?)")) {
                        stm.setLong(1, item.getProduto().getId()); stm.setLong(2, venda.getId());
                        stm.setInt(3, item.getQuantidade()); stm.setBigDecimal(4, item.getValorTotal());
                        stm.executeUpdate();
                    }
                }
                return true;
            });
        } catch (DAOException ex) { venda.setId(idAnterior); throw ex; }
    }

    @Override public void finalizarVenda(Venda venda) throws TipoChaveNaoEncontradaException, DAOException {
        Transacao.executar(con -> {
            Status atual = bloquearVenda(con, venda.getId());
            if (atual == Status.CANCELADA) throw new SQLException("Venda cancelada nao pode ser finalizada");
            atualizarStatus(con, venda.getId(), Status.CONCLUIDA);
            return null;
        });
        venda.setStatus(Status.CONCLUIDA);
    }

    @Override public void cancelarVenda(Venda venda) throws TipoChaveNaoEncontradaException, DAOException {
        Transacao.executar(con -> {
            Status atual = bloquearVenda(con, venda.getId());
            if (atual == Status.CANCELADA) return null; // Repetir nao duplica a devolucao.
            if (atual == Status.CONCLUIDA) throw new SQLException("Venda concluida nao pode ser cancelada");
            try (PreparedStatement stm = con.prepareStatement(
                    "SELECT id_produto_fk,quantidade FROM tb_produto_quantidade WHERE id_venda_fk=? ORDER BY id_produto_fk")) {
                stm.setLong(1, venda.getId());
                try (ResultSet rs = stm.executeQuery()) {
                    while (rs.next()) EstoqueDAO.devolver(con, rs.getLong(1), rs.getInt(2));
                }
            }
            atualizarStatus(con, venda.getId(), Status.CANCELADA);
            return null;
        });
        venda.setStatus(Status.CANCELADA);
    }
    private Status bloquearVenda(Connection con, Long id) throws SQLException {
        if (id == null) throw new SQLException("Venda deve estar cadastrada");
        try (PreparedStatement stm = con.prepareStatement("SELECT status_venda FROM tb_venda WHERE id=? FOR UPDATE")) {
            stm.setLong(1, id);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) throw new SQLException("Venda nao encontrada");
                return Status.valueOf(rs.getString(1));
            }
        }
    }
    private void atualizarStatus(Connection con, Long id, Status status) throws SQLException {
        try (PreparedStatement stm = con.prepareStatement("UPDATE tb_venda SET status_venda=? WHERE id=?")) {
            stm.setString(1, status.name()); stm.setLong(2, id); stm.executeUpdate();
        }
    }
    private String sqlBase() {
        return "SELECT V.id AS ID_VENDA,V.codigo,V.valor_total,V.data_venda,V.status_venda," +
            "C.id AS ID_CLIENTE,C.nome,C.cpf,C.tel,C.endereco,C.numero,C.cidade,C.estado,C.email " +
            "FROM tb_venda V JOIN tb_cliente C ON V.id_cliente_fk=C.id ";
    }
    @Override public Venda consultar(String codigo) throws MaisDeUmRegistroException, TableException, DAOException {
        try (Connection con = getConnection(); PreparedStatement stm = con.prepareStatement(sqlBase() + "WHERE V.codigo=?")) {
            stm.setString(1, codigo);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) return null;
                Venda venda = VendaFactory.convert(rs); carregarItens(con, venda); return venda;
            }
        } catch (SQLException ex) { throw new DAOException("Erro consultando venda", ex); }
    }
    @Override public Collection<Venda> buscarTodos() throws DAOException {
        List<Venda> vendas = new ArrayList<>();
        try (Connection con = getConnection(); PreparedStatement stm = con.prepareStatement(sqlBase()+"ORDER BY V.id");
                ResultSet rs = stm.executeQuery()) {
            while (rs.next()) {
                Venda venda = VendaFactory.convert(rs); carregarItens(con, venda); vendas.add(venda);
            }
            return vendas;
        } catch (SQLException ex) { throw new DAOException("Erro listando vendas", ex); }
    }
    private void carregarItens(Connection con, Venda venda) throws SQLException {
        String sql = "SELECT PQ.id,PQ.quantidade,PQ.valor_total,P.id AS ID_PRODUTO,P.codigo,P.nome,P.descricao,P.valor,P.categoria " +
            "FROM tb_produto_quantidade PQ JOIN tb_produto P ON PQ.id_produto_fk=P.id WHERE PQ.id_venda_fk=? ORDER BY P.id";
        try (PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setLong(1, venda.getId());
            try (ResultSet rs = stm.executeQuery()) {
                Set<ProdutoQuantidade> itens = new HashSet<>();
                while (rs.next()) itens.add(ProdutoQuantidadeFactory.convert(rs));
                venda.setProdutos(itens);
            }
        }
    }
	@Override
	protected String getQueryInsercao() {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO TB_VENDA ");
		sb.append("(ID, CODIGO, ID_CLIENTE_FK, VALOR_TOTAL, DATA_VENDA, STATUS_VENDA)");
		sb.append("VALUES (nextval('sq_venda'),?,?,?,?,?)");
		return sb.toString();
	}

	@Override
	protected void setParametrosQueryInsercao(PreparedStatement stmInsert, Venda entity) throws SQLException {
		stmInsert.setString(1, entity.getCodigo());
		stmInsert.setLong(2, entity.getCliente().getId());
		stmInsert.setBigDecimal(3, entity.getValorTotal());
		stmInsert.setTimestamp(4, Timestamp.from(entity.getDataVenda()));
		stmInsert.setString(5, entity.getStatus().name());
	}

	@Override
	protected String getQueryExclusao() {
		throw new UnsupportedOperationException("OPERAÇÃO NÃO PERMITIDA");
	}

	@Override
	protected void setParametrosQueryExclusao(PreparedStatement stmInsert, String valor) throws SQLException {
		throw new UnsupportedOperationException("OPERAÇÃO NÃO PERMITIDA");
	}

	@Override
	protected String getQueryAtualizacao() {
		throw new UnsupportedOperationException("OPERAÇÃO NÃO PERMITIDA");
	}

	@Override
	protected void setParametrosQueryAtualizacao(PreparedStatement stmUpdate, Venda entity) throws SQLException {
		throw new UnsupportedOperationException("OPERAÇÃO NÃO PERMITIDA");
	}

	@Override
	protected void setParametrosQuerySelect(PreparedStatement stm, String valor) throws SQLException {
		stm.setString(1, valor);
	}
	
	

}
