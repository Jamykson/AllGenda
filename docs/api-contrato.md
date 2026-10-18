# Contrato da API de colaboração — A-BE1 (#35)

Base: `/api`. JSON em UTF-8. IDs são UUIDs; datas usam ISO 8601.
Todos os endpoints exigem `Authorization: Bearer <token>`, exceto os POSTs de login e cadastro. Preflight CORS é tratado pelo servidor. JWT expira em 3600 segundos; não há refresh token nesta versão.

## Estado da implementação

Login, cadastro e endpoints de anotações abaixo estão implementados. Convites e a leitura exclusiva de anotações de colegas são um contrato **planejado para mocks**, sem controllers nesta entrega. As permissões de participação descritas nessas operações também são planejadas. Os endpoints atuais de anotações exigem autenticação, mas ainda não restringem acesso por participação nem edição por autor; as exceções são excluir a anotação e anexar ou remover imagens, que já exigem ser o autor dela.

## Cadastro e login

### POST /usuarios — implementado

Requisição:
```json
{"nome":"Maria","email":"maria@example.com","senha":"senha-forte-123"}
```
Sucesso `201`:
```json
{"id":"11111111-1111-4111-8111-111111111111","nome":"Maria","email":"maria@example.com"}
```
Nome obrigatório (até 255 caracteres), e-mail válido (até 254), senha com pelo menos 8 caracteres e até 72 bytes UTF-8. E-mail é normalizado para minúsculas. Nome e e-mail são únicos. Erros: `400` campos inválidos/JSON inválido; `409` nome ou e-mail existente. Senhas e hashes nunca são retornados.

### POST /auth/login — implementado

Requisição:
```json
{"email":"maria@example.com","senha":"senha-forte-123"}
```
Sucesso `200` (token ilustrativo):
```json
{"token":"eyJ...","tipo":"Bearer","expiraEm":3600,"usuario":{"id":"11111111-1111-4111-8111-111111111111","nome":"Maria","email":"maria@example.com"}}
```
Erros: `400` e-mail/JSON inválido; `401` e-mail ou senha incorretos. Usuários legados sem credenciais não conseguem fazer login.

## Convites — planejado, usar mocks

Participação associa um usuário e uma disciplina, com papel `DONO` ou `MEMBRO` e status `PENDENTE`, `ACEITO` ou `RECUSADO`. Um par usuário/disciplina possui no máximo uma participação. Donos são participações `DONO/ACEITO`. Convites são enviados a usuários já cadastrados.

### POST /disciplinas/{disciplinaId}/convites

Apenas dono aceito envia. Corpo:
```json
{"email":"colega@example.com"}
```
Sucesso `201`:
```json
{"id":"22222222-2222-4222-8222-222222222222","disciplinaId":"33333333-3333-4333-8333-333333333333","disciplinaNome":"PDS","usuarioId":"44444444-4444-4444-8444-444444444444","usuarioNome":"João","papel":"MEMBRO","status":"PENDENTE"}
```
Erros: `400` e-mail inválido; `401` sem token válido; `403` solicitante não é dono; `404` disciplina ou destinatário inexistente; `409` participação já existente, inclusive recusada, ou autoconvite. Reenvio de convites recusados fica fora desta versão.

### GET /convites?status=PENDENTE

Sem corpo. Retorna somente convites do usuário autenticado. Filtro opcional, padrão `PENDENTE`; aceita também `ACEITO` e `RECUSADO`.
Sucesso `200`:
```json
[{"id":"22222222-2222-4222-8222-222222222222","disciplinaId":"33333333-3333-4333-8333-333333333333","disciplinaNome":"PDS","usuarioId":"44444444-4444-4444-8444-444444444444","usuarioNome":"João","papel":"MEMBRO","status":"PENDENTE"}]
```
Lista vazia: `[]`. Erros: `400` status inválido; `401` sem token válido.

### PATCH /convites/{id}/aceitar

Sem corpo. Apenas destinatário pode aceitar convite pendente. Sucesso `200`:
```json
{"id":"22222222-2222-4222-8222-222222222222","disciplinaId":"33333333-3333-4333-8333-333333333333","disciplinaNome":"PDS","usuarioId":"44444444-4444-4444-8444-444444444444","usuarioNome":"João","papel":"MEMBRO","status":"ACEITO"}
```
Erros: `401` sem token válido; `403` outro destinatário; `404` convite inexistente; `409` status diferente de `PENDENTE` (inclusive repetição).

### PATCH /convites/{id}/recusar

Sem corpo. Apenas destinatário pode recusar convite pendente. Sucesso `200`:
```json
{"id":"22222222-2222-4222-8222-222222222222","disciplinaId":"33333333-3333-4333-8333-333333333333","disciplinaNome":"PDS","usuarioId":"44444444-4444-4444-8444-444444444444","usuarioNome":"João","papel":"MEMBRO","status":"RECUSADO"}
```
Erros: `401` sem token válido; `403` outro destinatário; `404` convite inexistente; `409` status diferente de `PENDENTE`.

## Anotações

### GET /disciplinas/{disciplinaId}/anotacoes/colegas — planejado

Sem corpo. Participação `ACEITO` obrigatória. Retorna anotações da disciplina cujos autores são outros usuários; exclui as do solicitante. Sucesso `200` com lista no formato abaixo, ou `[]`. Ordenação: data da aula e data de criação crescentes. Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` disciplina inexistente.

### POST /anotacoes — implementado

Corpo (o autor vem exclusivamente do JWT; não enviar `autorId`):
```json
{"aulaId":"55555555-5555-4555-8555-555555555555","conteudo":"Strategy e Observer","tags":["java","poo"]}
```
Sucesso `201`:
```json
{"id":"66666666-6666-4666-8666-666666666666","conteudo":"Strategy e Observer","dataCriacao":"2026-10-14T10:00:00","autorNome":"Maria","tags":["java","poo"],"disciplinaNome":"PDS","aulaId":"55555555-5555-4555-8555-555555555555","aulaData":"2026-10-14","aulaTopico":"Padrões de projeto","imagens":[]}
```
Tags são opcionais; sua ordem não é garantida. `imagens` lista as imagens anexadas (ver [Imagens de anotação](#imagens-de-anotação)); vem vazia numa anotação nova. Erros: `400` aulaId/conteúdo ausente ou JSON inválido; `401` token ausente, inválido ou expirado; `404` aula ou usuário do token não existe.

### GET /anotacoes?aulaId={id} — implementado

Sem corpo. Alternativas: `?disciplinaId={id}` com `&tag=java` opcional, ou `?tag=java`. Filtro por aula não pode ser combinado. Sucesso `200`:
```json
[{"id":"66666666-6666-4666-8666-666666666666","conteudo":"Strategy e Observer","dataCriacao":"2026-10-14T10:00:00","autorNome":"Maria","tags":["java","poo"],"disciplinaNome":"PDS","aulaId":"55555555-5555-4555-8555-555555555555","aulaData":"2026-10-14","aulaTopico":"Padrões de projeto","imagens":[]}]
```
Pode retornar `[]`. Inclui anotações próprias e de outros usuários. Erros: `400` filtro ausente/incompatível, tag vazia ou UUID inválido; `401` sem token válido; `404` aula/disciplina inexistente.

### PATCH /anotacoes/{id} — implementado

Corpo:
```json
{"conteudo":"Conteúdo revisado"}
```
Sucesso `200`: objeto de anotação do POST, com `conteudo` atualizado. Erros: `400` conteúdo vazio/JSON inválido; `401` sem token válido; `404` anotação inexistente.

### DELETE /anotacoes/{id} — implementado

Sem corpo. Apenas o autor exclui. Sucesso `204`. As imagens da anotação são removidas junto: os registros saem na mesma transação e os arquivos são apagados do disco depois que a exclusão é confirmada. Os vínculos com tags também saem, mas as tags continuam existindo para as demais anotações. Erros: `401` sem token válido; `403` solicitante não é o autor; `404` anotação inexistente (inclusive se já foi excluída).

### POST /anotacoes/{id}/tags — implementado

Corpo:
```json
["java","spring"]
```
Sucesso `200`: objeto de anotação do POST, com tags acrescentadas sem duplicação. Erros: `400` lista sem tags válidas/JSON inválido; `401` sem token válido; `404` anotação inexistente.

## Imagens de anotação

Implementado (B-BE1, #54). Aceita PNG, JPEG e WebP de até 5 MB, com no máximo 10 imagens por anotação. O tipo é detectado pelo conteúdo do arquivo: o `Content-Type` e a extensão enviados são ignorados. Os arquivos ficam no diretório `UPLOADS_DIR` (padrão `allgenda/uploads/`, ignorado pelo Git).

### POST /anotacoes/{id}/imagens — implementado

Corpo `multipart/form-data` com o arquivo no campo `arquivo`:
```bash
curl -F arquivo=@quadro.png -H "Authorization: Bearer $TOKEN" localhost:8080/api/anotacoes/66666666-6666-4666-8666-666666666666/imagens
```
Sucesso `201`, com `Location` apontando para a imagem:
```json
{"id":"88888888-8888-4888-8888-888888888888","nome":"quadro.png","contentType":"image/png","tamanho":48213,"url":"/api/anotacoes/66666666-6666-4666-8666-666666666666/imagens/88888888-8888-4888-8888-888888888888"}
```
`nome` é o nome enviado, sem o caminho; serve só para exibição. Erros: `400` campo `arquivo` ausente, arquivo vazio, formato não suportado ou limite de imagens atingido; `401` sem token válido; `403` solicitante não é o autor da anotação; `404` anotação inexistente; `413` arquivo acima de 5 MB.

### GET /anotacoes/{id}/imagens/{imagemId} — implementado

Sem corpo. Sucesso `200` com os bytes da imagem, `Content-Type` do formato detectado e `Content-Disposition: inline`. Qualquer usuário autenticado pode baixar; a restrição por participação virá com a A-BE6 (#40). Erros: `401` sem token válido; `404` imagem inexistente ou de outra anotação.

Como `<img src>` não envia o header `Authorization`, o front deve baixar a imagem com axios (`responseType: 'blob'`) e exibi-la com `URL.createObjectURL`, liberando a URL com `URL.revokeObjectURL` quando não precisar mais dela.

### DELETE /anotacoes/{id}/imagens/{imagemId} — implementado

Sem corpo. Sucesso `204`; o arquivo é apagado do disco depois que a remoção é confirmada no banco. Erros: `401` sem token válido; `403` solicitante não é o autor da anotação; `404` anotação ou imagem inexistente.

## Erros e exemplos para mocks

Erros de aplicação usam Problem Detail (`application/problem+json`), com `instance` podendo conter o caminho. Exemplo:
```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"O conteúdo da anotação é obrigatório.","instance":"/api/anotacoes"}
```
Exemplos para as demais respostas:
```json
{"type":"about:blank","title":"Unauthorized","status":401,"detail":"E-mail ou senha inválidos."}
```
```json
{"type":"about:blank","title":"Forbidden","status":403,"detail":"Apenas o dono pode enviar convites."}
```
```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"Convite não encontrado."}
```
```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"O convite já foi respondido."}
```
O filtro de segurança pode responder `401`/`403` sem JSON e com `WWW-Authenticate: Bearer`; clientes devem tratar o status mesmo com corpo vazio. UUIDs inválidos retornam `400`. Operações sem corpo não precisam enviar `{}`.

## Validação com a equipe

O contrato está pronto para revisão e mocks. A confirmação de entendimento pelos colegas, exigida na issue #35, ainda precisa ocorrer; não foi presumida nesta entrega.

## Execução e verificação — A-BE2 / A-BE3

Veja o `README.md` na raiz para subir o ambiente (`mise run dev`). Em desenvolvimento o `JWT_SECRET` (mínimo de 32 bytes) vem do `mise.toml`; em qualquer outro ambiente use um segredo aleatório próprio, fora do Git. Hibernate cria/atualiza `participacoes`, com FKs obrigatórias e unicidade de `(usuario_id, disciplina_id)`. Não existem cascatas de persistência/remoção nessa associação.

Para conferir um insert manual no PostgreSQL, use IDs existentes de usuário e disciplina:
```sql
BEGIN;
INSERT INTO participacoes (id, usuario_id, disciplina_id, papel, status)
VALUES ('77777777-7777-4777-8777-777777777777',
        '11111111-1111-4111-8111-111111111111',
        '33333333-3333-4333-8333-333333333333', 'MEMBRO', 'PENDENTE');
SELECT * FROM participacoes WHERE id = '77777777-7777-4777-8777-777777777777';
ROLLBACK;
```
Os UUIDs são ilustrativos: substitua as duas FKs por registros reais. O rollback evita deixar dados de teste.

Execute `./mvnw test` para testes com H2 isolado (não requer PostgreSQL nem segredo de produção). Incluem cadastro/hash/login, assinatura JWT, requisição protegida sem token, autoria autenticada e persistência/remoção da participação sem apagar usuário/disciplina. H2 não substitui a validação de inicialização e insert manual no PostgreSQL.
