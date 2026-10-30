package br.com.rpires.dao;
import java.sql.*;
import br.com.rpires.dao.generic.jdbc.ConnectionFactory;
import br.com.rpires.exceptions.DAOException;
final class Transacao {
    interface Operacao<R> { R executar(Connection con) throws Exception; }
    static <R> R executar(Operacao<R> operacao) throws DAOException {
        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                R resultado = operacao.executar(con);
                con.commit();
                return resultado;
            } catch (Exception ex) {
                try { con.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw new DAOException("Transacao desfeita: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) { throw new DAOException("Erro abrindo/fechando transacao", ex); }
    }
}
