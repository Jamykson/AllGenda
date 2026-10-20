import type { MensagemAssistente } from '~/types/assistente'

// Pergunta ao assistente de IA da aula. A resposta chega por Server-Sent Events, em pedaços, então a
// chamada usa fetch em vez do $api: o axios não entrega o corpo aos poucos no navegador, e o
// EventSource não faz POST nem manda o cabeçalho Authorization. Por isso o token e o tratamento de
// 401 são feitos aqui, como no plugin do axios.
//
// O servidor não guarda a conversa: cada pergunta leva o histórico inteiro.

interface EventoSse {
  nome: string
  dados: string
}

// separa os eventos completos do texto recebido; devolve também o que sobrou de um evento incompleto
function lerEventos(texto: string): { eventos: EventoSse[], resto: string } {
  const blocos = texto.replace(/\r\n?/g, '\n').split('\n\n')
  const resto = blocos.pop() ?? ''
  const eventos = blocos.map((bloco) => {
    let nome = 'message'
    const dados: string[] = []
    for (const linha of bloco.split('\n')) {
      const separador = linha.indexOf(':')
      if (separador <= 0) continue
      const campo = linha.slice(0, separador)
      const valor = linha.slice(separador + 1).replace(/^ /, '')
      if (campo === 'event') nome = valor
      else if (campo === 'data') dados.push(valor)
    }
    return { nome, dados: dados.join('\n') }
  })
  return { eventos, resto }
}

async function detalheDoErro(resposta: Response, padrao: string) {
  try {
    const corpo = await resposta.json()
    if (typeof corpo?.detail === 'string' && corpo.detail.trim()) return corpo.detail as string
  }
  catch {
    // corpo vazio ou que não é JSON (ex: 401 do filtro de segurança)
  }
  return padrao
}

export function useAssistente() {
  const config = useRuntimeConfig()
  const { token, sair } = useAuth()

  // entrega cada pedaço do texto em aoReceberTexto e termina quando a resposta acaba; lança Error com
  // uma mensagem para o aluno se algo der errado. Abortar o sinal lança o AbortError do fetch.
  async function perguntar(
    aulaId: string,
    mensagens: MensagemAssistente[],
    aoReceberTexto: (texto: string) => void,
    sinal: AbortSignal
  ) {
    const resposta = await fetch(`${config.public.apiBase}/api/aulas/${aulaId}/assistente`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream, application/problem+json',
        ...(token.value ? { Authorization: `Bearer ${token.value}` } : {})
      },
      body: JSON.stringify({
        mensagens: mensagens.map(({ papel, texto, foco }) => ({
          papel,
          texto,
          foco: foco ? { tipo: foco.tipo, id: foco.id } : null
        }))
      }),
      signal: sinal
    })

    if (resposta.status === 401) {
      sair()
      await navigateTo({ path: '/', query: { sessao: 'expirada' } })
      throw new Error('Sua sessão expirou.')
    }
    if (!resposta.ok || !resposta.body) {
      throw new Error(await detalheDoErro(resposta, 'Não foi possível falar com o assistente.'))
    }

    const leitor = resposta.body.pipeThrough(new TextDecoderStream()).getReader()
    let pendente = ''
    for (;;) {
      const { value, done } = await leitor.read()
      if (done) break
      const { eventos, resto } = lerEventos(pendente + value)
      pendente = resto
      for (const evento of eventos) {
        if (evento.nome === 'texto') aoReceberTexto(JSON.parse(evento.dados).texto)
        else if (evento.nome === 'erro') throw new Error(JSON.parse(evento.dados).mensagem)
        else if (evento.nome === 'fim') return
      }
    }
    // a conexão caiu antes do evento "fim"
    throw new Error('A resposta foi interrompida. Tente de novo.')
  }

  return { perguntar }
}
