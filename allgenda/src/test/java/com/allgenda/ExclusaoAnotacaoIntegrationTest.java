package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.TagRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: cada requisição confirma a própria transação, como em produção,
// o que é necessário para os arquivos serem apagados do disco depois do commit da exclusão.
@SpringBootTest
class ExclusaoAnotacaoIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired AnotacaoRepository anotacoes;
    @Autowired ImagemAnotacaoRepository imagens;
    @Autowired TagRepository tags;
    @Autowired ArmazenamentoService armazenamento;

    private MockMvc mvc;
    private String tokenAutor;
    private String tokenColega;
    private String tag;
    private String aulaId;
    private String anotacaoId;
    private String imagemId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        tag = "tag-" + sufixo;
        var autor = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Autor " + sufixo, "autor-" + sufixo + "@example.com", "senha-forte-123"));
        var colega = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Colega " + sufixo, "colega-" + sufixo + "@example.com", "senha-forte-123"));
        tokenAutor = auth.login(new LoginRequestDTO(autor.email(), "senha-forte-123")).token();
        tokenColega = auth.login(new LoginRequestDTO(colega.email(), "senha-forte-123")).token();

        String disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\"}");
        aulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId + "\",\"data\":\"2026-10-17\",\"horario\":\"08:00\",\"topico\":\"Padrões de projeto\"}");
        anotacaoId = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Quadro\",\"tags\":[\"" + tag + "\"]}");

        String resposta = mvc.perform(comToken(multipart("/api/anotacoes/{id}/imagens", anotacaoId)
                        .file(new MockMultipartFile("arquivo", "quadro.png", "image/png", PNG)), tokenAutor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        imagemId = JsonPath.read(resposta, "$.id");
    }

    private static RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criar(String caminho, String json) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json), tokenAutor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private RequestBuilder exclusao(String token) {
        return comToken(delete("/api/anotacoes/{id}", anotacaoId), token);
    }

    @Test
    void autorExcluiAnotacaoComImagensETagsSemApagarAsTags() throws Exception {
        String chave = imagens.findById(UUID.fromString(imagemId)).orElseThrow().getChaveArmazenamento();
        assertThat(armazenamento.carregar(chave)).isPresent();

        mvc.perform(exclusao(tokenAutor)).andExpect(status().isNoContent());

        assertThat(anotacoes.existsById(UUID.fromString(anotacaoId))).isFalse();
        assertThat(imagens.existsById(UUID.fromString(imagemId))).isFalse();
        assertThat(armazenamento.carregar(chave)).isEmpty();
        assertThat(tags.findByNomeIgnoreCase(tag)).isPresent();
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void excluirDeNovoRecebe404() throws Exception {
        mvc.perform(exclusao(tokenAutor)).andExpect(status().isNoContent());

        mvc.perform(exclusao(tokenAutor)).andExpect(status().isNotFound());
    }

    @Test
    void colegaNaoExcluiAnotacaoDoAutor() throws Exception {
        mvc.perform(exclusao(tokenColega)).andExpect(status().isForbidden());

        assertThat(anotacoes.existsById(UUID.fromString(anotacaoId))).isTrue();
        assertThat(imagens.existsById(UUID.fromString(imagemId))).isTrue();
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(delete("/api/anotacoes/{id}", anotacaoId)).andExpect(status().isUnauthorized());

        assertThat(anotacoes.existsById(UUID.fromString(anotacaoId))).isTrue();
    }
}
