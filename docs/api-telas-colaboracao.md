# Telas de colaboração — #48, #49, #50 e #52

- `/cadastro`: formulário de nome, e-mail e senha, validação no cliente e mensagens Problem Detail da API. Usa `POST /api/usuarios`, retorna ao login após confirmar o cadastro. Senha exige pelo menos 8 caracteres e no máximo 72 bytes UTF-8, como no backend.
- `/`: login real com `POST /api/auth/login`. `useAuth` guarda token e dados públicos do usuário em cookies e estado compartilhado. O cliente Axios envia o Bearer nas chamadas protegidas. `401` encerra a sessão e redireciona ao login; erros de credenciais aparecem no formulário. Sair apaga a sessão. Rotas internas exigem token, exceto o atalho explícito de usuário de teste no desenvolvimento.
- `/avaliacoes`: formulário de disciplina, tipo, descrição e data. Lista as avaliações futuras por data crescente e mantém as passadas num histórico separado. Trocar disciplina descarta respostas de consultas antigas. Cadastro usa `POST /api/avaliacoes`; listagem usa `GET /api/avaliacoes?disciplinaId={uuid}`.
- `/convites`: envia pelo e-mail de um usuário cadastrado; lista os convites pendentes do usuário logado e permite aceitar/recusar. Usa os controllers de convites já presentes na main. Somente donos podem enviar; mensagens de falta de permissão, destinatário inexistente e participação já existente vêm da API. Nenhuma operação é simulada.

## API de avaliações

Segue as camadas já usadas para aulas: controller → service (interface + implementação) → repository, com mapper e DTOs.

Requisição de cadastro autenticada:
```json
{"disciplinaId":"33333333-3333-4333-8333-333333333333","tipo":"PROVA","descricao":"Primeira prova","data":"2030-01-09"}
```
Resposta `201`:
```json
{"id":"77777777-7777-4777-8777-777777777777","tipo":"PROVA","descricao":"Primeira prova","data":"2030-01-09","disciplinaId":"33333333-3333-4333-8333-333333333333","disciplinaNome":"PDS"}
```
O tipo aceita `PROVA` ou `ENTREGA`. Todos os campos são obrigatórios; descrição é aparada e limitada a 500 caracteres. Datas passadas são permitidas. Listagem retorna `200` com objetos no mesmo formato, ordenados por data e ID, ou `[]` para disciplina sem avaliações.

Erros: `400` para campos ausentes, enum/UUID/data inválidos ou descrição vazia/longa; `401` para token ausente/inválido; `403` para quem não tem participação aceita na disciplina; `404` para disciplina inexistente. A avaliação referencia a disciplina por FK obrigatória. `Disciplina.avaliacoes` tem cascata (como `aulas` e `tags`): apagar uma disciplina apaga suas avaliações. Isso difere da associação com `participacoes`, que não tem cascata e é gravada e removida explicitamente pelo service. A lista não é embutida no DTO de disciplina.

## Validação

Backend: `mise run test`, com H2 isolado. Frontend: `mise run frontend:build`. Teste manual: cadastrar dois usuários, entrar com o primeiro, criar uma disciplina, cadastrar prova e entrega com datas diferentes, recarregar a tela para conferir a persistência, enviar convite e aceitar/recusar no segundo usuário. Para verificar a expiração, use um token inválido numa sessão de teste e abra uma tela interna; deve voltar ao login.

Não é preciso habilitar `API_COM_USUARIO_DE_TESTE` para usar as telas. O atalho continua disponível somente no desenvolvimento; uma sessão que recebe 401 não é renovada silenciosamente. `SecurityConfig` e `AnotacaoServiceImpl.buscarAutor()` permanecem inalterados.

### Resultado da validação em 19/10/2026

- Build de produção do frontend com Node 24.11: passou.
- Suíte completa do backend com Java 25 e H2: 104 testes, nenhuma falha ou erro.
- Chrome headless contra o build de produção e uma API temporária com H2 isolado: 13 verificações passaram. Incluíram cadastro de duas contas, duplicidade de cadastro, login incorreto/correto, token nas chamadas, criação/persistência/ordenação de prova e entrega, filtro por disciplina, convite aceito por outro usuário, recusa, viewport móvel, logout e redirecionamento por 401. Nenhum erro JavaScript foi detectado nas páginas testadas.
- Não foi usado o PostgreSQL local nem foi habilitado o usuário de teste automático nesses fluxos. A validação no seu ambiente de desenvolvimento permanece separada.

Após integrar a main remota em `9f2c5de`, a suíte completa passou com **120 testes**, sem falhas ou erros. Os testes novos de visibilidade usam `UsuarioService` para cadastrar suas contas; o teste de imagens grava a participação explicitamente, sem depender de cascata.
