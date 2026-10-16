# AllGenda

Sistema de agenda acadêmica com anotações contextualizadas (disciplina → aula → anotação),
tags, provas/entregas e colaboração entre colegas por disciplina.

- `allgenda/`: back-end (Spring Boot, Java 25, Maven, PostgreSQL)
- `frontend/`: front-end (Nuxt 4, Vue 3, TypeScript)
- `docs/api-contrato.md`: contrato da API (endpoints, erros e exemplos)
- `mise.toml` e `compose.yaml`: ferramentas, variáveis de ambiente, tarefas e banco de dados

## Como rodar

O projeto usa o [mise](https://mise.jdx.dev) para instalar as ferramentas (Java 25 e Node 24),
definir as variáveis de ambiente e padronizar as tarefas. O banco (PostgreSQL 16) sobe em Docker.

### Início rápido (com Docker)

```bash
./iniciar-allgenda.sh   # instala o mise (se faltar), sobe o banco, instala dependências e compila
mise run dev            # inicia banco, back-end e front-end
```

Se você já tem o mise instalado, o script equivale a:

```bash
mise trust && mise install && mise run setup
```

O back-end fica em `http://localhost:8080` e o front-end em `http://localhost:3000`.

### Sem Docker (PostgreSQL próprio)

1. Tenha o PostgreSQL na porta **5433**, com o banco `allgenda` e o usuário `allgenda_user`
   (senha `alljgc`, ou outra que você informe em `DB_PASSWORD`).
2. Rode o back-end com as variáveis do `mise.toml` ativas, a partir de `allgenda/`:
   ```bash
   cd allgenda
   mise exec -- ./mvnw spring-boot:run
   ```
   Sem o mise, defina `JWT_SECRET` você mesmo antes de rodar (veja "Configuração").
   Em uma IDE, coloque `JWT_SECRET` nas variáveis de ambiente da configuração de execução.
3. Rode o front-end com `npm install` e `npm run dev` dentro de `frontend/`.

> As tarefas `mise run dev` e `mise run backend:dev` sobem o banco com Docker.
> Sem Docker, use o comando do passo 2.

## Tarefas (`mise tasks` lista todas)

| Comando | O que faz |
|---|---|
| `mise run setup` | Sobe o banco, instala o front-end e compila o back-end |
| `mise run dev` | Inicia banco, back-end e front-end |
| `mise run backend:dev` / `frontend:dev` | Inicia só um dos lados |
| `mise run test` | Roda os testes do back-end |
| `mise run build` | Gera o jar do back-end e o build do front-end |
| `mise run db:up` / `db:down` | Sobe / para o PostgreSQL |
| `mise run db:psql` / `db:logs` | Abre o `psql` / acompanha os logs |
| `mise run db:reset` | Para o PostgreSQL e **apaga os dados** (pede confirmação) |

## Configuração

As variáveis ficam em `[env]` no `mise.toml`:

| Variável | Padrão | Observação |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5433`, `allgenda` | Conexão com o banco |
| `DB_USER`, `DB_PASSWORD` | `allgenda_user`, `alljgc` | Credenciais **somente de desenvolvimento** |
| `JWT_SECRET` | valor de desenvolvimento no `mise.toml` | Chave de assinatura dos tokens (mínimo de 32 bytes) |

O `JWT_SECRET` do `mise.toml` serve **apenas para desenvolvimento**. O back-end não tem valor
padrão para ele e não sobe sem uma chave definida, então um ambiente real precisa de uma chave
própria, aleatória e fora do Git.

Para ajustes só na sua máquina, crie o arquivo `mise.local.toml` (ele não vai para o Git):

```toml
[env]
JWT_SECRET = "cole-aqui-um-valor-aleatorio"
NUXT_PUBLIC_API_BASE = "http://localhost:8080"
```

Para gerar um valor aleatório: `openssl rand -base64 48`.

## Autenticação

Quase todos os endpoints exigem um token. Cadastre-se em `POST /api/usuarios`, entre em
`POST /api/auth/login` e envie `Authorization: Bearer <token>` nas demais requisições
(o token vale por 1 hora). Os detalhes estão em `docs/api-contrato.md`.

## Testes

`mise run test` (ou `./mvnw test` dentro de `allgenda/`) usa um banco H2 em memória e uma chave
JWT própria de teste (`allgenda/src/test/resources/application.properties`). Não precisa de
PostgreSQL nem de Docker.

## Problemas comuns

- **`Could not resolve placeholder 'JWT_SECRET'`**: o back-end foi iniciado sem as variáveis do
  mise. Use `mise run backend:dev`, `mise exec -- ./mvnw spring-boot:run`, ou defina `JWT_SECRET`
  no ambiente.
- **Porta 5433 ocupada**: há um PostgreSQL antigo rodando (por exemplo, instalado direto na
  máquina). Pare-o antes de `mise run db:up`, ou mude `DB_PORT` no `mise.local.toml`.
- **Dados antigos do banco**: o banco do Docker começa vazio; os dados de um PostgreSQL
  anterior não são migrados.
- **Windows**: as tarefas do mise usam comandos de shell Unix (`./mvnw`, `$DB_USER`). Use o
  WSL ou o Git Bash. Não foi testado no Windows puro.
