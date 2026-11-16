package com.allgenda.framework.acesso;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.allgenda.model.StatusParticipacao;

// Implementação da PoliticaDeAcesso montada por declaração, sem código: para cada papel, a lista de ações
// que ele pode. Ex.:
//   PoliticaDeAcessoDeclarativa.construtor()
//       .papel(DONO, Acao.values())
//       .papel(MEMBRO, Acao.VER_CONTEUDO, Acao.CRIAR_ANOTACAO)
//       .papelDoCriador(DONO).papelDoConvidado(MEMBRO).statusAoIngressar(StatusParticipacao.PENDENTE)
//       .construir();
// Papel não declarado ou ação não listada para o papel: não permite.
public final class PoliticaDeAcessoDeclarativa implements PoliticaDeAcesso {

    private final Set<Papel> papeis;
    private final Map<Acao, Set<Papel>> permissoes; // ação -> papéis que podem fazê-la
    private final Papel papelDoCriador;
    private final Papel papelDoConvidado;
    private final StatusParticipacao statusAoIngressar;

    private PoliticaDeAcessoDeclarativa(Construtor construtor) {
        this.papeis = Collections.unmodifiableSet(new LinkedHashSet<>(construtor.papeis));
        Map<Acao, Set<Papel>> copia = new EnumMap<>(Acao.class);
        construtor.permissoes.forEach((acao, papeisDaAcao) -> copia.put(acao, Set.copyOf(papeisDaAcao)));
        this.permissoes = Collections.unmodifiableMap(copia);
        this.papelDoCriador = construtor.papelDoCriador;
        this.papelDoConvidado = construtor.papelDoConvidado;
        this.statusAoIngressar = construtor.statusAoIngressar;
    }

    public static Construtor construtor() {
        return new Construtor();
    }

    @Override
    public Set<Papel> papeis() {
        return papeis;
    }

    @Override
    public Papel papelDoCriador() {
        return papelDoCriador;
    }

    @Override
    public Papel papelDoConvidado() {
        return papelDoConvidado;
    }

    @Override
    public boolean permite(Papel papel, Acao acao) {
        return papel != null && acao != null && permissoes.getOrDefault(acao, Set.of()).contains(papel);
    }

    @Override
    public StatusParticipacao statusAoIngressar() {
        return statusAoIngressar;
    }

    public static final class Construtor {

        private final Set<Papel> papeis = new LinkedHashSet<>();
        private final Map<Acao, Set<Papel>> permissoes = new EnumMap<>(Acao.class);
        private Papel papelDoCriador;
        private Papel papelDoConvidado;
        private StatusParticipacao statusAoIngressar;

        private Construtor() {
        }

        // declara o papel e as ações que ele pode (chamar de novo para o mesmo papel acrescenta ações)
        public Construtor papel(Papel papel, Acao... acoes) {
            if (papel == null) {
                throw new IllegalArgumentException("O papel não pode ser nulo.");
            }
            papeis.add(papel);
            for (Acao acao : acoes) {
                permissoes.computeIfAbsent(acao, a -> new LinkedHashSet<>()).add(papel);
            }
            return this;
        }

        public Construtor papelDoCriador(Papel papel) {
            this.papelDoCriador = papel;
            return this;
        }

        public Construtor papelDoConvidado(Papel papel) {
            this.papelDoConvidado = papel;
            return this;
        }

        public Construtor statusAoIngressar(StatusParticipacao status) {
            this.statusAoIngressar = status;
            return this;
        }

        // falha logo na inicialização se a declaração estiver incompleta ou incoerente
        public PoliticaDeAcessoDeclarativa construir() {
            exigirDeclarado(papelDoCriador, "do criador do grupo");
            exigirDeclarado(papelDoConvidado, "do convidado");
            if (statusAoIngressar == null) {
                throw new IllegalStateException("A política de acesso precisa definir o status de quem ingressa no grupo.");
            }
            return new PoliticaDeAcessoDeclarativa(this);
        }

        private void exigirDeclarado(Papel papel, String qual) {
            if (papel == null) {
                throw new IllegalStateException("A política de acesso precisa definir o papel " + qual + ".");
            }
            if (!papeis.contains(papel)) {
                throw new IllegalStateException("O papel " + qual + " (" + papel + ") não foi declarado na política de acesso.");
            }
        }
    }
}
