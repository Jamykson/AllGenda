package com.allgenda.service.impl;

import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.ArquivoMuitoGrandeException;
import com.allgenda.exception.ImagemNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.AnotacaoMapper;
import com.allgenda.model.Anotacao;
import com.allgenda.model.ImagemAnotacao;
import com.allgenda.model.Usuario;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.ImagemAnotacaoService.ImagemArquivo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagemAnotacaoServiceImplTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @Mock
    private AnotacaoRepository anotacaoRepository;
    @Mock
    private ImagemAnotacaoRepository imagemRepository;
    @Mock
    private ArmazenamentoService armazenamento;

    private ImagemAnotacaoServiceImpl service;

    private final UUID anotacaoId = UUID.randomUUID();
    private final UUID autorId = UUID.randomUUID();
    private Anotacao anotacao;

    @BeforeEach
    void setUp() {
        service = criarService(DataSize.ofMegabytes(5), 10);

        Usuario autor = new Usuario();
        autor.setNome("Maria");
        ReflectionTestUtils.setField(autor, "id", autorId);
        anotacao = new Anotacao();
        anotacao.setConteudo("Strategy e Observer");
        anotacao.setAutor(autor);
        ReflectionTestUtils.setField(anotacao, "id", anotacaoId);

        autenticar(autorId);
    }

    @AfterEach
    void limparContexto() { SecurityContextHolder.clearContext(); }

    private ImagemAnotacaoServiceImpl criarService(DataSize tamanhoMaximo, int maximoPorAnotacao) {
        return new ImagemAnotacaoServiceImpl(anotacaoRepository, imagemRepository, armazenamento,
                new AnotacaoMapper(), tamanhoMaximo, maximoPorAnotacao);
    }

    private static void autenticar(UUID usuarioId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId.toString(), null, List.of()));
    }

    private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo);
    }

    private ImagemAnotacao imagemSalva(String chave) {
        ImagemAnotacao imagem = new ImagemAnotacao();
        imagem.setNomeOriginal("quadro.png");
        imagem.setContentType("image/png");
        imagem.setChaveArmazenamento(chave);
        anotacao.addImagem(imagem);
        return imagem;
    }

    private void anotacaoExiste() {
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
    }

    @Test
    void anexaPngPeloTipoDetectadoEIgnoraOCaminhoDoNome() {
        anotacaoExiste();
        when(armazenamento.salvar(PNG, "png")).thenReturn("chave.png");
        when(imagemRepository.save(any(ImagemAnotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        ImagemAnotacaoResponseDTO resposta = service.anexar(anotacaoId, arquivo("../../fotos/quadro.png", PNG));

        assertThat(resposta.nome()).isEqualTo("quadro.png");
        assertThat(resposta.contentType()).isEqualTo("image/png");
        assertThat(resposta.tamanho()).isEqualTo(PNG.length);
        assertThat(resposta.url()).startsWith("/api/anotacoes/" + anotacaoId + "/imagens/");
        assertThat(anotacao.getImagens()).singleElement()
                .satisfies(imagem -> assertThat(imagem.getChaveArmazenamento()).isEqualTo("chave.png"));
    }

    @Test
    void anexaJpegEWebp() {
        anotacaoExiste();
        when(armazenamento.salvar(any(), anyString())).thenAnswer(inv -> "chave." + inv.getArgument(1));
        when(imagemRepository.save(any(ImagemAnotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.anexar(anotacaoId, arquivo("foto.jpeg", JPEG)).contentType()).isEqualTo("image/jpeg");
        assertThat(service.anexar(anotacaoId, arquivo("foto.webp", WEBP)).contentType()).isEqualTo("image/webp");
        verify(armazenamento).salvar(JPEG, "jpg");
        verify(armazenamento).salvar(WEBP, "webp");
    }

    @Test
    void textoComExtensaoPngLancaRequisicaoInvalida() {
        anotacaoExiste();

        assertThatThrownBy(() -> service.anexar(anotacaoId, arquivo("falso.png", "não sou imagem".getBytes())))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(armazenamento, never()).salvar(any(), anyString());
    }

    @Test
    void arquivoVazioLancaRequisicaoInvalida() {
        anotacaoExiste();

        assertThatThrownBy(() -> service.anexar(anotacaoId, arquivo("vazio.png", new byte[0])))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void arquivoAcimaDoLimiteLancaArquivoMuitoGrande() {
        service = criarService(DataSize.ofBytes(PNG.length - 1), 10);
        anotacaoExiste();

        assertThatThrownBy(() -> service.anexar(anotacaoId, arquivo("grande.png", PNG)))
                .isInstanceOf(ArquivoMuitoGrandeException.class);
        verify(armazenamento, never()).salvar(any(), anyString());
    }

    @Test
    void anotacaoNoLimiteDeImagensLancaRequisicaoInvalida() {
        service = criarService(DataSize.ofMegabytes(5), 1);
        imagemSalva("existente.png");
        anotacaoExiste();

        assertThatThrownBy(() -> service.anexar(anotacaoId, arquivo("outra.png", PNG)))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(armazenamento, never()).salvar(any(), anyString());
    }

    @Test
    void quemNaoEhAutorNaoAnexa() {
        autenticar(UUID.randomUUID());
        anotacaoExiste();

        assertThatThrownBy(() -> service.anexar(anotacaoId, arquivo("quadro.png", PNG)))
                .isInstanceOf(AcessoNegadoException.class);
        verify(armazenamento, never()).salvar(any(), anyString());
    }

    @Test
    void anexarEmAnotacaoInexistenteLancaErroTratado() {
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anexar(anotacaoId, arquivo("quadro.png", PNG)))
                .isInstanceOf(AnotacaoNaoEncontradaException.class);
    }

    @Test
    void carregaConteudoETipoDaImagem() {
        UUID imagemId = UUID.randomUUID();
        ImagemAnotacao imagem = imagemSalva("chave.png");
        when(imagemRepository.findByIdAndAnotacaoId(imagemId, anotacaoId)).thenReturn(Optional.of(imagem));
        when(armazenamento.carregar("chave.png")).thenReturn(Optional.of(new ByteArrayResource(PNG)));

        ImagemArquivo arquivo = service.carregar(anotacaoId, imagemId);

        assertThat(arquivo.contentType()).isEqualTo("image/png");
        assertThat(arquivo.nome()).isEqualTo("quadro.png");
    }

    @Test
    void imagemDeOutraAnotacaoNaoEhEncontrada() {
        UUID imagemId = UUID.randomUUID();
        when(imagemRepository.findByIdAndAnotacaoId(imagemId, anotacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.carregar(anotacaoId, imagemId))
                .isInstanceOf(ImagemNaoEncontradaException.class);
    }

    @Test
    void arquivoAusenteNoArmazenamentoLancaNaoEncontrada() {
        UUID imagemId = UUID.randomUUID();
        when(imagemRepository.findByIdAndAnotacaoId(imagemId, anotacaoId)).thenReturn(Optional.of(imagemSalva("sumiu.png")));
        when(armazenamento.carregar("sumiu.png")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.carregar(anotacaoId, imagemId))
                .isInstanceOf(ImagemNaoEncontradaException.class);
    }

    @Test
    void autorRemoveImagemEArquivo() {
        UUID imagemId = UUID.randomUUID();
        ImagemAnotacao imagem = imagemSalva("chave.png");
        anotacaoExiste();
        when(imagemRepository.findByIdAndAnotacaoId(imagemId, anotacaoId)).thenReturn(Optional.of(imagem));

        service.remover(anotacaoId, imagemId);

        assertThat(anotacao.getImagens()).isEmpty();
        verify(armazenamento).remover("chave.png");
    }

    @Test
    void quemNaoEhAutorNaoRemoveNemDescobreAImagem() {
        autenticar(UUID.randomUUID());
        imagemSalva("chave.png");
        anotacaoExiste();

        assertThatThrownBy(() -> service.remover(anotacaoId, UUID.randomUUID()))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(anotacao.getImagens()).hasSize(1);
        verify(imagemRepository, never()).findByIdAndAnotacaoId(any(), any());
        verify(armazenamento, never()).remover(anyString());
    }

    @Test
    void nomeSeguroTiraCaminhoEControlesELimitaTamanho() {
        assertThat(ImagemAnotacaoServiceImpl.nomeSeguro(null)).isEqualTo("imagem");
        assertThat(ImagemAnotacaoServiceImpl.nomeSeguro("C:\\fotos\\quadro.png")).isEqualTo("quadro.png");
        assertThat(ImagemAnotacaoServiceImpl.nomeSeguro("pasta/")).isEqualTo("imagem");
        assertThat(ImagemAnotacaoServiceImpl.nomeSeguro("a\r\nb.png")).isEqualTo("ab.png");

        char[] longo = new char[300];
        Arrays.fill(longo, 'x');
        assertThat(ImagemAnotacaoServiceImpl.nomeSeguro(new String(longo))).hasSize(255);
    }
}
