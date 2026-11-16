package com.allgenda.framework.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.acesso.PoliticaDeTeste;
import com.allgenda.framework.assistente.ComportamentoDoAssistente;
import com.allgenda.framework.extensao.DadosDoGrupo;
import com.allgenda.framework.extensao.DefinicaoDaAplicacao;
import com.allgenda.framework.extensao.FabricaDeGrupo;
import com.allgenda.framework.extensao.RegistroDeTipos;
import com.allgenda.framework.model.Grupo;

// O núcleo sobe com exatamente uma DefinicaoDaAplicacao; com nenhuma ou com duas, a inicialização falha
// dizendo o que fazer.
class ConfiguracaoDaAplicacaoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracaoDaAplicacao.class);

    private static DefinicaoDaAplicacao definicao(String nome) {
        return new DefinicaoDaAplicacao() {
            @Override
            public String nome() {
                return nome;
            }

            @Override
            public void registrarTipos(RegistroDeTipos registro) {
                registro.registrar(new FabricaDeGrupo() {
                    @Override
                    public String tipo() {
                        return "CLUBE";
                    }

                    @Override
                    public Grupo criar(DadosDoGrupo dados) {
                        return new Grupo() {
                            @Override
                            public String tipo() {
                                return "CLUBE";
                            }
                        };
                    }
                });
            }

            @Override
            public PoliticaDeAcesso politicaDeAcesso() {
                return PoliticaDeTeste.politica();
            }

            @Override
            public ComportamentoDoAssistente comportamentoDoAssistente() {
                return (grupo, destaque) -> "Você ajuda o clube " + grupo.getNome() + ".";
            }
        };
    }

    @Test
    void comUmaDefinicaoOsTiposSaoRegistradosEOsPontosVariaveisViramBeans() {
        contexto.withBean("clube", DefinicaoDaAplicacao.class, () -> definicao("Clube do livro"))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(RegistroDeTipos.class).fabricaDeGrupo("CLUBE")).isNotNull();
                    assertThat(ctx).hasSingleBean(PoliticaDeAcesso.class);
                    assertThat(ctx).hasSingleBean(ComportamentoDoAssistente.class);
                });
    }

    @Test
    void semDefinicaoAInicializacaoFalhaComMensagemClara() {
        contexto.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Nenhuma aplicação definida")
                    .hasMessageContaining("DefinicaoDaAplicacao");
        });
    }

    @Test
    void comDuasDefinicoesAInicializacaoFalhaComMensagemClara() {
        contexto.withBean("clube", DefinicaoDaAplicacao.class, () -> definicao("Clube do livro"))
                .withBean("rpg", DefinicaoDaAplicacao.class, () -> definicao("Mesa de RPG"))
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Mais de uma aplicação definida")
                            .hasMessageContaining("Clube do livro")
                            .hasMessageContaining("Mesa de RPG");
                });
    }
}
