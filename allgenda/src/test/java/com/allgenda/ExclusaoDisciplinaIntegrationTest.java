package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.AuthService;
import com.allgenda.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: cada requisição confirma a própria transação, como em produção,
// o que é necessário para os arquivos serem apagados do disco depois do commit da exclusão.
@SpringBootTest
class ExclusaoDisciplinaIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired AulaRepository aulas;
    @Autowired ImagemAnotacaoRepository imagens;
    @Autowired ImagemAulaRepository imagensAvulsas;
    @Autowired ArmazenamentoService armazenamento;

    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        var usuario = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Aluno " + sufixo, "aluno-" + sufixo + "@example.com", "senha-forte-123"));
        token = auth.login(new LoginRequestDTO(usuario.email(), "senha-forte-123")).token();
    }

    private RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criar(String caminho, String json) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private String criarDisciplina() throws Exception {
        return criar("/api/disciplinas", "{\"nome\":\"PDS\"}");
    }

    private String criarAula(String disciplinaId) throws Exception {
        return criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-18\",\"horario\":\"08:00\",\"topico\":\"Padrões de projeto\"}");
    }

    // cria uma anotação na aula com a quantidade pedida de imagens e devolve as chaves dos arquivos
    private List<String> criarAnotacaoComImagens(String aulaId, int quantidade) throws Exception {
        String anotacaoId = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Quadro\",\"tags\":[\"java\"]}");
        List<String> chaves = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            String resposta = mvc.perform(comToken(multipart("/api/anotacoes/{id}/imagens", anotacaoId)
                            .file(new MockMultipartFile("arquivo", "quadro.png", "image/png", PNG))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            String imagemId = JsonPath.read(resposta, "$.id");
            chaves.add(imagens.findById(UUID.fromString(imagemId)).orElseThrow().getChaveArmazenamento());
        }
        return chaves;
    }

    // envia uma imagem avulsa para a aula e devolve a chave do arquivo
    private String criarImagemAvulsa(String aulaId) throws Exception {
        String resposta = mvc.perform(comToken(multipart("/api/aulas/{id}/imagens", aulaId)
                        .file(new MockMultipartFile("arquivo", "slide.png", "image/png", PNG))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String imagemId = JsonPath.read(resposta, "$.id");
        return imagensAvulsas.findById(UUID.fromString(imagemId)).orElseThrow().getChaveArmazenamento();
    }

    @Test
    void excluirDisciplinaApagaOsArquivosDasImagensDeTodasAsAulasDela() throws Exception {
        String disciplinaId = criarDisciplina();
        List<String> chavesDaDisciplina = new ArrayList<>();
        chavesDaDisciplina.addAll(criarAnotacaoComImagens(criarAula(disciplinaId), 2));
        String segundaAulaId = criarAula(disciplinaId);
        chavesDaDisciplina.addAll(criarAnotacaoComImagens(segundaAulaId, 1));
        String chaveAvulsa = criarImagemAvulsa(segundaAulaId);

        String outraDisciplinaId = criarDisciplina();
        String aulaDaOutraId = criarAula(outraDisciplinaId);
        List<String> chavesDaOutra = criarAnotacaoComImagens(aulaDaOutraId, 1);
        String chaveAvulsaDaOutra = criarImagemAvulsa(aulaDaOutraId);

        assertThat(imagens.findChavesArmazenamentoByDisciplinaId(UUID.fromString(disciplinaId)))
                .containsExactlyInAnyOrderElementsOf(chavesDaDisciplina);
        assertThat(imagensAvulsas.findChavesArmazenamentoByDisciplinaId(UUID.fromString(disciplinaId)))
                .containsExactly(chaveAvulsa);
        chavesDaDisciplina.add(chaveAvulsa);
        assertThat(chavesDaDisciplina).hasSize(4).allSatisfy(chave -> assertThat(armazenamento.carregar(chave)).isPresent());

        mvc.perform(comToken(delete("/api/disciplinas/{id}", disciplinaId))).andExpect(status().isNoContent());

        assertThat(chavesDaDisciplina).allSatisfy(chave -> assertThat(armazenamento.carregar(chave)).isEmpty());
        assertThat(imagens.findChavesArmazenamentoByDisciplinaId(UUID.fromString(disciplinaId))).isEmpty();
        assertThat(imagensAvulsas.findChavesArmazenamentoByDisciplinaId(UUID.fromString(disciplinaId))).isEmpty();
        assertThat(aulas.findAll()).noneSatisfy(aula -> assertThat(aula.getDisciplina().getId().toString()).isEqualTo(disciplinaId));

        // a outra disciplina não é afetada
        assertThat(chavesDaOutra).singleElement().satisfies(chave -> assertThat(armazenamento.carregar(chave)).isPresent());
        assertThat(armazenamento.carregar(chaveAvulsaDaOutra)).isPresent();
        assertThat(imagens.findChavesArmazenamentoByDisciplinaId(UUID.fromString(outraDisciplinaId)))
                .containsExactlyElementsOf(chavesDaOutra);
        assertThat(imagensAvulsas.findChavesArmazenamentoByDisciplinaId(UUID.fromString(outraDisciplinaId)))
                .containsExactly(chaveAvulsaDaOutra);
    }

    @Test
    void excluirDisciplinaSemImagensContinuaFuncionando() throws Exception {
        String disciplinaId = criarDisciplina();
        criarAnotacaoComImagens(criarAula(disciplinaId), 0);

        mvc.perform(comToken(delete("/api/disciplinas/{id}", disciplinaId))).andExpect(status().isNoContent());
        mvc.perform(comToken(delete("/api/disciplinas/{id}", disciplinaId))).andExpect(status().isNotFound());
    }
}
