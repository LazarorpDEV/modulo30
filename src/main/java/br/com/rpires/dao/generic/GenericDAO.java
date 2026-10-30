package br.com.rpires.dao.generic;

import java.io.Serializable;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import anotacao.*;
import br.com.rpires.dao.Persistente;
import br.com.rpires.dao.generic.jdbc.ConnectionFactory;
import br.com.rpires.exceptions.*;

public abstract class GenericDAO<T extends Persistente, E extends Serializable> implements IGenericDAO<T,E> {
    public abstract Class<T> getTipoClasse();
    // Grafia do contrato original do professor preservada.
    public abstract void atualiarDados(T entity, T entityCadastrado);
    protected abstract String getQueryInsercao();
    protected abstract String getQueryExclusao();
    protected abstract String getQueryAtualizacao();
    protected abstract void setParametrosQueryInsercao(PreparedStatement stm, T entity) throws SQLException;
    protected abstract void setParametrosQueryExclusao(PreparedStatement stm, E valor) throws SQLException;
    protected abstract void setParametrosQueryAtualizacao(PreparedStatement stm, T entity) throws SQLException;
    protected abstract void setParametrosQuerySelect(PreparedStatement stm, E valor) throws SQLException;

    @SuppressWarnings("unchecked")
    public E getChave(T entity) throws TipoChaveNaoEncontradaException {
        for (Field field : getTipoClasse().getDeclaredFields()) {
            if (field.isAnnotationPresent(TipoChave.class)) {
                try {
                    return (E) getTipoClasse().getMethod(field.getAnnotation(TipoChave.class).value()).invoke(entity);
                } catch (ReflectiveOperationException ex) {
                    throw new TipoChaveNaoEncontradaException("Chave nao encontrada", ex);
                }
            }
        }
        throw new TipoChaveNaoEncontradaException("Anotacao TipoChave nao encontrada");
    }

    // Reutilizavel dentro de uma transacao maior (produto + estoque).
    protected boolean inserir(Connection con, T entity) throws SQLException {
        try (PreparedStatement stm = con.prepareStatement(getQueryInsercao(), new String[]{"id"})) {
            setParametrosQueryInsercao(stm, entity);
            if (stm.executeUpdate() != 1) return false;
            try (ResultSet rs = stm.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("ID nao retornado pelo banco");
                entity.setId(rs.getLong(1));
            }
            return true;
        }
    }

    @Override public Boolean cadastrar(T entity) throws TipoChaveNaoEncontradaException, DAOException {
        try (Connection con = getConnection()) { return inserir(con, entity); }
        catch (SQLException ex) { throw new DAOException("Erro cadastrando objeto", ex); }
    }
    @Override public void excluir(E valor) throws DAOException {
        try (Connection con = getConnection(); PreparedStatement stm = con.prepareStatement(getQueryExclusao())) {
            setParametrosQueryExclusao(stm, valor); stm.executeUpdate();
        } catch (SQLException ex) { throw new DAOException("Erro excluindo objeto", ex); }
    }
    @Override public void alterar(T entity) throws TipoChaveNaoEncontradaException, DAOException {
        try (Connection con = getConnection(); PreparedStatement stm = con.prepareStatement(getQueryAtualizacao())) {
            setParametrosQueryAtualizacao(stm, entity);
            if (stm.executeUpdate() != 1) throw new SQLException("Registro nao encontrado para alteracao");
        } catch (SQLException ex) { throw new DAOException("Erro alterando objeto", ex); }
    }
    @Override public T consultar(E valor) throws MaisDeUmRegistroException, TableException, DAOException {
        try (Connection con = getConnection(); PreparedStatement stm = con.prepareStatement(
                "SELECT * FROM " + getTableName() + " WHERE " + getNomeCampoChave(getTipoClasse()) + " = ?")) {
            setParametrosQuerySelect(stm, valor);
            try (ResultSet rs = stm.executeQuery()) {
                if (!rs.next()) return null;
                T entity = converter(rs);
                if (rs.next()) throw new MaisDeUmRegistroException("Mais de um registro encontrado");
                return entity;
            }
        } catch (SQLException | TipoChaveNaoEncontradaException ex) {
            throw new DAOException("Erro consultando objeto", ex);
        }
    }
    @Override public Collection<T> buscarTodos() throws DAOException {
        List<T> itens = new ArrayList<>();
        try (Connection con = getConnection(); PreparedStatement stm = con.prepareStatement(
                "SELECT * FROM " + getTableName() + " ORDER BY id"); ResultSet rs = stm.executeQuery()) {
            while (rs.next()) itens.add(converter(rs));
            return itens;
        } catch (SQLException | TableException ex) { throw new DAOException("Erro listando objetos", ex); }
    }

    public String getNomeCampoChave(Class<?> tipo) throws TipoChaveNaoEncontradaException {
        for (Field field : tipo.getDeclaredFields())
            if (field.isAnnotationPresent(TipoChave.class) && field.isAnnotationPresent(ColunaTabela.class))
                return field.getAnnotation(ColunaTabela.class).dbName();
        throw new TipoChaveNaoEncontradaException("Campo chave nao encontrado");
    }
    private String getTableName() throws TableException {
        Tabela tabela = getTipoClasse().getAnnotation(Tabela.class);
        if (tabela == null) throw new TableException("Tabela nao mapeada");
        return tabela.value();
    }
    private T converter(ResultSet rs) throws DAOException, SQLException {
        try {
            T entity = getTipoClasse().getDeclaredConstructor().newInstance();
            for (Field field : getTipoClasse().getDeclaredFields()) {
                ColunaTabela coluna = field.getAnnotation(ColunaTabela.class);
                if (coluna == null) continue;
                Class<?> tipo = field.getType();
                Object valor;
                if (tipo == String.class) valor = rs.getString(coluna.dbName());
                else if (tipo == BigDecimal.class) valor = rs.getBigDecimal(coluna.dbName());
                else if (tipo == Long.class) { long v = rs.getLong(coluna.dbName()); valor = rs.wasNull() ? null : v; }
                else if (tipo == Integer.class) { int v = rs.getInt(coluna.dbName()); valor = rs.wasNull() ? null : v; }
                else if (tipo == Double.class) { double v = rs.getDouble(coluna.dbName()); valor = rs.wasNull() ? null : v; }
                else if (tipo == Short.class) { short v = rs.getShort(coluna.dbName()); valor = rs.wasNull() ? null : v; }
                else throw new IllegalArgumentException("Tipo nao suportado: " + tipo);
                getTipoClasse().getMethod(coluna.setJavaName(), tipo).invoke(entity, valor);
            }
            return entity;
        } catch (ReflectiveOperationException | IllegalArgumentException ex) {
            throw new DAOException("Erro convertendo registro por reflection", ex);
        }
    }
    protected Connection getConnection() throws DAOException {
        try { return ConnectionFactory.getConnection(); }
        catch (SQLException ex) { throw new DAOException("Erro conectando ao banco", ex); }
    }
}
