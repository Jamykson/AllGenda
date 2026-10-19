package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.model.Anotacao;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.AuthService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: cada requisição confirma a própria transação, como em produção,
// o que é necessário para o arquivo ser apagado do disco depois do commit da remoção.
@SpringBootTest
class ImagemAnotacaoIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired AulaRepository aulas;
    @Autowired AnotacaoRepository anotacoes;
    @Autowired ImagemAnotacaoRepository imagens;
    @Autowired ArmazenamentoService armazenamento;

    private MockMvc mvc;
    private String tokenAutor;
    private String tokenColega;
    private UUID aulaId;
    private UUID anotacaoId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        var autor = auth.cadastrar(new UsuarioRequestDTO("Autor " + sufixo, "autor-" + sufixo + "@example.com", "senha-forte-123"));
        var colega = auth.cadastrar(new UsuarioRequestDTO("Colega " + sufixo, "colega-" + sufixo + "@example.com", "senha-forte-123"));
        tokenAutor = auth.login(new LoginRequestDTO(autor.email(), "senha-forte-123")).token();
        tokenColega = auth.login(new LoginRequestDTO(colega.email(), "senha-forte-123")).token();

        var disciplina = new Disciplina();
        disciplina.setNome("PDS");
        var participacao = new Participacao();
        participacao.setUsuario(usuarios.findById(autor.id()).orElseThrow());
        participacao.setPapel(PapelParticipacao.DONO);
        participacao.setStatus(StatusParticipacao.ACEITO);
        disciplina.addParticipacao(participacao);
        disciplina = disciplinas.save(disciplina);
        var aula = new Aula();
        aula.setDisciplina(disciplina);
        aula.setData(LocalDate.now());
        aula.setHorario("08:00");
        aula = aulas.save(aula);
        aulaId = aula.getId();
        var anotacao = new Anotacao();
        anotacao.setConteudo("Quadro da aula");
        anotacao.setDataCriacao(LocalDateTime.now());
        anotacao.setAula(aula);
        anotacao.setAutor(usuarios.findById(autor.id()).orElseThrow());
        anotacaoId = anotacoes.save(anotacao).getId();
    }

    private static RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockMultipartHttpServletRequestBuilder envio(String nome, byte[] conteudo) {
        return multipart("/api/anotacoes/{id}/imagens", anotacaoId)
                .file(new MockMultipartFile("arquivo", nome, "image/png", conteudo));
    }

    private String anexarComoAutor() throws Exception {
        String resposta = mvc.perform(comToken(envio("quadro.png", PNG), tokenAutor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    @Test
    void autorAnexaListaBaixaERemoveImagem() throws Exception {
        String resposta = mvc.perform(comToken(envio("quadro.png", PNG), tokenAutor))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/anotacoes/" + anotacaoId + "/imagens/")))
                .andExpect(jsonPath("$.nome").value("quadro.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.tamanho").value(PNG.length))
                .andReturn().getResponse().getContentAsString();
        String imagemId = JsonPath.read(resposta, "$.id");
        String chave = imagens.findById(UUID.fromString(imagemId)).orElseThrow().getChaveArmazenamento();
        assertThat(armazenamento.carregar(chave)).isPresent();

        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId.toString()), tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imagens[0].id").value(imagemId))
                .andExpect(jsonPath("$[0].imagens[0].url").value("/api/anotacoes/" + anotacaoId + "/imagens/" + imagemId));

        mvc.perform(comToken(get("/api/anotacoes/{a}/imagens/{i}", anotacaoId, imagemId), tokenAutor))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")));

        mvc.perform(comToken(delete("/api/anotacoes/{a}/imagens/{i}", anotacaoId, imagemId), tokenAutor))
                .andExpect(status().isNoContent());

        assertThat(imagens.existsById(UUID.fromString(imagemId))).isFalse();
        assertThat(armazenamento.carregar(chave)).isEmpty();
        mvc.perform(comToken(get("/api/anotacoes/{a}/imagens/{i}", anotacaoId, imagemId), tokenAutor))
                .andExpect(status().isNotFound());
    }

    @Test
    void colegaNaoAnexaNemRemoveImagensDoAutor() throws Exception {
        String imagemId = anexarComoAutor();

        mvc.perform(comToken(envio("intrusa.png", PNG), tokenColega))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/anotacoes/{a}/imagens/{i}", anotacaoId, imagemId), tokenColega))
                .andExpect(status().isForbidden());

        assertThat(imagens.existsById(UUID.fromString(imagemId))).isTrue();
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(envio("quadro.png", PNG)).andExpect(status().isUnauthorized());
    }

    @Test
    void arquivoQueNaoEhImagemRecebe400() throws Exception {
        mvc.perform(comToken(envio("falso.png", "não sou imagem".getBytes()), tokenAutor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requisicaoSemOCampoArquivoRecebe400() throws Exception {
        mvc.perform(comToken(multipart("/api/anotacoes/{id}/imagens", anotacaoId)
                        .file(new MockMultipartFile("outro", "quadro.png", "image/png", PNG)), tokenAutor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("arquivo")));
    }

    @Test
    void arquivoAcimaDoLimiteRecebe413() throws Exception {
        byte[] grande = Arrays.copyOf(PNG, 5 * 1024 * 1024 + 1);

        mvc.perform(comToken(envio("grande.png", grande), tokenAutor))
                .andExpect(status().isContentTooLarge());
    }

    @Test
    void imagemInexistenteRecebe404() throws Exception {
        mvc.perform(comToken(get("/api/anotacoes/{a}/imagens/{i}", anotacaoId, UUID.randomUUID()), tokenAutor))
                .andExpect(status().isNotFound());
    }
}
