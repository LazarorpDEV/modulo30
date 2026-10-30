package br.com.rpires.dao;
import java.sql.*;
import br.com.rpires.dao.generic.jdbc.ConnectionFactory;
import br.com.rpires.domain.Estoque;
import br.com.rpires.exceptions.DAOException;
public class EstoqueDAO implements IEstoqueDAO {
    @Override public Estoque consultar(String codigo) throws DAOException {
        String sql = "SELECT E.id_produto_fk, E.quantidade FROM tb_estoque E JOIN tb_produto P ON P.id=E.id_produto_fk WHERE P.codigo=?";
        try (Connection con = ConnectionFactory.getConnection(); PreparedStatement stm = con.prepareStatement(sql)) {
            stm.setString(1, codigo);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) return null;
                Estoque e = new Estoque(); e.setIdProduto(rs.getLong(1)); e.setQuantidade(rs.getInt(2)); return e;
            }
        } catch (SQLException ex) { throw new DAOException("Erro consultando estoque", ex); }
    }
    @Override public void adicionar(String codigo, int quantidade) throws DAOException {
        if (quantidade <= 0) throw new IllegalArgumentException("Entrada deve ser positiva");
        Transacao.executar(con -> {
            try (PreparedStatement stm = con.prepareStatement("UPDATE tb_estoque SET quantidade=quantidade+? WHERE id_produto_fk=(SELECT id FROM tb_produto WHERE codigo=?)")) {
                stm.setInt(1, quantidade); stm.setString(2, codigo);
                if (stm.executeUpdate() != 1) throw new SQLException("Produto/estoque nao encontrado");
            }
            return null;
        });
    }
    // Usa a mesma conexao da venda: saldo, itens e venda sao confirmados juntos.
    static void baixar(Connection con, long idProduto, int quantidade) throws SQLException {
        if (quantidade <= 0) throw new SQLException("Quantidade de venda deve ser positiva");
        try (PreparedStatement stm = con.prepareStatement("UPDATE tb_estoque SET quantidade=quantidade-? WHERE id_produto_fk=? AND quantidade>=?")) {
            stm.setInt(1, quantidade); stm.setLong(2, idProduto); stm.setInt(3, quantidade);
            if (stm.executeUpdate() != 1) throw new SQLException("Estoque insuficiente para produto " + idProduto);
        }
    }
    static void devolver(Connection con, long idProduto, int quantidade) throws SQLException {
        try (PreparedStatement stm = con.prepareStatement("UPDATE tb_estoque SET quantidade=quantidade+? WHERE id_produto_fk=?")) {
            stm.setInt(1, quantidade); stm.setLong(2, idProduto);
            if (stm.executeUpdate() != 1) throw new SQLException("Estoque nao encontrado");
        }
    }
}
