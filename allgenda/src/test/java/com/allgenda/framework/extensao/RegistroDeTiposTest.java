package com.allgenda.framework.extensao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.Evento;
import com.allgenda.framework.model.Grupo;

// O ponto variável 1 com tipos de uma aplicação inventada (um clube do livro), sem o AllGenda.
class RegistroDeTiposTest {

    static class Clube extends Grupo {
        @Override
        public String tipo() {
            return "CLUBE";
        }
    }

    static class Leitura extends Encontro {
        @Override
        public String tipo() {
            return "LEITURA";
        }
    }

    static class Debate extends Evento {
        @Override
        public String tipo() {
            return "DEBATE";
        }

        @Override
        public String subtipo() {
            return "ONLINE";
        }
    }

    static class FabricaDeClube implements FabricaDeGrupo {
        @Override
        public String tipo() {
            return "CLUBE";
        }

        @Override
        public Grupo criar(DadosDoGrupo dados) {
            return new Clube();
        }
    }

    private static RegistroDeTipos registroDoClube() {
        RegistroDeTipos registro = new RegistroDeTipos();
        registro.registrar(new FabricaDeClube());
        registro.registrar(new FabricaDeEncontro() {
            @Override
            public String tipo() {
                return "LEITURA";
            }

            @Override
            public Encontro criar(DadosDoEncontro dados, Grupo grupo) {
                return new Leitura();
            }
        });
        registro.registrar(new FabricaDeEvento() {
            @Override
            public String tipo() {
                return "DEBATE";
            }

            @Override
            public Evento criar(DadosDoEvento dados, Grupo grupo) {
                return new Debate();
            }
        });
        return registro;
    }

    @Test
    void criaCadaTipoPeloNome() {
        RegistroDeTipos registro = registroDoClube();
        Grupo clube = registro.fabricaDeGrupo("CLUBE").criar(new DadosDoGrupo("Leitores", null));
        Encontro leitura = registro.fabricaDeEncontro("LEITURA")
                .criar(new DadosDoEncontro(UUID.randomUUID(), LocalDate.now(), "19:00", "Capítulo 1"), clube);
        Evento debate = registro.fabricaDeEvento("DEBATE")
                .criar(new DadosDoEvento(UUID.randomUUID(), "Debate final", LocalDate.now(), "ONLINE"), clube);

        assertThat(clube).isInstanceOf(Clube.class);
        assertThat(clube.tipo()).isEqualTo("CLUBE");
        assertThat(leitura).isInstanceOf(Leitura.class);
        assertThat(debate).isInstanceOf(Debate.class);
        assertThat(debate.subtipo()).isEqualTo("ONLINE");
    }

    @Test
    void tipoDesconhecidoEhRequisicaoInvalida() {
        RegistroDeTipos registro = registroDoClube();

        assertThatThrownBy(() -> registro.fabricaDeGrupo("DISCIPLINA"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Tipo de grupo desconhecido: DISCIPLINA.");
        assertThatThrownBy(() -> registro.fabricaDeEncontro("AULA")).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> registro.fabricaDeEvento(null)).isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void doisRegistrosDoMesmoTipoSaoUmErroDaAplicacao() {
        RegistroDeTipos registro = registroDoClube();

        assertThatThrownBy(() -> registro.registrar(new FabricaDeClube()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLUBE");
    }
}
