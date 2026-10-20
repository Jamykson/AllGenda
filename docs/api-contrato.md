# Contrato da API de colaboração — A-BE1 (#35)

Base: `/api`. JSON em UTF-8. IDs são UUIDs; datas usam ISO 8601.
Todos os endpoints exigem `Authorization: Bearer <token>`, exceto os POSTs de login e cadastro. Preflight CORS é tratado pelo servidor. JWT expira em 3600 segundos; não há refresh token nesta versão.

## Estado da implementação

Login, cadastro, disciplinas, aulas, convites, anotações (inclusive as dos colegas), tags, imagens e o assistente da aula estão implementados. Avaliações: veja [contrato de avaliações e telas](api-telas-colaboracao.md).

### Regra geral de acesso

Quase tudo o que pertence a uma disciplina exige **participação `ACEITO`** nela (dono ou membro). Quem não participa, ou tem convite pendente ou recusado, recebe `403`. A checagem é feita num só lugar do back-end, a classe `AcessoDisciplina`, e vem **depois** da verificação de existência: um id que não existe dá `404`, e um que existe mas é de uma disciplina alheia dá `403`.

| Operação | Quem pode |
|---|---|
| Listar disciplinas (`GET /disciplinas`) | Qualquer usuário autenticado, mas só vê as disciplinas em que participa (`ACEITO`) |
| Ver disciplina, aulas, anotações, tags, avaliações e imagens da aula | Participante `ACEITO` |
| Criar aula, criar anotação, acrescentar/remover tags, renomear/excluir tag, reordenar imagens da aula | Participante `ACEITO` |
| Baixar imagem de uma anotação | Participante `ACEITO` da disciplina da anotação |
| Excluir disciplina, enviar convite | Somente o dono (`DONO` com `ACEITO`) |
| Editar ou excluir uma anotação, anexar ou remover suas imagens, editar ou excluir uma imagem da aula | Somente o autor |
| Aceitar ou recusar convite | Somente o destinatário |

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

## Disciplinas — implementado

### POST /disciplinas

Corpo (a descrição é opcional):
```json
{"nome":"PDS","descricao":"Projeto e desenvolvimento de software"}
```
Quem cria a disciplina passa a ser o **dono**: o back-end grava, na mesma transação, a participação `DONO/ACEITO` dele. Sucesso `201`:
```json
{"id":"33333333-3333-4333-8333-333333333333","nome":"PDS","descricao":"Projeto e desenvolvimento de software"}
```
O nome é obrigatório e é salvo sem os espaços das pontas. A descrição tem no máximo 500 caracteres; vazia, fica `null`. Erros: `400` nome ausente/vazio, descrição com mais de 500 caracteres ou JSON inválido; `401` sem token válido.

### GET /disciplinas

Sem corpo. Devolve **somente as disciplinas em que o usuário autenticado tem participação `ACEITO`** (as que ele criou e as de convites aceitos). Convites pendentes ou recusados e disciplinas de outras pessoas não aparecem. Sucesso `200` com uma lista no formato acima, ou `[]`. Erro: `401` sem token válido.

### GET /disciplinas/{id}

Sem corpo. Sucesso `200` com a disciplina, no formato do POST. Erros: `400` UUID inválido; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` disciplina inexistente.

### DELETE /disciplinas/{id}

Sem corpo. **Somente o dono** exclui. Apaga também as aulas, as avaliações, as tags, as participações e os arquivos das imagens da disciplina. A checagem de permissão vem antes de qualquer remoção. Sucesso `204`. Erros: `400` UUID inválido; `401` sem token válido; `403` solicitante não é o dono; `404` disciplina inexistente (inclusive se já foi excluída).

## Convites — implementado

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

### GET /disciplinas/{disciplinaId}/anotacoes/colegas — implementado

Sem corpo. Participação `ACEITO` obrigatória. Retorna anotações da disciplina cujos autores são outros usuários; exclui as do solicitante. Sucesso `200` com lista no formato abaixo, ou `[]`. Ordenação: data da aula e data de criação crescentes. Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` disciplina inexistente.

### POST /anotacoes — implementado

Corpo (o autor vem exclusivamente do JWT; não enviar `autorId`):
```json
{"aulaId":"55555555-5555-4555-8555-555555555555","titulo":"Padrões comportamentais","conteudo":"Strategy e Observer","tags":["java","poo"]}
```
Sucesso `201`:
```json
{"id":"66666666-6666-4666-8666-666666666666","titulo":"Padrões comportamentais","conteudo":"Strategy e Observer","dataCriacao":"2026-10-14T10:00:00","autorId":"11111111-1111-4111-8111-111111111111","autorNome":"Maria","tags":["java","poo"],"disciplinaNome":"PDS","aulaId":"55555555-5555-4555-8555-555555555555","aulaData":"2026-10-14","aulaTopico":"Padrões de projeto","imagens":[]}
```
`titulo` e `conteudo` são opcionais, mas é preciso informar ao menos um dos dois; o que vier vazio ou só com espaços fica `null` na resposta. O título tem no máximo 150 caracteres e é salvo sem os espaços das pontas. Tags são opcionais; sua ordem não é garantida. `autorId` é o `id` do autor, o mesmo `usuario.id` da resposta do login; o front compara os dois para saber se a anotação é do usuário logado. `imagens` lista as imagens anexadas (ver [Imagens de anotação](#imagens-de-anotação)); vem vazia numa anotação nova. Erros: `400` aulaId ausente, título e conteúdo ausentes, título com mais de 150 caracteres ou JSON inválido; `401` token ausente, inválido ou expirado; `403` o usuário não tem participação `ACEITO` na disciplina da aula; `404` aula ou usuário do token não existe.

### GET /anotacoes?aulaId={id} — implementado

Sem corpo. Alternativas: `?disciplinaId={id}` com `&tag=java` opcional, ou `?tag=java`. Filtro por aula não pode ser combinado. Sucesso `200`:
```json
[{"id":"66666666-6666-4666-8666-666666666666","titulo":"Padrões comportamentais","conteudo":"Strategy e Observer","dataCriacao":"2026-10-14T10:00:00","autorId":"11111111-1111-4111-8111-111111111111","autorNome":"Maria","tags":["java","poo"],"disciplinaNome":"PDS","aulaId":"55555555-5555-4555-8555-555555555555","aulaData":"2026-10-14","aulaTopico":"Padrões de projeto","imagens":[]}]
```
Pode retornar `[]`. Inclui anotações próprias e de outros usuários. Com `aulaId` ou `disciplinaId`, exige participação `ACEITO` na disciplina; só com `tag`, devolve apenas anotações das disciplinas em que o usuário participa. Erros: `400` filtro ausente/incompatível, tag vazia ou UUID inválido; `401` sem token válido; `403` participação ausente/pendente/recusada na disciplina do filtro; `404` aula/disciplina inexistente.

### PATCH /anotacoes/{id} — implementado

Corpo:
```json
{"titulo":"Padrões comportamentais","conteudo":"Conteúdo revisado"}
```
Apenas o autor edita. Os dois campos são substituídos pelos valores enviados: um campo ausente ou vazio fica `null`, então para manter o título é preciso reenviá-lo. Vale a mesma regra do POST, ao menos um dos dois é obrigatório. Sucesso `200`: objeto de anotação do POST, com `titulo` e `conteudo` atualizados. Erros: `400` título e conteúdo vazios, título com mais de 150 caracteres ou JSON inválido; `401` sem token válido; `403` solicitante não é o autor; `404` anotação inexistente.

### DELETE /anotacoes/{id} — implementado

Sem corpo. Apenas o autor exclui. Sucesso `204`. As imagens da anotação são removidas junto: os registros saem na mesma transação e os arquivos são apagados do disco depois que a exclusão é confirmada. Os vínculos com tags também saem, mas as tags continuam existindo para as demais anotações. Erros: `401` sem token válido; `403` solicitante não é o autor; `404` anotação inexistente (inclusive se já foi excluída).

### POST /anotacoes/{id}/tags — implementado

Corpo:
```json
["java","spring"]
```
Qualquer participante da disciplina (participação `ACEITO`) acrescenta tags, inclusive em anotações de outros autores; o autor da anotação não muda. As tags são da disciplina da aula (ver [Tags](#tags)): um nome que a disciplina já tem, sem diferenciar maiúsculas, reaproveita a tag dela; um nome novo cria a tag na disciplina. Sucesso `200`: objeto de anotação do POST, com tags acrescentadas sem duplicação. Erros: `400` lista sem tags válidas/JSON inválido; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` anotação inexistente.

### DELETE /anotacoes/{id}/tags?nome={nome} — implementado

Sem corpo. Tira a tag `nome` da anotação (o nome vai na query porque pode ter `/`, como em `C/C++`), sem diferenciar maiúsculas; a tag continua existindo para as demais anotações. Como acrescentar, é permitido a qualquer participante da disciplina (participação `ACEITO`), inclusive em anotações de outros autores. Uma tag que a anotação não tem não é erro. Sucesso `200`: objeto de anotação do POST, sem a tag. Erros: `400` parâmetro `nome` ausente ou vazio; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` anotação inexistente.

## Tags

Cada disciplina tem as suas tags: "Prova" em duas disciplinas são duas tags, e renomear ou excluir uma não afeta a outra. Dentro da disciplina, o nome não se repete, sem diferenciar maiúsculas. Uma tag é criada ao ser atribuída a uma anotação (`POST /anotacoes/{id}/tags` ou `tags` no `POST /anotacoes`). A busca de anotações por tag (`GET /anotacoes?tag=`) continua pelo nome, então encontra a tag de mesmo nome em todas as disciplinas do usuário.

Bancos criados quando as tags eram globais são migrados ao iniciar o back-end: cada tag antiga vai para as disciplinas das anotações que a usam (uma cópia por disciplina), as tags antigas sem uso são apagadas, e a restrição única antiga em `tags(nome)` é removida no PostgreSQL.

### GET /tags?disciplinaId={id} — implementado

Sem corpo. Com `disciplinaId`, lista as tags da disciplina e exige participação `ACEITO`; sem o filtro, lista as tags de todas as disciplinas em que o usuário participa. Em ordem alfabética, sem diferenciar maiúsculas. Sucesso `200`:
```json
[{"id":"77777777-7777-4777-8777-777777777777","nome":"Prova","disciplinaId":"33333333-3333-4333-8333-333333333333"}]
```
Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` disciplina inexistente.

### PATCH /tags/{id} — implementado

Corpo:
```json
{"nome":"Prova 1"}
```
Renomeia a tag em todas as anotações da disciplina. Qualquer participante da disciplina renomeia. Mudar só maiúsculas e minúsculas é permitido. Sucesso `200` com a tag. Erros: `400` nome vazio ou com mais de 255 caracteres; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` tag inexistente; `409` outra tag da disciplina já tem esse nome.

### DELETE /tags/{id} — implementado

Sem corpo. Tira a tag de todas as anotações da disciplina e a apaga; as anotações continuam existindo. Qualquer participante da disciplina exclui. Sucesso `204`. Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` tag inexistente.

## Imagens de anotação

Implementado (B-BE1, #54). Aceita PNG, JPEG e WebP de até 5 MB, com no máximo 10 imagens por anotação. O tipo é detectado pelo conteúdo do arquivo: o `Content-Type` e a extensão enviados são ignorados. Os arquivos ficam no diretório `UPLOADS_DIR` (padrão `allgenda/uploads/`, ignorado pelo Git). Excluir a anotação (`DELETE /anotacoes/{id}`) ou a disciplina inteira (`DELETE /disciplinas/{id}`) também apaga os arquivos das imagens, depois que a exclusão é confirmada no banco.

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

Sem corpo. Sucesso `200` com os bytes da imagem, `Content-Type` do formato detectado e `Content-Disposition: inline`. Exige participação `ACEITO` na disciplina da anotação (a checagem vem antes de procurar a imagem, para quem está de fora não descobrir quais ids existem). Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` anotação inexistente, ou imagem inexistente ou de outra anotação.

Como `<img src>` não envia o header `Authorization`, o front deve baixar a imagem com axios (`responseType: 'blob'`) e exibi-la com `URL.createObjectURL`, liberando a URL com `URL.revokeObjectURL` quando não precisar mais dela.

### DELETE /anotacoes/{id}/imagens/{imagemId} — implementado

Sem corpo. Sucesso `204`; o arquivo é apagado do disco depois que a remoção é confirmada no banco. Erros: `401` sem token válido; `403` solicitante não é o autor da anotação; `404` anotação ou imagem inexistente.

## Aulas

Todas as operações de aula exigem participação `ACEITO` na disciplina.

### POST /aulas — implementado

Corpo (todos os campos são obrigatórios):
```json
{"disciplinaId":"33333333-3333-4333-8333-333333333333","data":"2026-10-14","horario":"08:00 – 09:40","topico":"Padrões de projeto"}
```
Sucesso `201`: objeto de aula, no formato do `GET /aulas/{id}` abaixo. Erros: `400` campo ausente/vazio, data inválida ou JSON inválido; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` disciplina inexistente.

### GET /aulas?disciplinaId={id} — implementado

Sem corpo. Lista as aulas da disciplina, no formato do `GET /aulas/{id}`, ou `[]`. Erros: `400` `disciplinaId` ausente ou UUID inválido; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` disciplina inexistente.

### GET /aulas/{id} — implementado

Sem corpo. Sucesso `200`:
```json
{"id":"55555555-5555-4555-8555-555555555555","data":"2026-10-14","horario":"08:00 – 09:40","topico":"Padrões de projeto","disciplinaId":"33333333-3333-4333-8333-333333333333","disciplinaNome":"PDS"}
```
A página da aula usa `disciplinaId` para reconhecer a disciplina. Erros: `400` UUID inválido; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` aula inexistente.

## Imagens da aula

Imagens avulsas, que pertencem à aula e não a uma anotação, com legenda opcional. Aceitam os mesmos formatos e o mesmo tamanho máximo das [imagens de anotação](#imagens-de-anotação), sem limite de quantidade por aula. Enviar, listar, baixar e reordenar exigem participação `ACEITO` na disciplina da aula; editar a legenda e excluir exigem ser o autor da imagem. As imagens têm uma ordem manual, a mesma para todos os participantes: `posicao` começa em 0, e uma imagem nova entra no fim. Excluir a disciplina (`DELETE /disciplinas/{id}`) também apaga os arquivos dessas imagens. A busca de anotações (`GET /anotacoes`) não inclui imagens avulsas.

### POST /aulas/{aulaId}/imagens — implementado

Corpo `multipart/form-data` com o arquivo no campo `arquivo` e, opcionalmente, a legenda no campo `legenda`:
```bash
curl -F arquivo=@slide.png -F legenda="Diagrama de classes" -H "Authorization: Bearer $TOKEN" localhost:8080/api/aulas/55555555-5555-4555-8555-555555555555/imagens
```
Sucesso `201`, com `Location` apontando para a imagem:
```json
{"id":"99999999-9999-4999-8999-999999999999","legenda":"Diagrama de classes","nome":"slide.png","contentType":"image/png","tamanho":48213,"url":"/api/aulas/55555555-5555-4555-8555-555555555555/imagens/99999999-9999-4999-8999-999999999999","dataCriacao":"2026-10-14T10:05:00","autorId":"11111111-1111-4111-8111-111111111111","autorNome":"Maria","aulaId":"55555555-5555-4555-8555-555555555555","posicao":0}
```
A legenda tem no máximo 300 caracteres e é salva sem os espaços das pontas; vazia, fica `null`. `autorId` serve, como nas anotações, para o front saber se a imagem é do usuário logado. Erros: `400` campo `arquivo` ausente, arquivo vazio, formato não suportado ou legenda com mais de 300 caracteres; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` aula inexistente; `413` arquivo acima de 5 MB.

### GET /aulas/{aulaId}/imagens — implementado

Sem corpo. Sucesso `200` com a lista de imagens avulsas da aula, no formato do POST, na ordem da galeria (`posicao`; em caso de empate, data de criação). Pode retornar `[]`. Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` aula inexistente.

### PUT /aulas/{aulaId}/imagens/ordem — implementado

Corpo: os ids de todas as imagens da aula, na nova ordem.
```json
["99999999-9999-4999-8999-999999999999","88888888-8888-4888-8888-888888888888"]
```
Qualquer participante reordena, não só os autores das imagens. A lista precisa ter cada imagem da aula exatamente uma vez; uma lista diferente (por exemplo, de uma tela desatualizada) é recusada sem mudar nada. Sucesso `200` com a lista de imagens na nova ordem, no formato do GET. Erros: `400` lista com imagem faltando, repetida ou de outra aula, ou JSON inválido; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` aula inexistente.

### GET /aulas/{aulaId}/imagens/{imagemId} — implementado

Sem corpo. Sucesso `200` com os bytes da imagem, como em `GET /anotacoes/{id}/imagens/{imagemId}`; o front a exibe da mesma forma, baixando com axios e usando `URL.createObjectURL`. Erros: `401` sem token válido; `403` participação ausente/pendente/recusada; `404` aula ou imagem inexistente, ou imagem de outra aula.

### PATCH /aulas/{aulaId}/imagens/{imagemId} — implementado

Corpo:
```json
{"legenda":"Diagrama revisado"}
```
Apenas o autor edita. A legenda é substituída; ausente ou vazia, a imagem fica sem legenda. Sucesso `200`: objeto de imagem do POST. Erros: `400` legenda com mais de 300 caracteres ou JSON inválido; `401` sem token válido; `403` solicitante não é o autor; `404` imagem inexistente ou de outra aula.

### DELETE /aulas/{aulaId}/imagens/{imagemId} — implementado

Sem corpo. Apenas o autor exclui. Sucesso `204`; o arquivo é apagado do disco depois que a remoção é confirmada no banco. Erros: `401` sem token válido; `403` solicitante não é o autor; `404` imagem inexistente ou de outra aula.

## Assistente da aula

Assistente de IA da página da aula: responde perguntas usando as anotações (de todos os participantes) e as imagens (avulsas e anexadas) da aula. Usa a API da Anthropic, configurada no back-end pela variável `ANTHROPIC_API_KEY` (e, opcionalmente, `ASSISTENTE_MODELO`); sem a chave, o back-end sobe normalmente e o endpoint responde `503`. **O material da aula é enviado à Anthropic** a cada pergunta, e a tela avisa isso.

O servidor não guarda as conversas: o cliente envia a conversa inteira a cada pergunta. As imagens são numeradas como na tela: primeiro as avulsas, na ordem da galeria; depois as anexadas, na ordem das anotações. Cada pergunta leva no máximo 20 imagens (`app.assistente.maximo-imagens`); a imagem em foco sempre entra. Imagens grandes são reduzidas para 1568 px no maior lado antes do envio. Cada usuário faz no máximo 30 perguntas por hora (`app.assistente.perguntas-por-hora`).

### POST /aulas/{aulaId}/assistente — implementado

Corpo: a conversa, alternando `USUARIO` e `ASSISTENTE`, começando e terminando com uma pergunta. `foco` é opcional e indica a anotação (`ANOTACAO`) ou a imagem avulsa ou anexada (`IMAGEM`) da aula sobre a qual é a pergunta.
```json
{"mensagens":[
  {"papel":"USUARIO","texto":"Faça um resumo da aula."},
  {"papel":"ASSISTENTE","texto":"A aula tratou dos padrões Observer e Strategy..."},
  {"papel":"USUARIO","texto":"O que esta imagem mostra?","foco":{"tipo":"IMAGEM","id":"99999999-9999-4999-8999-999999999999"}}
]}
```
Envie `Accept: text/event-stream, application/problem+json`. Sucesso `200` com `Content-Type: text/event-stream` (Server-Sent Events, em UTF-8). A resposta chega em pedaços, à medida que o modelo a gera:
```text
event:texto
data:{"texto":"A aula tratou "}

event:texto
data:{"texto":"dos padrões Observer e Strategy."}

event:fim
data:{}
```
Se o modelo falhar depois que a resposta começou (serviço sobrecarregado, erro de rede), o stream termina com `event:erro` e `data:{"mensagem":"..."}`, sem `fim`. Fechar a conexão interrompe a geração. Como o `EventSource` do navegador não faz POST nem envia o cabeçalho `Authorization`, o front lê o stream com `fetch` (`composables/useAssistente.ts`).

Os erros abaixo saem antes do stream, como Problem Details em JSON: `400` conversa vazia, fora de ordem, com mais de 40 mensagens, pergunta com mais de 4000 caracteres ou `foco` que não pertence à aula; `401` sem token válido; `403` participação ausente/pendente/recusada; `404` aula inexistente; `429` limite de perguntas por hora atingido; `503` assistente não configurado no servidor.

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

Veja o `README.md` na raiz para subir o ambiente (`mise run dev`). Em desenvolvimento o `JWT_SECRET` (mínimo de 32 bytes) vem do `mise.toml`; em qualquer outro ambiente use um segredo aleatório próprio, fora do Git. Hibernate cria/atualiza `participacoes`, com FKs obrigatórias e unicidade de `(usuario_id, disciplina_id)`. A associação não usa cascatas nem remoção de órfãos em nenhum lado. A criação da disciplina grava a participação do dono explicitamente; a exclusão da disciplina remove seus vínculos explicitamente antes de apagar a disciplina, na mesma transação. Remover um item da coleção Java não apaga a participação.

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

### Evidências de aceite — #35, #36 e #37

- #35: contrato neste arquivo com verbos, caminhos, corpos, sucessos, erros e exemplos JSON. A confirmação de entendimento dos colegas continua pendente; não foi enviada mensagem nem presumida aprovação.
- #36: `Participacao` e repository existentes; duas FKs obrigatórias, enums persistidos como texto, sem composição JPA. Teste H2 executa insert SQL direto, verifica ambas as FKs e confirma que tirar uma participação da coleção não apaga o registro. Ainda falta executar o insert no PostgreSQL local para validar esse ambiente específico.
- #37: login com BCrypt e JWT, `401` sem token e com token expirado, e autoria pela identidade autenticada são cobertos pelos testes. `UsuarioPadraoInitializer` não existe; `UsuarioTesteInitializer` é apenas uma conveniência de desenvolvimento e não substitui a identidade do JWT nas requisições. `SecurityConfig` e o código de autoria foram preservados.

Validação final em 19/10/2026: suíte completa com **107 testes passando**, sem falhas ou erros. Não havia container PostgreSQL em execução; nenhum banco local foi criado, apagado ou alterado.
