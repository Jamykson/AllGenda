package com.allgenda.framework.acesso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.allgenda.framework.model.StatusParticipacao;

// O ponto variável 2 não depende do AllGenda: aqui a política usa papéis de outra aplicação, inventada
// (um clube em que o organizador cria o grupo e quem entra já participa, sem convite pendente).
class PoliticaDeAcessoDeclarativaTest {

    private static final Papel ORGANIZADOR = new Papel("ORGANIZADOR");
    private static final Papel PARTICIPANTE = new Papel("PARTICIPANTE");

    private static PoliticaDeAcessoDeclarativa.Construtor clube() {
        return PoliticaDeAcessoDeclarativa.construtor()
                .papel(ORGANIZADOR, Acao.values())
                .papel(PARTICIPANTE, Acao.VER_CONTEUDO, Acao.CRIAR_ANOTACAO)
                .papelDoCriador(ORGANIZADOR)
                .papelDoConvidado(PARTICIPANTE)
                .statusAoIngressar(StatusParticipacao.ACEITO);
    }

    @Test
    void permiteSoOQueFoiDeclaradoParaCadaPapel() {
        PoliticaDeAcesso politica = clube().construir();

        assertThat(politica.permite(ORGANIZADOR, Acao.EXCLUIR_GRUPO)).isTrue();
        assertThat(politica.permite(PARTICIPANTE, Acao.CRIAR_ANOTACAO)).isTrue();
        assertThat(politica.permite(PARTICIPANTE, Acao.CRIAR_ENCONTRO)).isFalse();
        assertThat(politica.permite(new Papel("VISITANTE"), Acao.VER_CONTEUDO)).isFalse(); // papel não declarado
        assertThat(politica.permite(null, Acao.VER_CONTEUDO)).isFalse();
    }

    @Test
    void papeisSaoComparadosPeloCodigo() {
        assertThat(clube().construir().permite(new Papel("PARTICIPANTE"), Acao.VER_CONTEUDO)).isTrue();
    }

    @Test
    void informaCriadorConvidadoEStatusAoIngressar() {
        PoliticaDeAcesso politica = clube().construir();

        assertThat(politica.papeis()).containsExactly(ORGANIZADOR, PARTICIPANTE);
        assertThat(politica.papelDoCriador()).isEqualTo(ORGANIZADOR);
        assertThat(politica.papelDoConvidado()).isEqualTo(PARTICIPANTE);
        assertThat(politica.statusAoIngressar()).isEqualTo(StatusParticipacao.ACEITO);
    }

    @Test
    void recusaCriadorNaoDeclarado() {
        assertThatThrownBy(() -> clube().papelDoCriador(new Papel("DONO")).construir())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("criador");
    }

    @Test
    void recusaConvidadoNaoDeclarado() {
        assertThatThrownBy(() -> clube().papelDoConvidado(new Papel("MEMBRO")).construir())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("convidado");
    }

    @Test
    void recusaPoliticaSemStatusAoIngressar() {
        assertThatThrownBy(() -> clube().statusAoIngressar(null).construir())
                .isInstanceOf(IllegalStateException.class);
    }
}
