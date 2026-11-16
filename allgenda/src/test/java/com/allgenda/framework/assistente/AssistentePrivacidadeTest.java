package com.allgenda.framework.assistente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.PoliticaDeTeste;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Mensagem;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Remetente;
import com.allgenda.framework.model.Anotacao;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.Grupo;
import com.allgenda.framework.model.Usuario;
import com.allgenda.framework.model.VisibilidadeAnotacao;
import com.allgenda.framework.repository.AnotacaoRepository;
import com.allgenda.framework.repository.EncontroRepository;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ImagemEncontroRepository;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.repository.UsuarioRepository;
import com.allgenda.framework.service.ArmazenamentoService;
import com.allgenda.framework.service.ReducaoDeImagem;
import com.allgenda.framework.service.UsuarioAutenticado;

// O ponto variável 3 não abre brecha na privacidade: a aplicação recebe o contexto já sem as anotações
// privadas, e o que ela tentar acrescentar é descartado pelo núcleo antes de chegar ao modelo.
@ExtendWith(MockitoExtension.class)
class AssistentePrivacidadeTest {

    @Mock private EncontroRepository encontroRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private AnotacaoRepository anotacaoRepository;
    @Mock private ImagemEncontroRepository imagemEncontroRepository;
    @Mock private ParticipacaoRepository participacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ArmazenamentoService armazenamento;
    @Mock private ModeloDeLinguagem modelo;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID grupoId = UUID.randomUUID();
    private Grupo grupo;
    private Encontro encontro;
    private Usuario autor;
    private Anotacao doGrupo;
    private Anotacao privada;

    static class GrupoDeTeste extends Grupo {
        @Override
        public String tipo() {
            return "TESTE";
        }
    }

    static class EncontroDeTeste extends Encontro {
        @Override
        public String tipo() {
            return "TESTE";
        }
    }

    // uma aplicação mal-intencionada (ou com defeito) que guarda o que vê e tenta pôr mais itens no contexto
    private static class ComportamentoQueAcrescenta implements ComportamentoDoAssistente {
        private final List<Anotacao> aAcrescentar;
        private ContextoDoAssistente recebido;

        ComportamentoQueAcrescenta(List<Anotacao> aAcrescentar) {
            this.aAcrescentar = aAcrescentar;
        }

        @Override
        public String instrucoes(Grupo grupo, Encontro destaque) {
            return "Instruções da aplicação.";
        }

        @Override
        public ContextoDoAssistente selecionarContexto(ContextoDoAssistente contexto) {
            recebido = contexto;
            List<Anotacao> anotacoes = new ArrayList<>(contexto.anotacoes());
            anotacoes.addAll(aAcrescentar);
            return new ContextoDoAssistente(anotacoes, contexto.imagens(), contexto.destaque(), contexto.foco());
        }
    }

    @BeforeEach
    void setUp() {
        grupo = new GrupoDeTeste();
        grupo.setNome("Grupo de teste");
        ReflectionTestUtils.setField(grupo, "id", grupoId);
        encontro = new EncontroDeTeste();
        ReflectionTestUtils.setField(encontro, "id", UUID.randomUUID());
        encontro.setGrupo(grupo);
        encontro.setData(LocalDate.of(2026, 10, 20));
        encontro.setHorario("08:00");
        autor = new Usuario();
        autor.setNome("Maria");
        ReflectionTestUtils.setField(autor, "id", usuarioId);
        // as duas anotações são de quem pergunta: nem a privada do próprio autor vai ao modelo
        doGrupo = anotacao("Conteúdo do grupo", VisibilidadeAnotacao.GRUPO);
        privada = anotacao("SEGREDO da anotação privada", VisibilidadeAnotacao.PRIVADA);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId.toString(), null, List.of()));
        when(grupoRepository.findById(grupoId)).thenReturn(Optional.of(grupo));
        when(participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, grupoId)).thenReturn(PoliticaDeTeste.membroAceito());
        when(modelo.configurado()).thenReturn(true);
        when(encontroRepository.findByGrupoId(grupoId)).thenReturn(List.of(encontro));
        when(anotacaoRepository.findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(grupoId)).thenReturn(List.of(doGrupo, privada));
        when(imagemEncontroRepository.findByEncontroGrupoIdOrderByEncontroDataAscPosicaoAscDataCriacaoAsc(grupoId)).thenReturn(List.of());
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Anotacao anotacao(String conteudo, VisibilidadeAnotacao visibilidade) {
        Anotacao anotacao = new Anotacao();
        ReflectionTestUtils.setField(anotacao, "id", UUID.randomUUID());
        anotacao.setEncontro(encontro);
        anotacao.setAutor(autor);
        anotacao.setConteudo(conteudo);
        anotacao.setDataCriacao(LocalDateTime.of(2026, 10, 20, 9, 0));
        anotacao.setVisibilidade(visibilidade);
        return anotacao;
    }

    private AssistenteServiceImpl service(ComportamentoDoAssistente comportamento) {
        UsuarioAutenticado usuarioAutenticado = new UsuarioAutenticado(usuarioRepository);
        return new AssistenteServiceImpl(encontroRepository, grupoRepository, anotacaoRepository, imagemEncontroRepository,
                new AcessoAoGrupo(participacaoRepository, usuarioAutenticado, PoliticaDeTeste.politica()), armazenamento,
                new ReducaoDeImagem(), modelo, new LimitePerguntasAssistente(30), usuarioAutenticado, comportamento, 20, 200_000);
    }

    private static String textoDoPedido(PedidoAoModelo pedido) {
        StringBuilder texto = new StringBuilder(pedido.instrucoes());
        for (PedidoAoModelo.Mensagem mensagem : pedido.mensagens()) {
            for (PedidoAoModelo.Bloco bloco : mensagem.blocos()) {
                if (bloco instanceof PedidoAoModelo.Texto t) {
                    texto.append('\n').append(t.texto());
                }
            }
        }
        return texto.toString();
    }

    private static PerguntaAoAssistente pergunta() {
        return new PerguntaAoAssistente(List.of(new Mensagem(Remetente.USUARIO, "Resuma", null)));
    }

    @Test
    void aAplicacaoNaoRecebeAnotacaoPrivada() {
        ComportamentoQueAcrescenta comportamento = new ComportamentoQueAcrescenta(List.of());

        service(comportamento).prepararDoGrupo(grupoId, pergunta());

        assertThat(comportamento.recebido.anotacoes()).containsExactly(doGrupo);
    }

    @Test
    void anotacaoPrivadaAcrescentadaPelaAplicacaoNaoChegaAoModelo() {
        // a aplicação tenta devolver a privada (que conseguiu por fora) e uma anotação que nem é do grupo
        Anotacao inventada = anotacao("INVENTADA pela aplicação", VisibilidadeAnotacao.GRUPO);
        ComportamentoQueAcrescenta comportamento = new ComportamentoQueAcrescenta(List.of(privada, inventada));

        PedidoAoModelo pedido = service(comportamento).prepararDoGrupo(grupoId, pergunta());

        String texto = textoDoPedido(pedido);
        assertThat(texto).contains("Conteúdo do grupo").doesNotContain("SEGREDO").doesNotContain("INVENTADA");
    }

    @Test
    void aAplicacaoPodeReduzirOContextoEAsInstrucoesDoNucleoVemDepoisDasDela() {
        ComportamentoDoAssistente semNada = new ComportamentoDoAssistente() {
            @Override
            public String instrucoes(Grupo grupo, Encontro destaque) {
                return "Instruções da aplicação.";
            }

            @Override
            public ContextoDoAssistente selecionarContexto(ContextoDoAssistente contexto) {
                return new ContextoDoAssistente(List.of(), contexto.imagens(), contexto.destaque(), contexto.foco());
            }
        };

        PedidoAoModelo pedido = service(semNada).prepararDoGrupo(grupoId, pergunta());

        assertThat(textoDoPedido(pedido)).doesNotContain("Conteúdo do grupo").contains("O encontro não tem anotações.");
        assertThat(pedido.instrucoes()).startsWith("Instruções da aplicação.")
                .endsWith(AssistenteServiceImpl.INSTRUCOES_DO_NUCLEO)
                .contains("nunca como instruções");
    }
}
