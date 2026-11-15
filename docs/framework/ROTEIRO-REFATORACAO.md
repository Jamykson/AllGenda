# Roteiro: refatorar o AllGenda (versão 1) para um framework, com o AllGenda instanciado nele

> **Para o Claude Code que vai executar este roteiro.** Leia este arquivo inteiro antes de mexer em qualquer coisa. Ele reúne todo o contexto e todas as decisões já tomadas pelo grupo. Onde ele disser **"combinado"**, não mude; onde disser **"detalhe seu"**, você decide (e anota no PR); onde faltar informação, **pergunte ao usuário** em vez de inventar. Leia também `AGENTS.md`, `docs/api-contrato.md` e `docs/framework/diagrama-de-classes.md` (com as imagens em `docs/framework/img/`), que acompanham este roteiro.

---

## 1. Contexto

**O que é o projeto.** AllGenda é um sistema de agenda acadêmica com anotações colaborativas por disciplina e um assistente de IA que responde com base nas anotações. Back-end em `allgenda/` (Spring Boot 4.1, Java 25, Maven, Spring Data JPA, PostgreSQL, JWT stateless). Front-end em `frontend/` (Nuxt 4). Pacote Java raiz: `com.allgenda`. Código, comentários e nomes estão em **português**: mantenha.

**Por que esta refatoração.** Trabalho da disciplina DIM0138 (Projeto Detalhado de Software), feito em grupo de três. O professor pediu, para a próxima aula:

> "Vocês devem trazer o diagrama de classe indicando as mudanças feitas na versão 1 para atender o framework e também o código-fonte do mesmo (esse se não conseguir olhar na aula, olharei offline posteriormente). A aplicação 1 também já deverá ser instanciada no código do framework."

Ou seja: **(a)** o diagrama de classes das mudanças da versão 1 para o framework (já feito, em `docs/framework/`); **(b)** o código-fonte do framework; **(c)** a aplicação 1 (o AllGenda) já instanciada nesse código. Este roteiro cobre (b) e (c). O professor vai fazer perguntas ao grupo sobre o código, então a refatoração precisa ser **compreensível e bem explicada** (comentários curtos em português nos pontos novos, nomes claros).

**Conceitos da disciplina que o código precisa refletir** (slides do professor): *frozen spots* (núcleo fixo) e *hot spots* (pontos variáveis); inversão de controle (o núcleo chama a aplicação, não o contrário); caixa-branca/cinza/preta; Template Method; Strategy; Factory com registro. Nosso framework é **caixa-cinza**: um ponto variável é instanciado por **herança** e dois por **composição/configuração**.

**Estado atual (versão 1).** Já foram concluídos e estão no `main`, com testes passando, cinco commits preparatórios: assistente por disciplina inteira; anotação privada; descrição da disciplina e tópico da aula; lista/remoção de participantes e edição da descrição; editar/cancelar/excluir aula; recorrência semanal/quinzenal. **Não reimplemente nada disso**, apenas preserve o comportamento. Os dados do banco local são só de teste: **não há migração de dados**.

**Como rodar.** `mise run test` na raiz (roda `./mvnw test` em `allgenda/`, com Java 25 via mise e H2 em memória; não precisa de PostgreSQL nem de chave da Anthropic, os testes usam um modelo falso). Registre no início quantos testes passam (no último levantamento eram **224**, 0 falhas). O front-end **não deve ser alterado** e não precisa ser compilado.

---

## 2. Objetivo e critério de "pronto"

Transformar o back-end em duas partes dentro do mesmo módulo Maven:

- `com.allgenda.framework`: o **framework** (núcleo fixo + pontos variáveis).
- `com.allgenda.aplicacao.allgenda`: a **aplicação 1** (AllGenda), instanciada sobre o framework.

**Está pronto quando:**
1. Todos os testes que existiam continuam passando (com ajustes apenas nos *setups* e nas referências a classes renomeadas, conforme a seção 9). **Nenhuma rota, status HTTP ou JSON do AllGenda muda.** O front-end continua funcionando sem alteração.
2. Nada em `com.allgenda.framework` importa `com.allgenda.aplicacao` (há um teste que garante isso, seção 9.3).
3. Os três pontos variáveis existem como descrito na seção 5 e o AllGenda os usa.
4. Os testes novos da seção 9.3 passam.
5. A documentação (`AGENTS.md`, `README.md`, `docs/framework/`) reflete a nova estrutura.

---

## 3. Regras de trabalho (obrigatórias)

1. **Comportamento idêntico.** É uma refatoração, não uma mudança de funcionalidade. Regras de negócio, validações, status HTTP e JSON permanecem. Se algo no roteiro parecer exigir mudar comportamento, pare e pergunte.
2. **Rede de segurança = testes.** Rode `mise run test` ao fim de **cada fase** (seção 8). Só avance com tudo verde. Não apague nem enfraqueça asserções de testes; só adapte *setups*/nomes. As únicas asserções de texto que podem mudar estão listadas na seção 9.1.
3. **Faça em branch própria**, uma fase por commit (mensagens em português no estilo `refactor: ...` / `feat: ...`, com corpo explicando o porquê), e abra um PR. Não faça `git add -A`: adicione por diretório/arquivo. **Nunca versione** os arquivos `frontend/.gitignore`, `frontend/README.md`, `frontend/public/robots.txt`, `frontend/tsconfig.json` (têm só diferença de fim de linha locais) nem pasta de saídas do Claude.
4. **Não vá além do escopo.** Itens adiados (seção 11) **não** entram. Não crie as aplicações Mesa de RPG/Clube do livro. Não mexa no front.
5. **Não faça `mise run db:reset` nem apague dados sem perguntar** ao usuário (regra do `AGENTS.md`). Para testes não é necessário: H2 em memória.
6. **Preserve os nomes em português** e o estilo do código existente (camadas controller/service/repository/mapper/dto/exception/model). Comentários curtos em português explicando o *porquê*.
7. **Em dúvida, pergunte.** Liste dúvidas no PR; pare só se bloquearem.
8. O PR deve ter, na descrição, o mapa "classe da versão 1 → classe do framework" (seção 12), as decisões 🔶 que você tomou e o resultado dos testes.

---

## 4. Domínio e decisões combinadas

**Domínio do framework (combinado):** *gestão colaborativa de encontros e conteúdo por grupos*. Pessoas se reúnem em **grupos**, marcam **encontros** (que podem se repetir), registram **eventos** e escrevem **anotações** (do grupo ou privadas), com um **assistente de IA** que responde com base nas anotações. (Não é mais "acadêmico": o AllGenda é só uma instância.)

**Vocabulário v1 → framework:** Disciplina → Grupo; Aula → Encontro; Avaliação → Evento; Anotação continua Anotação; "dono" e "membro" passam a ser **papéis** definidos pela aplicação.

### 4.1 Pontos fixos (núcleo), combinado
`Grupo`, `Encontro`, `Evento`, `Anotacao` (**não especializada**: um único tipo serve a todas as aplicações), `Participacao`, `Tag`, `Usuario` e autenticação (JWT), `Recorrencia`, imagens (`ImagemEncontro`, `ImagemAnotacao`) e o **mecanismo do assistente** (validação da conversa, checagem de acesso, limite de perguntas, filtro de visibilidade, limites de texto/imagens, defesa contra injeção de prompt, chamada ao modelo, tratamento de erros).

### 4.2 Pontos variáveis (hot spots), combinado
1. **Tipos especializados de Grupo, Encontro e Evento** — instanciação por **herança** + **Factory com registro**. O `Evento` ganha, em cada aplicação, **uma subclasse que carrega um enum de tipos de evento** (no AllGenda: `Avaliacao` com `TipoAvaliacao { PROVA, ENTREGA }`).
2. **Política de acesso e papéis** — **Strategy**, declarativa: cada aplicação declara seus papéis e um mapa `Acao → papéis permitidos`; define também o papel de quem cria o grupo, o papel de quem é convidado e o status inicial ao ingressar.
3. **Comportamento do assistente** — a aplicação fornece as **instruções** (persona) e, opcionalmente, **seleciona/restringe o contexto**. O padrão é o grupo inteiro. O núcleo **sempre** filtra a visibilidade **antes** de chamar a aplicação: anotação privada **nunca** chega ao assistente, nem a do próprio autor.

### 4.3 Outras decisões combinadas
- **Rotas do AllGenda mantidas**: `/api/disciplinas`, `/api/aulas`, `/api/avaliacoes` e todas as demais rotas atuais. O front não muda.
- **Sem migração de dados.** Os dados atuais são de teste.
- **Um módulo Maven só**, dois pacotes raiz (`framework` e `aplicacao.allgenda`).
- **`Papel` é definido pela aplicação** (valor, não enum fixo); no banco continuam os mesmos textos `DONO`/`MEMBRO`.
- **Ponto de entrada da aplicação**: uma classe `DefinicaoDaAplicacao` por aplicação (no AllGenda: `DefinicaoAllGenda`).
- **`CRIAR_ANOTACAO` também cobre** tags e imagens do encontro.
- **Entrega só com o AllGenda instanciado.** Mesa de RPG e Clube do livro ficam fora.

### 4.4 Detalhes de projeto 🔶 (decididos neste roteiro; ajuste se houver bom motivo e anote no PR)
- A **camada web** (controllers, DTOs de entrada/saída, mappers) fica na **aplicação**, porque as rotas e o JSON usam o vocabulário do AllGenda (`aulaId`, `disciplinaNome`...). O **núcleo expõe services** que recebem e devolvem **entidades e *records* neutros**, nunca DTOs da aplicação. (Controllers neutros, como `/api/auth` e `/api/usuarios`, podem ficar no núcleo.)
- Estratégia de herança JPA: `JOINED` nas três hierarquias.
- Mensagens de erro do núcleo usam vocabulário neutro ("grupo", "encontro"). Os **status HTTP não mudam**.

---

## 5. Arquitetura-alvo

### 5.1 Pacotes

```
com.allgenda
├── AllgendaApplication.java            (fica na raiz: o scan cobre os dois pacotes)
├── framework
│   ├── model        Grupo, Encontro, Evento, Anotacao, ImagemAnotacao, ImagemEncontro, Tag,
│   │                Participacao, Usuario, Recorrencia, DiaSemana, FrequenciaRecorrencia,
│   │                StatusParticipacao, VisibilidadeAnotacao
│   ├── acesso       Papel, PapelConverter, Acao, PoliticaDeAcesso, PoliticaDeAcessoDeclarativa, AcessoAoGrupo
│   ├── extensao     DefinicaoDaAplicacao, RegistroDeTipos, FabricaDeGrupo, FabricaDeEncontro,
│   │                FabricaDeEvento, DadosDoGrupo, DadosDoEncontro, DadosDoEvento
│   ├── assistente   AssistenteService (+Impl), ComportamentoDoAssistente, ContextoDoAssistente,
│   │                ModeloDeLinguagem, ModeloAnthropic, PedidoAoModelo, FalhaDoModeloException,
│   │                LimitePerguntasAssistente
│   ├── repository   (todos os repositórios, com os nomes novos)
│   ├── service      (+ impl) Grupo, Encontro, Evento, Anotacao, Convite, Participante, Recorrencia, Tag,
│   │                ImagemEncontro, ImagemAnotacao, Usuario, Auth, Armazenamento(+Local),
│   │                RecebimentoImagem, ReducaoDeImagem, RemocaoArquivos, UsuarioAutenticado, ImagemArquivo
│   ├── exception    exceções genéricas + GlobalExceptionHandler
│   └── config       SecurityConfig, CorsConfig, UsuarioTesteInitializer, configuração que liga a DefinicaoDaAplicacao
└── aplicacao.allgenda
    ├── model        Disciplina, Aula, Avaliacao, TipoAvaliacao, PapelAllGenda (constantes DONO e MEMBRO)
    ├── DefinicaoAllGenda.java, AssistenteTutor.java, FabricaDeDisciplina/Aula/Avaliacao, PoliticaAllGenda (ou configuração equivalente)
    ├── controller   todos os controllers de rotas do AllGenda (disciplinas, aulas, avaliacoes, anotacoes, tags,
    │                convites, participantes, recorrencia, imagens, assistente)
    ├── dto          request/ e response/ do AllGenda (JSON atual, inalterado)
    └── mapper       mappers do AllGenda (entidade → DTO)
```

A divisão exata de subpacotes do núcleo é **detalhe seu**; as regras duras são: (1) `framework` nunca importa `aplicacao`; (2) a aplicação só conhece o núcleo pelas suas interfaces/extensões e pelos services.

### 5.2 Modelo do núcleo

- **`Grupo`** (abstrata, `@Entity`, `@Inheritance(JOINED)`): `id` (UUID), `nome`, `descricao` (até 500), `encontros`, `eventos`, `tags` (cascade ALL + orphanRemoval, como hoje em `Disciplina`), `participacoes` (associação independente, **sem cascata**: persistir e remover participações é explícito no service, como hoje). Método abstrato `String tipo()`. Preserve `addParticipacao(...)` (mantém os dois lados coerentes) e demais métodos de `Disciplina`.
- **`Encontro`** (abstrata, `JOINED`): `id`, `data`, `horario`, `topico`, `cancelada` (boolean, default false), `grupo` (ManyToOne, obrigatório), `anotacoes`, `imagens` (cascade ALL + orphanRemoval). `String tipo()` abstrato.
- **`Evento`** (abstrata, `JOINED`): `id`, `descricao` (até 500), `data`, `grupo`. `String tipo()` (chave do registro) e `String subtipo()` (nome do tipo de evento; no AllGenda, o nome do enum). 🔶 `subtipo()` é detalhe deste roteiro.
- **`Anotacao`**: igual à atual; o campo `aula` vira `encontro`. **Não** é especializada.
- **`ImagemEncontro`**: é a atual `ImagemAula` com outro nome (galeria avulsa do encontro: legenda, autor, posição, bytes no `ArmazenamentoService`). Rota do AllGenda continua `/api/aulas/{aulaId}/imagens`.
- **`Tag`**: `disciplina` vira `grupo`; unicidade `(grupo_id, nome)`.
- **`Participacao`**: `disciplina` vira `grupo`; `papel` vira `Papel` (abaixo); `status` continua `StatusParticipacao { PENDENTE, ACEITO, RECUSADO }`; unicidade `(usuario_id, grupo_id)`.
- **`Recorrencia`**: `disciplina` vira `grupo` (continua 0..1 por grupo, `unique`).
- Iguais: `Usuario`, `DiaSemana`, `FrequenciaRecorrencia { SEMANAL, QUINZENAL }`, `VisibilidadeAnotacao { GRUPO, PRIVADA }`, `ImagemAnotacao`.
- Tabelas (detalhe seu, dados são descartáveis): sugestão `grupos`/`encontros`/`eventos` (núcleo) e `disciplinas`/`aulas`/`avaliacoes` (subclasses), `imagens_encontro`, colunas `grupo_id`, `encontro_id`. Atenção: a coluna `posicao` de `ImagemEncontro` e `cancelada` de `Encontro` têm `columnDefinition ... default`; mantenha.

### 5.3 Ponto variável 1: tipos (herança + Factory com registro)

```java
public interface FabricaDeGrupo   { String tipo(); Grupo   criar(DadosDoGrupo dados); }
public interface FabricaDeEncontro{ String tipo(); Encontro criar(DadosDoEncontro dados, Grupo grupo); }
public interface FabricaDeEvento  { String tipo(); Evento  criar(DadosDoEvento dados, Grupo grupo); }

public class RegistroDeTipos {                      // @Component
    void registrar(FabricaDeGrupo f); void registrar(FabricaDeEncontro f); void registrar(FabricaDeEvento f);
    FabricaDeGrupo fabricaDeGrupo(String tipo);     // tipo desconhecido => RequisicaoInvalidaException (400)
    FabricaDeEncontro fabricaDeEncontro(String tipo);
    FabricaDeEvento fabricaDeEvento(String tipo);
}
public record DadosDoGrupo(String nome, String descricao) {}
public record DadosDoEncontro(UUID grupoId, LocalDate data, String horario, String topico) {}
public record DadosDoEvento(UUID grupoId, String descricao, LocalDate data, String subtipo) {}
```

- `GrupoService.criar(String tipo, DadosDoGrupo)`, `EncontroService.criar(String tipo, DadosDoEncontro)`, `EventoService.criar(String tipo, DadosDoEvento)`: o núcleo valida o que é comum (nome obrigatório, descrição ≤ 500, data obrigatória etc., **as mesmas validações de hoje**), busca a fábrica no registro, cria a entidade, persiste e aplica as regras fixas (ex.: criar grupo cria a `Participacao` do criador, ver 5.4).
- **Validação específica do tipo fica na fábrica.** Ex.: `FabricaDeAvaliacao` converte `subtipo` em `TipoAvaliacao` e, se for nulo/inválido, lança `RequisicaoInvalidaException("O tipo da avaliação é obrigatório: PROVA ou ENTREGA.")` (400), como hoje.
- O AllGenda registra tipos: `"DISCIPLINA"`, `"AULA"`, `"AVALIACAO"`.
- Inversão de controle: o núcleo só conhece `Grupo`/`Encontro`/`Evento` abstratos e o registro.

### 5.4 Ponto variável 2: política de acesso e papéis (Strategy declarativa)

```java
public record Papel(String codigo) {}               // valor definido pela aplicação (igualdade por codigo)
@Converter(autoApply = true)
public class PapelConverter implements AttributeConverter<Papel, String> { ... }   // banco guarda o texto: "DONO", "MEMBRO"

public enum Acao { VER_CONTEUDO, CRIAR_ENCONTRO, EDITAR_ENCONTRO, CANCELAR_ENCONTRO, EXCLUIR_ENCONTRO,
    CRIAR_EVENTO, CRIAR_ANOTACAO, CONVIDAR, REMOVER_MEMBRO, EDITAR_GRUPO, EXCLUIR_GRUPO,
    DEFINIR_RECORRENCIA, USAR_ASSISTENTE }

public interface PoliticaDeAcesso {
    Set<Papel> papeis();
    Papel papelDoCriador();                 // papel de quem cria o grupo (participação já ACEITO)
    Papel papelDoConvidado();               // papel de quem é convidado
    boolean permite(Papel papel, Acao acao);
    StatusParticipacao statusAoIngressar(); // AllGenda: PENDENTE (ingresso por convite)
}
public class PoliticaDeAcessoDeclarativa implements PoliticaDeAcesso { /* Map<Acao, Set<Papel>> */ }

@Component
public class AcessoAoGrupo {                // substitui AcessoDisciplina
    void exigirParticipanteAceito(UUID grupoId);                       // + sobrecarga com mensagem
    void exigir(Acao acao, UUID grupoId);                              // + sobrecarga com mensagem
}
```

- `AcessoAoGrupo.exigir`: busca a participação **ACEITO** da pessoa logada no grupo; sem participação aceita → `AcessoNegadoException` (403); com participação mas `!politica.permite(papel, acao)` → 403. Mantém as **sobrecargas com mensagem** (o 403 continua dizendo o que a pessoa tentou).
- `PoliticaDeAcessoDeclarativa`: construção declarativa (ex.: `.papelDoCriador(DONO).papelDoConvidado(MEMBRO).statusAoIngressar(PENDENTE).papel(DONO, Acao.values()).papel(MEMBRO, VER_CONTEUDO, CRIAR_ENCONTRO, ...)`). Valide na construção que criador/convidado estão entre os papéis declarados. Papel não declarado ou ação não listada ⇒ não permite.
- **Papel no JSON.** `Papel` é record; para o JSON do AllGenda não mudar, os DTOs da aplicação passam a ter `String papel` (preenchido com `papel.codigo()`), no lugar de `PapelParticipacao papel` (`ConviteResponseDTO`, `ParticipanteResponseDTO`). Resultado no JSON: `"DONO"`/`"MEMBRO"`, igual a hoje.
- **Consulta por papel.** `ParticipacaoRepository.findByUsuarioIdAndPapelAndStatus(UUID, PapelParticipacao, StatusParticipacao)` passa a usar `Papel`. Se a consulta derivada não funcionar bem com o conversor, resolva com `@Query` ou filtrando no service (detalhe seu).
- **Regras que NÃO são política (ficam fixas no núcleo):** só o autor edita, exclui ou muda a visibilidade da própria anotação; anotação privada só o autor vê (404 para os outros, fora das listagens e do assistente; o dono também não vê); só participante aceito acessa o grupo; **o criador do grupo não pode ser removido** (hoje: "não remover o DONO", passa a comparar com `politica.papelDoCriador()`); só o convidado responde ao próprio convite; convite já respondido ⇒ 409.

**Matriz do AllGenda (igual ao comportamento atual):**

| Ação | DONO | MEMBRO |
|---|:-:|:-:|
| VER_CONTEUDO | ✔ | ✔ |
| CRIAR_ENCONTRO | ✔ | ✔ |
| EDITAR_ENCONTRO, CANCELAR_ENCONTRO, EXCLUIR_ENCONTRO | ✔ | |
| CRIAR_EVENTO | ✔ | ✔ |
| CRIAR_ANOTACAO (também tags e imagens do encontro) | ✔ | ✔ |
| CONVIDAR, REMOVER_MEMBRO | ✔ | |
| EDITAR_GRUPO, EXCLUIR_GRUPO, DEFINIR_RECORRENCIA | ✔ | |
| USAR_ASSISTENTE | ✔ | ✔ |

**Como mapear as chamadas atuais a `Acao`** (confira lendo cada service antes de trocar; o resultado deve ser: quem passava hoje continua passando e quem era barrado continua barrado):

| Hoje | Passa a ser |
|---|---|
| `AulaServiceImpl.cadastrar` (`exigirParticipanteAceito`) | `CRIAR_ENCONTRO` |
| `AulaServiceImpl.listarPorDisciplina` / `buscarPorId` | `VER_CONTEUDO` |
| `AulaServiceImpl.atualizar` (`exigirDono`) | `EDITAR_ENCONTRO`; e **também** `CANCELAR_ENCONTRO` quando o campo `cancelada` vier informado (o PATCH atual cobre as duas coisas) |
| `AulaServiceImpl.excluir` (`exigirDono`) | `EXCLUIR_ENCONTRO` |
| `AvaliacaoServiceImpl.cadastrar` / `listar` | `CRIAR_EVENTO` / `VER_CONTEUDO` |
| `DisciplinaServiceImpl.buscarPorId` | `VER_CONTEUDO` |
| `DisciplinaServiceImpl.atualizar` (`exigirDono`) | `EDITAR_GRUPO` |
| `DisciplinaServiceImpl.excluir` (`exigirDono`) | `EXCLUIR_GRUPO` |
| `ConviteServiceImpl` enviar convite (`exigirDono`) | `CONVIDAR` |
| `ConviteServiceImpl` listar/aceitar/recusar | sem `Acao` (regra fixa: é o próprio convidado) |
| `ParticipanteServiceImpl.listar` | `VER_CONTEUDO` |
| `ParticipanteServiceImpl.remover` (`exigirDono`) | `REMOVER_MEMBRO` (+ regra fixa: criador não removível) |
| `RecorrenciaServiceImpl.obter` | `VER_CONTEUDO` |
| `RecorrenciaServiceImpl.definir` / `remover` (`exigirDono`) | `DEFINIR_RECORRENCIA` |
| `AnotacaoServiceImpl` criar, editar, excluir, mudar visibilidade, tags da anotação | `CRIAR_ANOTACAO` (+ regras fixas do autor) |
| `AnotacaoServiceImpl` listar/ler (inclui colegas) | `VER_CONTEUDO` (+ filtro fixo de visibilidade) |
| `TagServiceImpl` criar/renomear/excluir | `CRIAR_ANOTACAO`; listar ⇒ `VER_CONTEUDO` |
| `ImagemAnotacaoServiceImpl` enviar/excluir | `CRIAR_ANOTACAO`; ver ⇒ `VER_CONTEUDO` |
| `ImagemAulaServiceImpl` (→ `ImagemEncontroServiceImpl`) adicionar/reordenar/legenda/remover | `CRIAR_ANOTACAO`; listar/baixar ⇒ `VER_CONTEUDO` |
| `AssistenteServiceImpl.preparar` | `USAR_ASSISTENTE` |

### 5.5 Ponto variável 3: comportamento do assistente

```java
public interface ComportamentoDoAssistente {
    String instrucoes(Grupo grupo, Encontro destaque);                    // persona; destaque pode ser nulo
    default ContextoDoAssistente selecionarContexto(ContextoDoAssistente contexto) { return contexto; }
}
public record ContextoDoAssistente(List<Anotacao> anotacoes, List<ImagemEncontro> imagens,
                                   Encontro destaque, Foco foco) {}
```

- `AssistenteService` (núcleo) expõe `prepararDoGrupo(UUID grupoId, pergunta)`, `prepararDoEncontro(UUID encontroId, pergunta)` e `responder(PedidoAoModelo, Consumer<String>)`. Os nomes das rotas do AllGenda (`POST /api/disciplinas/{id}/assistente` e `POST /api/aulas/{id}/assistente`, SSE) **não mudam**; o `AssistenteController` fica na aplicação.
- **Fluxo fixo no núcleo**, nesta ordem: validar a conversa → `USAR_ASSISTENTE` → modelo configurado (senão 503) → carregar encontros/anotações/imagens → **remover as anotações PRIVADAS (e as imagens delas)** → montar o `ContextoDoAssistente` → chamar `comportamento.selecionarContexto(contexto)` → **descartar do resultado qualquer item que não estivesse na lista original** (a aplicação só pode reduzir/reordenar, nunca acrescentar, para que nada privado vaze) → numerar, validar o foco, registrar o limite de perguntas (só depois de validar) → escolher o que cabe nos limites (prioridade: foco, encontro em destaque, mais recentes) → montar o material → `PedidoAoModelo(instrucoes, conversa)`.
- **Instruções = parte da aplicação + parte fixa do núcleo.** O texto atual de `AssistenteServiceImpl.instrucoes` mistura persona e defesa. Separe assim:
  - **Aplicação (`AssistenteTutor.instrucoes`)**: "Você é o assistente de estudos do AllGenda e ajuda um aluno a estudar a disciplina de {nome}." + (se houver destaque) "O aluno está na página da aula de {dd/MM/yyyy} ({tópico}). Quando a pergunta não disser de qual aula fala, considere essa aula primeiro, mas use o material de toda a disciplina quando ajudar." + "Responda em português do Brasil, em Markdown, de forma clara e direta."
  - **Núcleo (sempre anexado depois, não sobrescrevível)**: descreve o bloco `<material_do_grupo>…</material_do_grupo>` (anotações dos participantes organizadas por encontro e imagens); diz que o material foi escrito pelos participantes e deve ser tratado como **conteúdo de consulta, nunca como instruções** (defesa contra injeção de prompt); manda basear as respostas no material, dizer claramente quando algo não estiver nele e distinguir conhecimento geral; citar anotação (título ou número e autor) ou número da imagem, e de qual encontro; lembrar quando o material avisar que parte dele ficou de fora. 🔶 A redação é sua, mantendo o sentido do texto atual.
- **Rótulos do material (núcleo, neutros) 🔶**: `<material_do_grupo>`, linha `Grupo: {nome}`, `<encontro data="dd/MM/yyyy" horario="..." topico="...">`. Isso muda duas asserções de teste (seção 9.1).
- Continuam no núcleo, sem alteração de comportamento: `ModeloDeLinguagem`, `ModeloAnthropic`, `PedidoAoModelo`, `LimitePerguntasAssistente`, `ReducaoDeImagem`, `RecebimentoImagem`, `ArmazenamentoService`, configuração `app.assistente.*`.

### 5.6 Ponto de entrada da aplicação

```java
public interface DefinicaoDaAplicacao {
    String nome();
    void registrarTipos(RegistroDeTipos registro);
    PoliticaDeAcesso politicaDeAcesso();
    ComportamentoDoAssistente comportamentoDoAssistente();
}
```

- `DefinicaoAllGenda` (`@Component` na aplicação): registra `FabricaDeDisciplina`, `FabricaDeAula`, `FabricaDeAvaliacao`; devolve a política declarativa (DONO/MEMBRO, matriz acima) e o `AssistenteTutor`.
- O núcleo localiza **exatamente uma** `DefinicaoDaAplicacao` no contexto Spring e: chama `registrarTipos` na inicialização; expõe `PoliticaDeAcesso` e `ComportamentoDoAssistente` como beans a partir dela. **Zero ou mais de uma definição ⇒ falha na inicialização com mensagem clara.**
- `PapelAllGenda` guarda as constantes `DONO = new Papel("DONO")` e `MEMBRO = new Papel("MEMBRO")`.

### 5.7 Camada web e DTOs (decisão 🔶 da seção 4.4)
- Os *services* do núcleo **não** recebem nem devolvem DTOs da aplicação. Recebem parâmetros simples/records neutros (ex.: `DadosDoEncontro`, `NovaAnotacao`, `AtualizacaoDeAnotacao`, `AtualizacaoDeEncontro(data, horario, topico, cancelada)`, `PerguntaAoAssistente` (+ `Mensagem`, `Foco`)) e devolvem entidades (ou records neutros).
- Os controllers do AllGenda convertem DTO → record neutro, chamam o service e convertem entidade → DTO com os mappers existentes, que passam a ficar na aplicação. Contagens que hoje saem do service/mapper (ex.: `anotacoes` em `AulaResponseDTO` e `ParticipanteResponseDTO`, que **só contam as de visibilidade GRUPO**) continuam com o mesmo resultado.
- O Spring Boot mantém *open-in-view* ligado por padrão (não há `spring.jpa.open-in-view` no `application.properties`); **não desligue**. Se aparecer `LazyInitializationException` no mapeamento fora do service, resolva com um método transacional de leitura que mapeie (detalhe seu), sem mudar o resultado.
- Validações que hoje usam DTOs (campos obrigatórios, tamanhos, mensagens) **migram junto**: ficam no service quando são regra de negócio (já estão lá); nada some.

---

## 6. Regras de negócio que NÃO podem mudar (invariantes)

Todas já cobertas por testes de integração; use-as como checklist de revisão:

- **Participação**: quem cria o grupo vira dono com status ACEITO. Convite cria participação com papel de convidado e status PENDENTE; só o convidado aceita/recusa; resposta repetida ⇒ 409. Só participante **ACEITO** acessa o grupo (pendente/recusado/estranho ⇒ 403).
- **Dono**: único que edita a descrição, remove membros, edita/cancela/exclui encontros, convida, define recorrência, exclui o grupo. O dono **não** é removível.
- **Anotações**: visibilidade `GRUPO` (padrão) ou `PRIVADA`. Privada: só o autor vê; para os outros é **404**, não aparece em listagens, nem em contagens, **nem no assistente**. Só o autor edita/exclui/muda visibilidade. Anotações de grupo de um membro removido **permanecem**.
- **Encontro**: cancelar mantém o encontro e as anotações; excluir remove anotações (inclusive privadas), imagens (registros **e** arquivos do disco, via `RemocaoArquivos.removerAoConfirmar`, só depois do commit). Excluir o grupo remove tudo em cascata, participações e recorrência incluídas, e os arquivos.
- **Recorrência**: 0..1 por grupo; semanal ou quinzenal; "quinzenal" = semanas pares contadas a partir da semana (domingo) de `dataInicio`; só o dono define/remove; qualquer participante aceito lê.
- **Tags**: pertencem ao grupo; nome não repete dentro do grupo.
- **Assistente**: limite de perguntas por hora por usuário (conta só depois de validar); 503 se não configurado; material limitado por `app.assistente.maximo-imagens` e `maximo-caracteres`; foco precisa pertencer ao grupo.
- **Erros**: mesmos status (400, 401, 403, 404, 409, 413, 429, 503). `GlobalExceptionHandler` continua traduzindo as exceções.

---

## 7. Inventário v1 → framework (arquivo a arquivo)

| Versão 1 | Destino |
|---|---|
| `model/Disciplina` | `framework/model/Grupo` (abstrata) + `aplicacao/allgenda/model/Disciplina extends Grupo` |
| `model/Aula` | `framework/model/Encontro` + `aplicacao/.../Aula extends Encontro` |
| `model/Avaliacao`, `TipoAvaliacao` | `framework/model/Evento` + `aplicacao/.../Avaliacao extends Evento`; `TipoAvaliacao` fica na aplicação |
| `model/Anotacao` | núcleo; campo `aula` → `encontro` |
| `model/ImagemAula` | `framework/model/ImagemEncontro` |
| `model/ImagemAnotacao`, `Tag`, `Usuario`, `Recorrencia`, `DiaSemana`, `FrequenciaRecorrencia`, `StatusParticipacao`, `VisibilidadeAnotacao` | núcleo; `Tag`/`Recorrencia`/`Participacao`: `disciplina` → `grupo` |
| `model/PapelParticipacao` | removido; `framework/acesso/Papel` + `aplicacao/.../PapelAllGenda` |
| `service/AcessoDisciplina` | `framework/acesso/AcessoAoGrupo` |
| `DisciplinaService(+Impl)` | `GrupoService(+Impl)` (cria por tipo) |
| `AulaService(+Impl)` | `EncontroService(+Impl)` (criar por tipo; editar, cancelar, excluir) |
| `AvaliacaoService(+Impl)` | `EventoService(+Impl)` |
| `ImagemAulaService(+Impl)` | `ImagemEncontroService(+Impl)` |
| `AnotacaoService`, `ConviteService`, `ParticipanteService`, `RecorrenciaService`, `TagService`, `ImagemAnotacaoService`, `UsuarioService`, `AuthService` | núcleo, mesmos nomes; trocam `AcessoDisciplina` por `AcessoAoGrupo`, recebem/devolvem entidades e records neutros |
| `AssistenteService(+Impl)` | `framework/assistente/AssistenteService(+Impl)`; instruções da persona saem para `AssistenteTutor` |
| `ModeloDeLinguagem`, `ModeloAnthropic`, `PedidoAoModelo`, `LimitePerguntasAssistente`, `FalhaDoModeloException`, `ArmazenamentoService(+Local)`, `ReducaoDeImagem`, `RecebimentoImagem`, `RemocaoArquivos`, `UsuarioAutenticado`, `ImagemArquivo` | núcleo, sem mudança de lógica |
| `repository/*` | núcleo; renomear (`DisciplinaRepository` → `GrupoRepository`, `AulaRepository` → `EncontroRepository`, `AvaliacaoRepository` → `EventoRepository`, `ImagemAulaRepository` → `ImagemEncontroRepository`) e os métodos derivados (`findByDisciplinaId` → `findByGrupoId` etc.) |
| `controller/*`, `dto/*`, `mapper/*` | `aplicacao/allgenda/{controller,dto,mapper}` (neutros como auth/usuário podem ficar no núcleo) |
| `exception/*` | exceções genéricas no núcleo (`DisciplinaNaoEncontradaException` → `GrupoNaoEncontradoException`, `AulaNaoEncontradaException` → `EncontroNaoEncontradoException`; avalie `AvaliacaoNaoEncontrada` se existir) |
| `config/MigracaoTagsPorDisciplina` (+ teste) | **removida** (era migração de dados; sem uso aqui) |
| `config/SecurityConfig`, `CorsConfig`, `UsuarioTesteInitializer` | núcleo; ajustar rotas/permissões se o pacote mudar (as rotas **não** mudam) |

---

## 8. Plano em fases (cada fase termina com `mise run test` verde e um commit)

**Fase 0: preparação.** Crie a branch (ex.: `refatoracao-framework`). Rode `mise run test` e anote o total de testes. Leia os services e testes que serão tocados. Nada de mudanças ainda.

**Fase 1: desacoplar os services dos DTOs (ainda nos pacotes atuais).** Para cada service que hoje recebe/devolve DTO (`Anotacao`, `Aula`, `Avaliacao`, `Convite`, `Disciplina`, `ImagemAnotacao`, `ImagemAula`, `Participante`, `Recorrencia`, `Tag`; `Usuario`/`Auth` podem manter DTOs neutros): passar a receber parâmetros/records neutros e devolver entidades; mover a chamada ao mapper e a conversão de DTO de entrada para o controller. Fazer **um service por vez**, rodando os testes. Ajustar os testes unitários dos services (`AnotacaoServiceImplTest`, `DisciplinaServiceImplTest`, `TagServiceImplTest`, `ImagemAnotacaoServiceImplTest`) para o novo tipo de retorno; os testes de integração (HTTP) **não mudam**.

**Fase 2: hierarquia de entidades e renomeações.** Criar `Grupo`/`Encontro`/`Evento` abstratos (`JOINED`) e fazer `Disciplina`/`Aula`/`Avaliacao` herdarem deles (por enquanto no mesmo pacote `model`; só sobem os campos comuns). Renomear `aula`→`encontro`, `disciplina`→`grupo` nos campos e nos repositórios/métodos derivados; `ImagemAula` → `ImagemEncontro`. Remover `MigracaoTagsPorDisciplina` e `MigracaoTagsPorDisciplinaTest`. Ajustar os *setups* dos testes de integração (que instanciam entidades/repositórios). Garantir cascatas e `orphanRemoval` iguais às atuais.

**Fase 3: papéis, ações e política.** Criar `Papel`, `PapelConverter`, `Acao`, `PoliticaDeAcesso`, `PoliticaDeAcessoDeclarativa`, `AcessoAoGrupo`; criar `PapelAllGenda` e a política do AllGenda. Trocar `AcessoDisciplina` por `AcessoAoGrupo` em **todos** os services seguindo a tabela da seção 5.4 e remover `PapelParticipacao`. Trocar `PapelParticipacao` por `String papel` nos DTOs. Migrar `AcessoDisciplinaTest` para `AcessoAoGrupoTest` (mesmos cenários + política). Rodar os testes.

**Fase 4: tipos, registro e definição da aplicação.** Criar `FabricaDe*`, `RegistroDeTipos`, `DadosDo*`, `DefinicaoDaAplicacao`, `DefinicaoAllGenda` e as três fábricas; passar `GrupoService`, `EncontroService` e `EventoService` a criar por tipo; ligar a definição ao Spring (seção 5.6). Os controllers do AllGenda chamam `criar("DISCIPLINA", ...)` etc.

**Fase 5: assistente.** Criar `ComportamentoDoAssistente`, `ContextoDoAssistente`, `AssistenteTutor`; separar instruções (seção 5.5); aplicar o filtro de privacidade **antes** de `selecionarContexto` e a regra de "só reduzir/reordenar"; `prepararDoGrupo`/`prepararDoEncontro`; rótulos neutros do material. Ajustar `AssistenteServiceImplTest` e as duas asserções da seção 9.1.

**Fase 6: pacotes.** Mover para `com.allgenda.framework` e `com.allgenda.aplicacao.allgenda` conforme a seção 5.1 (`git mv` + ajuste de `package`/`import`; mover os testes junto). Garantir que o `@SpringBootApplication` em `com.allgenda` continua varrendo os dois pacotes. Adicionar o teste de arquitetura (9.3). Rodar tudo.

**Fase 7: testes novos, documentação e conferência final.** Escrever os testes da seção 9.3. Atualizar `AGENTS.md` (estrutura: controllers/services agora em `framework` e `aplicacao.allgenda`), `README.md` (seção de estrutura) e `docs/framework/` (se algo divergiu dos diagramas, corrigir os diagramas e o documento, e registrar no PR). Rodar a suíte completa, comparar o total de testes com o da Fase 0 (deve ser igual ou maior) e conferir a lista "Pronto quando" da seção 2.

---

## 9. Testes

### 9.1 O que pode mudar nos testes existentes
- **Setups** que instanciam `Disciplina`/`Aula`/`Avaliacao`, usam `PapelParticipacao` ou repositórios renomeados: ajustar para as novas classes (`Disciplina` agora estende `Grupo`; papéis via `PapelAllGenda`).
- **Nomes** de classes/métodos renomeados.
- **Duas asserções de texto** do assistente, por causa dos rótulos neutros (decisão 🔶 da seção 5.5): `AssistenteIntegrationTest` (hoje procura `<aula data="20/10/2026"` e `<aula data="22/10/2026"`) e `AssistenteServiceImplTest` (hoje termina em `</material_da_disciplina>`) passam a usar `<encontro data=...` e `</material_do_grupo>`.
- Nada mais. Em especial, **todas as chamadas HTTP, status e campos de JSON dos testes de integração ficam como estão.** Se algum teste só passar mudando uma asserção de comportamento, pare e pergunte.

### 9.2 Testes que existem e devem continuar verdes
Todos os de `allgenda/src/test`, entre eles: `AuthIntegrationTest`, `AvaliacaoIntegrationTest`, `ConviteIntegrationTest`, `ColaboracaoAceiteIntegrationTest`, `DisciplinasDoUsuarioIntegrationTest`, `ExclusaoDisciplinaIntegrationTest`, `EdicaoAulaIntegrationTest`, `ImagemAulaIntegrationTest`, `ImagemAnotacaoIntegrationTest`, `EdicaoAnotacaoIntegrationTest`, `ExclusaoAnotacaoIntegrationTest`, `AnotacaoPrivadaIntegrationTest`, `AnotacaoColegaIntegrationTest`, `VisibilidadeAnotacoesIntegrationTest`, `ParticipacaoDonoIntegrationTest`, `ParticipanteIntegrationTest`, `RecorrenciaIntegrationTest`, `TagIntegrationTest`, `AssistenteIntegrationTest`, `UsuarioTesteIntegrationTest`, `AllgendaApplicationTests`, e os unitários de services/config.

### 9.3 Testes novos (simples, rápidos)
1. **`PoliticaDeAcessoDeclarativaTest`**: com papéis *diferentes* dos do AllGenda (ex.: `ORGANIZADOR`/`PARTICIPANTE`) verifica `permite`, `papelDoCriador`, `papelDoConvidado`, `statusAoIngressar`, e que a construção rejeita criador/convidado não declarado. Prova que o ponto variável 2 não depende do AllGenda.
2. **`RegistroDeTiposTest`**: registra fábricas de teste e cria por nome; tipo desconhecido ⇒ `RequisicaoInvalidaException`.
3. **`AssistentePrivacidadeTest`** (ou dentro do teste do assistente): com um `ComportamentoDoAssistente` de teste que tenta **acrescentar** uma anotação privada ao contexto, o pedido ao modelo **não** contém essa anotação.
4. **`DefinicaoDaAplicacao`**: o contexto sobe com uma definição; sem nenhuma (ou com duas) a inicialização falha com mensagem clara (teste de configuração simples, pode ser unitário).
5. **`NucleoNaoDependeDaAplicacaoTest`**: percorre `src/main/java/com/allgenda/framework` e falha se algum arquivo contiver `import com.allgenda.aplicacao`.

---

## 10. Pontos de atenção e armadilhas

- **Herança JPA e cascatas.** Com `JOINED`, `Grupo.encontros` é `List<Encontro>`; o repositório de aulas passa a ser de `Encontro` (ou de `Aula` se precisar de campos da subclasse). Cuidado com `findByGrupoId`/`orderBy` derivados; confira com os testes.
- **Conversor de `Papel` em consultas derivadas** (seção 5.4).
- **`Evento`**: `Avaliacao.tipo` (enum) é campo da subclasse; `subtipo()` devolve `tipo.name()`. `TipoAvaliacao` fica fora do núcleo.
- **Nome `Papel` duplicado**: `PerguntaAssistenteDTO` tem um `Papel` aninhado (`USUARIO`/`ASSISTENTE`) e `PedidoAoModelo` também. Não confundir com o novo `Papel` de acesso; use *import* explícito ou renomeie os aninhados com cuidado (detalhe seu), mantendo o JSON do assistente.
- **JSON de DTOs** (`papel` como texto; todos os campos `disciplinaId`, `aulaId` etc. iguais).
- **Segurança.** `SecurityConfig` protege rotas por caminho; como as rotas não mudam, nada muda.
- **Arquivos no disco.** Excluir encontro/grupo continua removendo os arquivos só depois do commit.
- **Colunas com `default`.** `ddl-auto=update` no Postgres local: o banco atual **vai divergir** do novo modelo (tabelas novas). Para rodar localmente o usuário precisará recriar o banco (`mise run db:reset`); **não rode você sem perguntar**. Os testes usam H2 `create-drop` e não são afetados.
- **Não** crie classes que a seção 5 não pede só "por completude". Poucas classes bem nomeadas valem mais do que muitas.

---

## 11. Fora do escopo (adiado, não implementar)

Presença em sessões; configurar o assistente pelo mestre/organizador; ingresso por código; periodicidade mensal (hoje só semanal e quinzenal, e "não periódico" = não ter recorrência); notificações; radar de carga/desempenho (calculado fora do framework); aplicações Mesa de RPG e Clube do livro; qualquer mudança no front-end; mudar rotas/JSON.

---

## 12. Entregáveis do PR

1. Código do framework + AllGenda instanciado, suíte verde.
2. `docs/framework/diagrama-de-classes.md` e `img/` atualizados se algo divergir do implementado.
3. Descrição do PR com: (a) mapa "classe da versão 1 → classe do framework" (seção 7), (b) decisões 🔶 tomadas e desvios, (c) total de testes antes/depois, (d) dúvidas.
4. `AGENTS.md` e `README.md` com a nova estrutura.

---

## 13. Resumo de uma linha por decisão (para conferência rápida)

Domínio: gestão colaborativa de encontros por grupos · Núcleo: Grupo, Encontro, Evento, Anotação (não especializada), Participação, Tag, Usuário/auth, Recorrência, imagens, mecanismo do assistente · Hot spot 1: tipos por herança + Factory com registro (Evento com enum por aplicação) · Hot spot 2: política de acesso declarativa (`Papel` definido pela aplicação, `Acao`, `PoliticaDeAcesso`) · Hot spot 3: comportamento do assistente (instruções + seleção de contexto; privacidade sempre no núcleo) · Caixa-cinza · Uma instância: AllGenda (DONO/MEMBRO; tutor) · Rotas do AllGenda mantidas · Sem migração de dados · Um módulo Maven, pacotes `framework` e `aplicacao.allgenda` · `CRIAR_ANOTACAO` cobre tags e imagens do encontro · `ImagemEncontro` = antiga `ImagemAula` · Adiado: presença, configurar assistente, ingresso por código, periodicidade mensal, notificações, radar de desempenho.
