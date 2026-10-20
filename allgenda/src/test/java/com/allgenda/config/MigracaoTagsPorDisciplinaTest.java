package com.allgenda.config;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.model.Anotacao;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.model.Usuario;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Simula um banco de antes das tags por disciplina: tags sem disciplina, gravadas direto por SQL.
@SpringBootTest
class MigracaoTagsPorDisciplinaTest {

    @Autowired MigracaoTagsPorDisciplina migracao;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired AulaRepository aulas;
    @Autowired AnotacaoRepository anotacoes;

    private Usuario autor() {
        String sufixo = UUID.randomUUID().toString();
        var criado = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Autor " + sufixo, "autor-" + sufixo + "@example.com", "senha-forte-123"));
        return usuarios.findById(criado.id()).orElseThrow();
    }

    private UUID disciplina(String nome) {
        var disciplina = new Disciplina();
        disciplina.setNome(nome);
        return disciplinas.save(disciplina).getId();
    }

    private UUID anotacao(UUID disciplinaId, Usuario autor) {
        var aula = new Aula();
        aula.setDisciplina(disciplinas.findById(disciplinaId).orElseThrow());
        aula.setData(LocalDate.now());
        aula.setHorario("08:00");
        aula = aulas.save(aula);
        var anotacao = new Anotacao();
        anotacao.setConteudo("Texto");
        anotacao.setDataCriacao(LocalDateTime.now());
        anotacao.setAula(aula);
        anotacao.setAutor(autor);
        return anotacoes.save(anotacao).getId();
    }

    private UUID tagAntiga(String nome) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into tags (id, nome) values (?, ?)", id, nome);
        return id;
    }

    private void vincular(UUID anotacaoId, UUID tagId) {
        jdbc.update("insert into anotacao_tag (anotacao_id, tag_id) values (?, ?)", anotacaoId, tagId);
    }

    // disciplina e nome da tag de cada vínculo da anotação
    private List<Map<String, Object>> tagsDa(UUID anotacaoId) {
        return jdbc.queryForList("""
                select t.id, t.nome, t.disciplina_id from anotacao_tag v join tags t on t.id = v.tag_id
                where v.anotacao_id = ?
                """, anotacaoId);
    }

    @Test
    void distribuiCadaTagAntigaPelasDisciplinasDasAnotacoesEApagaAsSemUso() {
        Usuario autor = autor();
        UUID pds = disciplina("PDS");
        UUID grafos = disciplina("Grafos");
        UUID anotacaoPds = anotacao(pds, autor);
        UUID outraPds = anotacao(pds, autor);
        UUID anotacaoGrafos = anotacao(grafos, autor);

        String nome = "prova-" + UUID.randomUUID();
        UUID antiga = tagAntiga(nome);
        vincular(anotacaoPds, antiga);
        vincular(outraPds, antiga);
        vincular(anotacaoGrafos, antiga);
        UUID semUso = tagAntiga("sem-uso-" + UUID.randomUUID());

        migracao.run(null);

        List<Map<String, Object>> tagsPds = tagsDa(anotacaoPds);
        List<Map<String, Object>> tagsGrafos = tagsDa(anotacaoGrafos);
        assertThat(tagsPds).singleElement().satisfies(tag -> {
            assertThat(tag.get("nome")).isEqualTo(nome);
            assertThat(tag.get("disciplina_id")).isEqualTo(pds);
        });
        assertThat(tagsDa(outraPds)).singleElement().satisfies(tag -> assertThat(tag.get("id")).isEqualTo(tagsPds.getFirst().get("id")));
        assertThat(tagsGrafos).singleElement().satisfies(tag -> {
            assertThat(tag.get("nome")).isEqualTo(nome);
            assertThat(tag.get("disciplina_id")).isEqualTo(grafos);
            assertThat(tag.get("id")).isNotEqualTo(tagsPds.getFirst().get("id"));
        });
        assertThat(jdbc.queryForObject("select count(*) from tags where id = ?", Integer.class, semUso)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from tags where disciplina_id is null", Integer.class)).isZero();

        // rodar de novo não muda nada
        Integer totalAntes = jdbc.queryForObject("select count(*) from tags", Integer.class);
        migracao.run(null);
        assertThat(jdbc.queryForObject("select count(*) from tags", Integer.class)).isEqualTo(totalAntes);
        assertThat(tagsDa(anotacaoGrafos)).isEqualTo(tagsGrafos);
    }

    @Test
    void reaproveitaATagQueADisciplinaJaTemComOMesmoNome() {
        Usuario autor = autor();
        UUID pds = disciplina("PDS");
        UUID anotacaoAntiga = anotacao(pds, autor);
        UUID anotacaoNova = anotacao(pds, autor);

        String nome = "Resumo-" + UUID.randomUUID();
        // a disciplina já tem uma tag com o nome (criada depois da mudança) e uma anotação ainda usa a antiga
        UUID daDisciplina = UUID.randomUUID();
        jdbc.update("insert into tags (id, nome, disciplina_id) values (?, ?, ?)", daDisciplina, nome, pds);
        vincular(anotacaoNova, daDisciplina);
        UUID antiga = tagAntiga(nome.toLowerCase());
        vincular(anotacaoAntiga, antiga);

        migracao.run(null);

        assertThat(tagsDa(anotacaoAntiga)).singleElement().satisfies(tag -> assertThat(tag.get("id")).isEqualTo(daDisciplina));
        assertThat(jdbc.queryForObject("select count(*) from tags where id = ?", Integer.class, antiga)).isZero();
    }
}
