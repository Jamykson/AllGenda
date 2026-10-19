# Guia do repositório AllGenda

## Estrutura do projeto

- `frontend/`: aplicação Nuxt 4 e Vue 3, escrita em TypeScript.
- `allgenda/`: back-end Spring Boot com Java 25, Maven, Spring MVC, Spring Data JPA e PostgreSQL.
- O cliente da API do front-end é configurado em `frontend/app/plugins/axios.ts`.
- Os endpoints do back-end ficam em `allgenda/src/main/java/com/allgenda/controller/`; regras de negócio, persistência, DTOs e modelos ficam nos pacotes de nome correspondente.

## Ferramentas

O `mise.toml` da raiz prepara o ambiente de desenvolvimento com o [mise](https://mise.jdx.dev): fixa as versões das ferramentas (Java 25, Node 24), define as variáveis de ambiente (`DB_*`, `JWT_SECRET`, o usuário de teste `DEV_USUARIO_*` e `API_COM_USUARIO_DE_TESTE`) e declara as tarefas. Rode `mise trust && mise install` uma vez, ou rode `./iniciar-allgenda.sh`, que também instala o mise e roda `mise run setup`. Ajustes locais, como `NUXT_PUBLIC_API_BASE`, vão num `mise.local.toml`, que é ignorado pelo Git.

## Comandos comuns

Rode-os de qualquer lugar do repositório (`mise tasks` lista todos):

- `mise run setup` sobe o banco, instala as dependências do front-end e compila o back-end.
- `mise run dev` sobe o banco, o back-end e o front-end juntos.
- `mise run backend:dev` / `mise run frontend:dev` iniciam só um dos lados.
- `mise run test` roda os testes do back-end. Eles usam um banco H2 em memória, então não precisam de PostgreSQL.
- `mise run build` gera o jar do back-end e o build do front-end.
- `mise run db:up`, `db:down`, `db:psql`, `db:logs` e `db:reset` gerenciam o banco local.

Os comandos de base continuam funcionando diretamente, desde que as ferramentas e as variáveis do mise estejam ativas: `npm run dev` / `npm run build` dentro de `frontend/`, e `./mvnw spring-boot:run` / `./mvnw test` dentro de `allgenda/`.

O PostgreSQL roda em Docker pelo `compose.yaml`, em `localhost:5433`, com um banco chamado `allgenda`. O `application.properties` lê a conexão das variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` definidas no `mise.toml` e, sem elas, usa esses mesmos valores. Os testes do back-end rodam num banco H2 em memória, com uma chave JWT própria (`allgenda/src/test/resources/application.properties`), então não precisam de PostgreSQL. O back-end exige `JWT_SECRET` (ao menos 32 bytes); o `mise.toml` define um valor só para desenvolvimento, que pode ser trocado no `mise.local.toml` ou no ambiente. Ao iniciar, o back-end cria o usuário de teste a partir de `DEV_USUARIO_*`, se ele não existir (`config/UsuarioTesteInitializer.java`). Só no `nuxt dev` e com `API_COM_USUARIO_DE_TESTE=true` (desligado por padrão), o front-end obtém um token com esse usuário para as chamadas à API quando não tem nenhum; nenhuma tela muda, então a tela de login continua aparecendo. O token fica a cargo de `composables/useAuth.ts` e dos interceptors em `plugins/axios.ts`.

## Rodar e testar no navegador

- A tela de login (`frontend/app/pages/index.vue`) é simulada: não chama a API e só aceita `aluno@gmail.com` / `12345678`, que leva a `/agenda`. As páginas não têm proteção de rota, então também é possível abrir `/agenda`, `/disciplinas` ou `/pesquisar` direto.
- Com o padrão `API_COM_USUARIO_DE_TESTE=false`, o login simulado não obtém token, a API responde 401 e as telas mostram erro (por exemplo, "Não foi possível carregar a agenda."). Para testar telas que usam a API, rode `mise set --file mise.local.toml API_COM_USUARIO_DE_TESTE=true` e reinicie o `mise run dev`; desfaça com `mise unset --file mise.local.toml API_COM_USUARIO_DE_TESTE`. Sempre passe `--file mise.local.toml`: sem ele, esses comandos alteram o `mise.toml` compartilhado.
- Todos os clones da máquina usam o mesmo volume do banco (`allgenda_pgdata`), porque o projeto do Compose se chama `allgenda`. Um banco antigo mantém os dados e o usuário de teste anteriores, e o back-end não atualiza um usuário de teste que já existe. Se o login do usuário de teste falhar, `mise run db:reset` recria o banco, mas apaga os dados locais: confirme com o usuário antes.
- Na primeira execução depois de um clone, o Vite otimiza dependências e recarrega a página, o que pode limpar um formulário recém-preenchido. Basta tentar de novo.

## Orientações para mudanças

- Mantenha as mudanças no front-end coerentes com os padrões existentes de Nuxt/Vue (Composition API) e TypeScript.
- Mantenha as responsabilidades do back-end nas camadas existentes: controller, service, repository, DTO, mapper, model e exception.
- Preserve os nomes em português e a linguagem voltada ao usuário onde fizer sentido.
- Prefira mudanças focadas e rode o build do front-end ou os testes do back-end correspondentes ao código alterado.
