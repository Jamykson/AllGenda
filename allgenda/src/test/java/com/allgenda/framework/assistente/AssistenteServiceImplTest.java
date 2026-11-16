package com.allgenda.framework.assistente;

import com.allgenda.aplicacao.allgenda.AssistenteTutor;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Foco;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Mensagem;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Remetente;
import com.allgenda.framework.assistente.PerguntaAoAssistente.TipoFoco;
import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.exception.AssistenteIndisponivelException;
import com.allgenda.framework.exception.LimiteDePerguntasException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Anotacao;
import com.allgenda.aplicacao.allgenda.model.Aula;
import com.allgenda.aplicacao.allgenda.model.Disciplina;
import com.allgenda.framework.model.ImagemEncontro;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.model.Usuario;
import com.allgenda.framework.repository.AnotacaoRepository;
import com.allgenda.framework.repository.EncontroRepository;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ImagemEncontroRepository;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.repository.UsuarioRepository;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.PoliticaDeTeste;
import com.allgenda.framework.service.ArmazenamentoService;
import com.allgenda.framework.service.ReducaoDeImagem;
import com.allgenda.framework.service.UsuarioAutenticado;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistenteServiceImplTest {

    @Mock private EncontroRepository encontroRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private AnotacaoRepository anotacaoRepository;
    @Mock private ImagemEncontroRepository imagemEncontroRepository;
    @Mock private ParticipacaoRepository participacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ArmazenamentoService armazenamento;
    @Mock private ModeloDeLinguagem modelo;

    private final UUID aulaId = UUID.randomUUID();
    private final UUID disciplinaId = UUID.randomUUID();
    private Disciplina disciplina;
    private final UUID usuarioId = UUID.randomUUID();
    private Aula aula;
    private Usuario autor;

    @BeforeEach
    void setUp() {
        disciplina = new Disciplina();
        disciplina.setNome("PDS");
        ReflectionTestUtils.setField(disciplina, "id", disciplinaId);
        aula = new Aula();
        ReflectionTestUtils.setField(aula, "id", aulaId);
        aula.setGrupo(disciplina);
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
        return service(maximoImagens, 200_000, limite);
    }

    private AssistenteServiceImpl service(int maximoImagens, int maximoCaracteres, LimitePerguntasAssistente limite) {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(usuarioRepository);
        return new AssistenteServiceImpl(encontroRepository, grupoRepository, anotacaoRepository, imagemEncontroRepository,
                new AcessoAoGrupo(participacaoRepository, usuarioAutenticado, PoliticaDeTeste.politica()), armazenamento, new ReducaoDeImagem(),
                modelo, limite, usuarioAutenticado, new AssistenteTutor(), maximoImagens, maximoCaracteres);
    }

    private void participanteDeAulaComImagens(ImagemEncontro... imagens) {
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, disciplinaId)).thenReturn(PoliticaDeTeste.membroAceito());
        when(modelo.configurado()).thenReturn(true);
        when(encontroRepository.findByGrupoId(disciplinaId)).thenReturn(List.of(aula));
        when(anotacaoRepository.findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(disciplinaId)).thenReturn(List.of());
        when(imagemEncontroRepository.findByEncontroGrupoIdOrderByEncontroDataAscPosicaoAscDataCriacaoAsc(disciplinaId))
                .thenReturn(List.of(imagens));
    }

    private ImagemEncontro imagem(String legenda) {
        ImagemEncontro imagem = new ImagemEncontro();
        ReflectionTestUtils.setField(imagem, "id", UUID.randomUUID());
        imagem.setLegenda(legenda);
        imagem.setAutor(autor);
        imagem.setEncontro(aula);
        imagem.setChaveArmazenamento(legenda + ".png");
        imagem.setContentType("image/png");
        return imagem;
    }

    private static PerguntaAoAssistente pergunta(String texto, Foco foco) {
        return new PerguntaAoAssistente(List.of(new Mensagem(Remetente.USUARIO, texto, foco)));
    }

    private static List<PedidoAoModelo.Bloco> blocosDaPrimeira(PedidoAoModelo pedido) {
        return pedido.mensagens().getFirst().blocos();
    }

    @Test
    void oLimiteDeImagensSempreIncluiAImagemEmFoco() {
        ImagemEncontro primeira = imagem("primeira");
        ImagemEncontro segunda = imagem("segunda");
        ImagemEncontro terceira = imagem("terceira");
        participanteDeAulaComImagens(primeira, segunda, terceira);
        when(armazenamento.carregar(anyString())).thenReturn(Optional.of(new ByteArrayResource(new byte[] {1, 2, 3})));

        PedidoAoModelo pedido = service(2, new LimitePerguntasAssistente(30))
                .prepararDoEncontro(aulaId, pergunta("O que mostra?", new Foco(TipoFoco.IMAGEM, terceira.getId())));

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

        PedidoAoModelo pedido = service(20, new LimitePerguntasAssistente(30)).prepararDoEncontro(aulaId, pergunta("Resuma", null));

        List<PedidoAoModelo.Texto> marcados = blocosDaPrimeira(pedido).stream()
                .filter(bloco -> bloco instanceof PedidoAoModelo.Texto texto && texto.fimDoMaterial())
                .map(PedidoAoModelo.Texto.class::cast)
                .toList();
        assertThat(marcados).singleElement().satisfies(texto -> {
            assertThat(texto.texto()).endsWith("</material_do_grupo>");
            assertThat(texto.texto()).contains("Imagem 1", "não pôde ser carregada");
        });
        assertThat(blocosDaPrimeira(pedido)).noneMatch(PedidoAoModelo.Imagem.class::isInstance);
        assertThat(((PedidoAoModelo.Texto) blocosDaPrimeira(pedido).getFirst()).texto()).contains("O encontro não tem anotações.");
    }

    @Test
    void semModeloConfiguradoOAssistenteFicaIndisponivelSemGastarOLimite() {
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, disciplinaId)).thenReturn(PoliticaDeTeste.membroAceito());
        when(modelo.configurado()).thenReturn(false);
        LimitePerguntasAssistente limite = new LimitePerguntasAssistente(1);

        assertThatThrownBy(() -> service(20, limite).prepararDoEncontro(aulaId, pergunta("Resuma", null)))
                .isInstanceOf(AssistenteIndisponivelException.class);
        limite.registrar(usuarioId); // a pergunta recusada não contou
    }

    @Test
    void passarDoLimitePorHoraDa429() {
        participanteDeAulaComImagens();
        AssistenteServiceImpl service = service(20, new LimitePerguntasAssistente(2));

        service.prepararDoEncontro(aulaId, pergunta("Uma", null));
        service.prepararDoEncontro(aulaId, pergunta("Duas", null));
        assertThatThrownBy(() -> service.prepararDoEncontro(aulaId, pergunta("Três", null)))
                .isInstanceOf(LimiteDePerguntasException.class)
                .hasMessageContaining("limite de 2 perguntas por hora");
    }

    private Aula outraAula(LocalDate data) {
        Aula outra = new Aula();
        ReflectionTestUtils.setField(outra, "id", UUID.randomUUID());
        outra.setGrupo(disciplina);
        outra.setData(data);
        outra.setHorario("08:00");
        return outra;
    }

    private Anotacao anotacao(Aula daAula, String letra, LocalDateTime criadaEm) {
        Anotacao anotacao = new Anotacao();
        ReflectionTestUtils.setField(anotacao, "id", UUID.randomUUID());
        anotacao.setEncontro(daAula);
        anotacao.setAutor(autor);
        anotacao.setConteudo(letra.repeat(1000));
        anotacao.setDataCriacao(criadaEm);
        return anotacao;
    }

    // três anotações de ~1100 caracteres, das quais só uma cabe no limite de 1500
    private void disciplinaComTresAnotacoes(Aula recente, Anotacao... anotacoes) {
        when(participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, disciplinaId)).thenReturn(PoliticaDeTeste.membroAceito());
        when(modelo.configurado()).thenReturn(true);
        when(encontroRepository.findByGrupoId(disciplinaId)).thenReturn(List.of(recente, aula));
        when(anotacaoRepository.findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(disciplinaId)).thenReturn(List.of(anotacoes));
        when(imagemEncontroRepository.findByEncontroGrupoIdOrderByEncontroDataAscPosicaoAscDataCriacaoAsc(disciplinaId)).thenReturn(List.of());
    }

    private static String materialEmTexto(PedidoAoModelo pedido) {
        StringBuilder texto = new StringBuilder();
        for (PedidoAoModelo.Bloco bloco : blocosDaPrimeira(pedido)) {
            if (bloco instanceof PedidoAoModelo.Texto t) {
                texto.append(t.texto()).append('\n');
            }
        }
        return texto.toString();
    }

    @Test
    void passandoDoLimiteDeCaracteresAAulaEmDestaqueTemPrioridade() {
        Aula recente = outraAula(LocalDate.of(2026, 10, 25));
        Anotacao daAulaAberta = anotacao(aula, "x", LocalDateTime.of(2026, 10, 20, 9, 0));
        Anotacao recente1 = anotacao(recente, "y", LocalDateTime.of(2026, 10, 25, 9, 0));
        Anotacao recente2 = anotacao(recente, "z", LocalDateTime.of(2026, 10, 25, 10, 0));
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        disciplinaComTresAnotacoes(recente, daAulaAberta, recente1, recente2);

        PedidoAoModelo pedido = service(20, 1500, new LimitePerguntasAssistente(30))
                .prepararDoEncontro(aulaId, pergunta("Resuma", null));

        String material = materialEmTexto(pedido);
        assertThat(material).contains("x".repeat(1000)).doesNotContain("y".repeat(1000)).doesNotContain("z".repeat(1000));
        assertThat(material).contains("encontro de 25/10/2026: 2 anotações", "Anotações deste encontro que você não pode ver: 2.");
        assertThat(pedido.instrucoes()).contains("página da aula de 20/10/2026");
    }

    @Test
    void semAulaEmDestaqueAsAnotacoesMaisRecentesTemPrioridade() {
        Aula recente = outraAula(LocalDate.of(2026, 10, 25));
        Anotacao daAulaAntiga = anotacao(aula, "x", LocalDateTime.of(2026, 10, 20, 9, 0));
        Anotacao recente1 = anotacao(recente, "y", LocalDateTime.of(2026, 10, 25, 9, 0));
        Anotacao recente2 = anotacao(recente, "z", LocalDateTime.of(2026, 10, 25, 10, 0));
        when(grupoRepository.findById(disciplinaId)).thenReturn(Optional.of(disciplina));
        disciplinaComTresAnotacoes(recente, daAulaAntiga, recente1, recente2);

        PedidoAoModelo pedido = service(20, 1500, new LimitePerguntasAssistente(30))
                .prepararDoGrupo(disciplinaId, pergunta("Resuma", null));

        String material = materialEmTexto(pedido);
        assertThat(material).contains("z".repeat(1000)).doesNotContain("x".repeat(1000)).doesNotContain("y".repeat(1000));
        // os números não mudam quando algo fica de fora: a anotação z é a terceira
        assertThat(material).contains("<anotacao numero=\"3\"");
        assertThat(pedido.instrucoes()).doesNotContain("página da aula");
    }

    @Test
    void anotacaoEmFocoEntraMesmoPassandoDoLimiteDeCaracteres() {
        Aula recente = outraAula(LocalDate.of(2026, 10, 25));
        Anotacao antiga = anotacao(aula, "x", LocalDateTime.of(2026, 10, 20, 9, 0));
        Anotacao recente1 = anotacao(recente, "y", LocalDateTime.of(2026, 10, 25, 9, 0));
        Anotacao recente2 = anotacao(recente, "z", LocalDateTime.of(2026, 10, 25, 10, 0));
        when(grupoRepository.findById(disciplinaId)).thenReturn(Optional.of(disciplina));
        disciplinaComTresAnotacoes(recente, antiga, recente1, recente2);

        PedidoAoModelo pedido = service(20, 1500, new LimitePerguntasAssistente(30))
                .prepararDoGrupo(disciplinaId, pergunta("Explique", new Foco(TipoFoco.ANOTACAO, antiga.getId())));

        assertThat(materialEmTexto(pedido)).contains("x".repeat(1000));
    }

    @Test
    void focoDeOutraDisciplinaERecusadoSemGastarOLimite() {
        when(grupoRepository.findById(disciplinaId)).thenReturn(Optional.of(disciplina));
        disciplinaComTresAnotacoes(outraAula(LocalDate.of(2026, 10, 25)));
        LimitePerguntasAssistente limite = new LimitePerguntasAssistente(1);

        assertThatThrownBy(() -> service(20, limite).prepararDoGrupo(disciplinaId,
                pergunta("Explique", new Foco(TipoFoco.ANOTACAO, UUID.randomUUID()))))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("O item em foco não pertence a este grupo.");
        limite.registrar(usuarioId); // a pergunta recusada não contou
    }

    @Test
    void quemNaoParticipaDaDisciplinaNaoUsaOAssistente() {
        when(grupoRepository.findById(disciplinaId)).thenReturn(Optional.of(disciplina));
        when(participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, disciplinaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(20, new LimitePerguntasAssistente(30)).prepararDoGrupo(disciplinaId, pergunta("Resuma", null)))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Somente participantes do grupo usam o assistente.");
    }
}
