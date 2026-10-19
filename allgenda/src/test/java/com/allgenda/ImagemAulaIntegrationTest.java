package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.dto.response.UsuarioResponseDTO;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.UsuarioRepository;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: cada requisição confirma a própria transação, como em produção,
// o que é necessário para o arquivo ser apagado do disco depois do commit da remoção.
@SpringBootTest
class ImagemAulaIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired AulaRepository aulas;
    @Autowired ImagemAulaRepository imagens;
    @Autowired ArmazenamentoService armazenamento;

    private MockMvc mvc;
    private String tokenAutor;
    private String tokenColega;
    private String tokenDeFora;
    private UUID autorId;
    private UUID aulaId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        var autor = cadastrar("Autor", sufixo);
        var colega = cadastrar("Colega", sufixo);
        var deFora = cadastrar("Visitante", sufixo);
        autorId = autor.id();
        tokenAutor = auth.login(new LoginRequestDTO(autor.email(), "senha-forte-123")).token();
        tokenColega = auth.login(new LoginRequestDTO(colega.email(), "senha-forte-123")).token();
        tokenDeFora = auth.login(new LoginRequestDTO(deFora.email(), "senha-forte-123")).token();

        var disciplina = new Disciplina();
        disciplina.setNome("PDS");
        disciplina = disciplinas.save(disciplina);
        participar(autor, disciplina, PapelParticipacao.DONO);
        participar(colega, disciplina, PapelParticipacao.MEMBRO);

        var aula = new Aula();
        aula.setDisciplina(disciplina);
        aula.setData(LocalDate.now());
        aula.setHorario("08:00");
        aulaId = aulas.save(aula).getId();
    }

    private UsuarioResponseDTO cadastrar(String nome, String sufixo) {
        return cadastroUsuarios.cadastrar(new UsuarioRequestDTO(nome + " " + sufixo,
                nome.toLowerCase() + "-" + sufixo + "@example.com", "senha-forte-123"));
    }

    private void participar(UsuarioResponseDTO usuario, Disciplina disciplina, PapelParticipacao papel) {
        var participacao = new Participacao();
        participacao.setUsuario(usuarios.findById(usuario.id()).orElseThrow());
        participacao.setDisciplina(disciplina);
        participacao.setPapel(papel);
        participacao.setStatus(StatusParticipacao.ACEITO);
        participacoes.save(participacao);
    }

    private static RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private MockMultipartHttpServletRequestBuilder envio(byte[] conteudo) {
        return multipart("/api/aulas/{id}/imagens", aulaId)
                .file(new MockMultipartFile("arquivo", "slide.png", "image/png", conteudo));
    }

    private RequestBuilder edicaoDaLegenda(String imagemId, String json, String token) {
        return comToken(patch("/api/aulas/{a}/imagens/{i}", aulaId, imagemId)
                .contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    private String enviarComoAutor() throws Exception {
        String resposta = mvc.perform(comToken(envio(PNG), tokenAutor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    @Test
    void autorEnviaListaBaixaEditaLegendaERemoveImagem() throws Exception {
        String resposta = mvc.perform(comToken(envio(PNG).param("legenda", "  Diagrama de classes  "), tokenAutor))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/aulas/" + aulaId + "/imagens/")))
                .andExpect(jsonPath("$.legenda").value("Diagrama de classes"))
                .andExpect(jsonPath("$.nome").value("slide.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.tamanho").value(PNG.length))
                .andExpect(jsonPath("$.autorId").value(autorId.toString()))
                .andExpect(jsonPath("$.aulaId").value(aulaId.toString()))
                .andReturn().getResponse().getContentAsString();
        String imagemId = JsonPath.read(resposta, "$.id");
        String chave = imagens.findById(UUID.fromString(imagemId)).orElseThrow().getChaveArmazenamento();
        assertThat(armazenamento.carregar(chave)).isPresent();

        mvc.perform(comToken(get("/api/aulas/{id}/imagens", aulaId), tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(imagemId))
                .andExpect(jsonPath("$[0].url").value("/api/aulas/" + aulaId + "/imagens/" + imagemId));

        mvc.perform(comToken(get("/api/aulas/{a}/imagens/{i}", aulaId, imagemId), tokenAutor))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")));

        mvc.perform(edicaoDaLegenda(imagemId, "{\"legenda\":\"Diagrama revisado\"}", tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legenda").value("Diagrama revisado"));
        mvc.perform(edicaoDaLegenda(imagemId, "{\"legenda\":\"\"}", tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legenda").value(nullValue()));

        mvc.perform(comToken(delete("/api/aulas/{a}/imagens/{i}", aulaId, imagemId), tokenAutor))
                .andExpect(status().isNoContent());

        assertThat(imagens.existsById(UUID.fromString(imagemId))).isFalse();
        assertThat(armazenamento.carregar(chave)).isEmpty();
        mvc.perform(comToken(get("/api/aulas/{a}/imagens/{i}", aulaId, imagemId), tokenAutor))
                .andExpect(status().isNotFound());
    }

    @Test
    void imagemSemLegendaFicaComLegendaNula() throws Exception {
        mvc.perform(comToken(envio(PNG), tokenAutor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legenda").value(nullValue()));
    }

    @Test
    void colegaParticipanteVeEBaixaMasNaoAlteraImagemDoAutor() throws Exception {
        String imagemId = enviarComoAutor();

        mvc.perform(comToken(get("/api/aulas/{id}/imagens", aulaId), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(imagemId));
        mvc.perform(comToken(get("/api/aulas/{a}/imagens/{i}", aulaId, imagemId), tokenColega))
                .andExpect(status().isOk());

        mvc.perform(edicaoDaLegenda(imagemId, "{\"legenda\":\"Do colega\"}", tokenColega))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Somente o autor da imagem pode alterá-la."));
        mvc.perform(comToken(delete("/api/aulas/{a}/imagens/{i}", aulaId, imagemId), tokenColega))
                .andExpect(status().isForbidden());

        assertThat(imagens.existsById(UUID.fromString(imagemId))).isTrue();
    }

    @Test
    void quemNaoParticipaNaoVeNemEnviaImagens() throws Exception {
        String imagemId = enviarComoAutor();

        mvc.perform(comToken(get("/api/aulas/{id}/imagens", aulaId), tokenDeFora))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/aulas/{a}/imagens/{i}", aulaId, imagemId), tokenDeFora))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(envio(PNG), tokenDeFora))
                .andExpect(status().isForbidden());

        assertThat(imagens.findByAulaIdOrderByDataCriacaoAsc(aulaId)).hasSize(1);
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(envio(PNG)).andExpect(status().isUnauthorized());
    }

    @Test
    void arquivoQueNaoEhImagemRecebe400() throws Exception {
        mvc.perform(comToken(envio("não sou imagem".getBytes()), tokenAutor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void legendaLongaDemaisRecebe400() throws Exception {
        mvc.perform(comToken(envio(PNG).param("legenda", "a".repeat(301)), tokenAutor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A legenda deve ter no máximo 300 caracteres."));

        assertThat(imagens.findByAulaIdOrderByDataCriacaoAsc(aulaId)).isEmpty();
    }

    @Test
    void aulaInexistenteRecebe404() throws Exception {
        mvc.perform(comToken(get("/api/aulas/{id}/imagens", UUID.randomUUID()), tokenAutor))
                .andExpect(status().isNotFound());
    }
}
