package com.allgenda.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

// As tags eram globais e passaram a ser de cada disciplina. Bancos criados antes disso têm tags sem
// disciplina e uma restrição única em tags(nome), que o ddl-auto=update não remove. Ao iniciar, esta
// migração tira a restrição antiga e distribui cada tag antiga pelas disciplinas das anotações que a
// usam: uma tag usada em duas disciplinas vira duas tags. Tags antigas sem uso são apagadas.
// Só mexe em tags sem disciplina, então rodar de novo não muda nada.
@Component
public class MigracaoTagsPorDisciplina implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoTagsPorDisciplina.class);

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;

    public MigracaoTagsPorDisciplina(JdbcTemplate jdbc, TransactionTemplate transacao) {
        this.jdbc = jdbc;
        this.transacao = transacao;
    }

    private record TagAntiga(UUID id, String nome) {}

    @Override
    public void run(ApplicationArguments args) {
        transacao.executeWithoutResult(status -> migrar());
    }

    void migrar() {
        removerRestricaoAntiga();

        List<TagAntiga> antigas = jdbc.query("select id, nome from tags where disciplina_id is null",
                (linha, indice) -> new TagAntiga(linha.getObject("id", UUID.class), linha.getString("nome")));
        if (antigas.isEmpty()) {
            return;
        }

        int apagadas = 0;
        for (TagAntiga antiga : antigas) {
            if (!distribuir(antiga)) {
                apagadas++;
            }
        }
        log.info("Tags migradas para as disciplinas: {} ({} sem uso foram apagadas).", antigas.size(), apagadas);
    }

    // devolve se a tag antiga continuou existindo (reaproveitada pela primeira disciplina)
    private boolean distribuir(TagAntiga antiga) {
        List<UUID> disciplinas = jdbc.queryForList("""
                select distinct a.disciplina_id
                from anotacao_tag v
                join anotacoes n on n.id = v.anotacao_id
                join aulas a on a.id = n.aula_id
                where v.tag_id = ?
                """, UUID.class, antiga.id());

        boolean reaproveitada = false;
        for (UUID disciplinaId : disciplinas) {
            UUID existente = jdbc.query("select id from tags where disciplina_id = ? and lower(nome) = lower(?)",
                    (linha, indice) -> linha.getObject("id", UUID.class), disciplinaId, antiga.nome())
                    .stream().findFirst().orElse(null);

            UUID destino;
            if (existente != null) {
                destino = existente;
            }
            else if (!reaproveitada) {
                jdbc.update("update tags set disciplina_id = ? where id = ?", disciplinaId, antiga.id());
                reaproveitada = true;
                continue; // os vínculos já apontam para esta linha
            }
            else {
                destino = UUID.randomUUID();
                jdbc.update("insert into tags (id, nome, disciplina_id) values (?, ?, ?)", destino, antiga.nome(), disciplinaId);
            }

            jdbc.update("""
                    update anotacao_tag set tag_id = ?
                    where tag_id = ? and anotacao_id in (
                        select n.id from anotacoes n join aulas a on a.id = n.aula_id where a.disciplina_id = ?
                    )
                    """, destino, antiga.id(), disciplinaId);
        }

        if (!reaproveitada) {
            jdbc.update("delete from tags where id = ?", antiga.id());
        }
        return reaproveitada;
    }

    // a restrição única antiga em tags(nome) impediria o mesmo nome em duas disciplinas
    private void removerRestricaoAntiga() {
        String banco = jdbc.execute((ConnectionCallback<String>) conexao -> conexao.getMetaData().getDatabaseProductName());
        if (!"PostgreSQL".equalsIgnoreCase(banco)) {
            return; // os testes usam H2 com o esquema criado do zero, já sem a restrição
        }
        List<String> restricoes = jdbc.queryForList("""
                select c.conname
                from pg_constraint c join pg_class t on t.oid = c.conrelid
                where t.relname = 'tags' and c.contype = 'u' and pg_get_constraintdef(c.oid) = 'UNIQUE (nome)'
                """, String.class);
        for (String restricao : restricoes) {
            jdbc.execute("alter table tags drop constraint \"" + restricao + "\"");
            log.info("Restrição única antiga removida de tags(nome): {}", restricao);
        }
    }
}
