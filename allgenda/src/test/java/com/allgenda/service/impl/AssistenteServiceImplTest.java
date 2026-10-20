package com.allgenda.service.impl;

import com.allgenda.dto.request.PerguntaAssistenteDTO;
import com.allgenda.dto.request.PerguntaAssistenteDTO.Foco;
import com.allgenda.dto.request.PerguntaAssistenteDTO.Mensagem;
import com.allgenda.dto.request.PerguntaAssistenteDTO.Papel;
import com.allgenda.dto.request.PerguntaAssistenteDTO.TipoFoco;
import com.allgenda.exception.AssistenteIndisponivelException;
import com.allgenda.exception.LimiteDePerguntasException;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.model.ImagemAula;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.Usuario;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.LimitePerguntasAssistente;
import com.allgenda.service.ModeloDeLinguagem;
import com.allgenda.service.PedidoAoModelo;
import com.allgenda.service.ReducaoDeImagem;
import com.allgenda.service.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistenteServiceImplTest {

    @Mock private AulaRepository aulaRepository;
    @Mock private AnotacaoRepository anotacaoRepository;
    @Mock private ImagemAulaRepository imagemAulaRepository;
    @Mock private ParticipacaoRepository participacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ArmazenamentoService armazenamento;
    @Mock private ModeloDeLinguagem modelo;

    private final UUID aulaId = UUID.randomUUID();
    private final UUID disciplinaId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private Aula aula;
    private Usuario autor;

    @BeforeEach
    void setUp() {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("PDS");
        ReflectionTestUtils.setField(disciplina, "id", disciplinaId);
        aula = new Aula();
        aula.setDisciplina(disciplina);
        aula.setData(LocalDate.of(2026, 10, 20));
        aula.setHorario("08:00");
        autor = new Usuario();
        autor.setNome("Maria");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId.toString(), null, List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private AssistenteServiceImpl service(int maximoImagens, LimitePerguntasAssistente limite) {
        return new AssistenteServiceImpl(aulaRepository, anotacaoRepository, imagemAulaRepository, participacaoRepository,
                armazenamento, new ReducaoDeImagem(), modelo, limite, new UsuarioAutenticado(usuarioRepository), maximoImagens);
    }

    private void participanteDeAulaComImagens(ImagemAula... imagens) {
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(participacaoRepository.existsByUsuarioIdAndDisciplinaIdAndStatus(usuarioId, disciplinaId, StatusParticipacao.ACEITO))
                .thenReturn(true);
        when(modelo.configurado()).thenReturn(true);
        when(anotacaoRepository.findByAulaIdOrderByDataCriacaoAsc(aulaId)).thenReturn(List.of());
        when(imagemAulaRepository.findByAulaIdOrderByPosicaoAscDataCriacaoAsc(aulaId)).thenReturn(List.of(imagens));
    }

    private ImagemAula imagem(String legenda) {
        ImagemAula imagem = new ImagemAula();
        ReflectionTestUtils.setField(imagem, "id", UUID.randomUUID());
        imagem.setLegenda(legenda);
        imagem.setAutor(autor);
        imagem.setChaveArmazenamento(legenda + ".png");
        imagem.setContentType("image/png");
        return imagem;
    }

    private static PerguntaAssistenteDTO pergunta(String texto, Foco foco) {
        return new PerguntaAssistenteDTO(List.of(new Mensagem(Papel.USUARIO, texto, foco)));
    }

    private static List<PedidoAoModelo.Bloco> blocosDaPrimeira(PedidoAoModelo pedido) {
        return pedido.mensagens().getFirst().blocos();
    }

    @Test
    void oLimiteDeImagensSempreIncluiAImagemEmFoco() {
        ImagemAula primeira = imagem("primeira");
        ImagemAula segunda = imagem("segunda");
        ImagemAula terceira = imagem("terceira");
        participanteDeAulaComImagens(primeira, segunda, terceira);
        when(armazenamento.carregar(anyString())).thenReturn(Optional.of(new ByteArrayResource(new byte[] {1, 2, 3})));

        PedidoAoModelo pedido = service(2, new LimitePerguntasAssistente(30))
                .preparar(aulaId, pergunta("O que mostra?", new Foco(TipoFoco.IMAGEM, terceira.getId())));

        List<String> descricoes = blocosDaPrimeira(pedido).stream()
                .filter(PedidoAoModelo.Texto.class::isInstance)
                .map(bloco -> ((PedidoAoModelo.Texto) bloco).texto())
                .filter(texto -> texto.startsWith("Imagem "))
                .toList();
        assertThat(descricoes).hasSize(2);
        assertThat(descricoes.get(0)).contains("Imagem 1", "primeira");
        assertThat(descricoes.get(1)).contains("Imagem 3", "terceira");
        assertThat(blocosDaPrimeira(pedido)).filteredOn(PedidoAoModelo.Imagem.class::isInstance).hasSize(2);

        PedidoAoModelo.Texto fechamento = (PedidoAoModelo.Texto) blocosDaPrimeira(pedido).get(blocosDaPrimeira(pedido).size() - 2);
        assertThat(fechamento.texto()).contains("Imagem 2", "segunda", "não enviada, porque o limite é de 2 imagens");
        assertThat(((PedidoAoModelo.Texto) blocosDaPrimeira(pedido).getLast()).texto())
                .isEqualTo("[Pergunta sobre a imagem 3]\nO que mostra?");
    }

    @Test
    void soOFechamentoDoMaterialFicaMarcadoParaCache() {
        participanteDeAulaComImagens(imagem("quadro"));
        when(armazenamento.carregar(anyString())).thenReturn(Optional.empty()); // arquivo sumiu do disco

        PedidoAoModelo pedido = service(20, new LimitePerguntasAssistente(30)).preparar(aulaId, pergunta("Resuma", null));

        List<PedidoAoModelo.Texto> marcados = blocosDaPrimeira(pedido).stream()
                .filter(bloco -> bloco instanceof PedidoAoModelo.Texto texto && texto.fimDoMaterial())
                .map(PedidoAoModelo.Texto.class::cast)
                .toList();
        assertThat(marcados).singleElement().satisfies(texto -> {
            assertThat(texto.texto()).endsWith("</material_da_aula>");
            assertThat(texto.texto()).contains("Imagem 1", "não pôde ser carregada");
        });
        assertThat(blocosDaPrimeira(pedido)).noneMatch(PedidoAoModelo.Imagem.class::isInstance);
        assertThat(((PedidoAoModelo.Texto) blocosDaPrimeira(pedido).getFirst()).texto()).contains("A aula ainda não tem anotações.");
    }

    @Test
    void semModeloConfiguradoOAssistenteFicaIndisponivelSemGastarOLimite() {
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(participacaoRepository.existsByUsuarioIdAndDisciplinaIdAndStatus(usuarioId, disciplinaId, StatusParticipacao.ACEITO))
                .thenReturn(true);
        when(modelo.configurado()).thenReturn(false);
        LimitePerguntasAssistente limite = new LimitePerguntasAssistente(1);

        assertThatThrownBy(() -> service(20, limite).preparar(aulaId, pergunta("Resuma", null)))
                .isInstanceOf(AssistenteIndisponivelException.class);
        limite.registrar(usuarioId); // a pergunta recusada não contou
    }

    @Test
    void passarDoLimitePorHoraDa429() {
        participanteDeAulaComImagens();
        AssistenteServiceImpl service = service(20, new LimitePerguntasAssistente(2));

        service.preparar(aulaId, pergunta("Uma", null));
        service.preparar(aulaId, pergunta("Duas", null));
        assertThatThrownBy(() -> service.preparar(aulaId, pergunta("Três", null)))
                .isInstanceOf(LimiteDePerguntasException.class)
                .hasMessageContaining("limite de 2 perguntas por hora");
    }
}
