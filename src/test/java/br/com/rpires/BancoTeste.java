package br.com.rpires;
import java.sql.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import br.com.rpires.dao.generic.jdbc.ConnectionFactory;
public abstract class BancoTeste {
    @Before public void prepararBanco() throws Exception {
        if (!"postgres".equals(System.getProperty("test.database"))) {
            System.setProperty("db.url", "jdbc:h2:mem:mod30;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
            System.setProperty("db.user", "sa"); System.setProperty("db.password", "");
        } else {
            String url = System.getProperty("db.url", System.getenv("DB_URL"));
            if (url == null || !url.matches("jdbc:postgresql://[^/]+/modulo30_testes(?:\\?.*)?"))
                throw new IllegalStateException("Configure DB_URL para um banco exclusivo chamado modulo30_testes");
        }
        try (Connection con = ConnectionFactory.getConnection(); Statement stm = con.createStatement();
                InputStream in = BancoTeste.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql ausente");
            for (String comando : new String(in.readAllBytes(), StandardCharsets.UTF_8).split(";"))
                if (!comando.trim().isEmpty()) stm.execute(comando);
            for (String tabela : new String[]{"tb_produto_quantidade","tb_venda","tb_estoque","tb_produto","tb_cliente"})
                stm.executeUpdate("DELETE FROM " + tabela);
        }
    }
}
