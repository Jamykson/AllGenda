import { isAxiosError } from 'axios'
import type { Anotacao, AulaDaAnotacao, ImagemAnotacao, ImagemAula, Tag } from '~/types/anotacao'

// Limites aceitos pelo backend (app.uploads.* no application.properties).
export const TIPOS_DE_IMAGEM = ['image/png', 'image/jpeg', 'image/webp']
export const TAMANHO_MAXIMO_IMAGEM = 5 * 1024 * 1024
export const MAXIMO_IMAGENS_POR_ANOTACAO = 10
export const TAMANHO_MAXIMO_TITULO = 150
export const TAMANHO_MAXIMO_LEGENDA = 300
export const TOPICO_AULA_RECORRENTE = 'Aula recorrente'

// Um arquivo de 5 MB pode demorar mais que o timeout padrão do $api (5 s) numa rede lenta.
const TIMEOUT_UPLOAD = 30_000

/**
 * Texto para mostrar ao usuário a partir de um erro da API.
 * Usa o "detail" do Problem Detail quando o backend o envia; senão, a mensagem padrão.
 */
export function mensagemDeErro(erro: unknown, padrao: string): string {
  if (isAxiosError(erro)) {
    const detalhe = (erro.response?.data as { detail?: unknown } | undefined)?.detail
    if (typeof detalhe === 'string' && detalhe.trim()) {
      return detalhe
    }
    if (erro.response?.status === 413) {
      return 'A imagem passa de 5 MB.'
    }
  }
  return padrao
}

/**
 * Chamadas à API de anotações, tags e imagens (anexadas a anotações ou avulsas na aula).
 *
 * Uso:
 *   const { listarPorAula, criar } = useAnotacoes()
 */
export function useAnotacoes() {
  const { $api } = useNuxtApp()

  async function listarPorAula(aulaId: string) {
    const { data } = await $api.get<Anotacao[]>('/api/anotacoes', { params: { aulaId } })
    return data
  }

  async function listarPorDisciplina(disciplinaId: string) {
    const { data } = await $api.get<Anotacao[]>('/api/anotacoes', { params: { disciplinaId } })
    return data
  }

  async function listarTags() {
    const { data } = await $api.get<Tag[]>('/api/tags')
    return data.map(tag => tag.nome)
  }

  // título e conteúdo vazios vão como null; a API exige ao menos um dos dois
  async function criar(aulaId: string, titulo: string, conteudo: string, tags: string[]) {
    const { data } = await $api.post<Anotacao>('/api/anotacoes', {
      aulaId,
      titulo: titulo || null,
      conteudo: conteudo || null,
      tags
    })
    return data
  }

  // só o autor edita; a API substitui os dois campos, então o título é sempre reenviado
  async function editar(anotacaoId: string, titulo: string, conteudo: string) {
    const { data } = await $api.patch<Anotacao>(`/api/anotacoes/${anotacaoId}`, {
      titulo: titulo || null,
      conteudo: conteudo || null
    })
    return data
  }

  // a API só acrescenta tags; não há como remover uma tag de uma anotação
  async function acrescentarTags(anotacaoId: string, tags: string[]) {
    const { data } = await $api.post<Anotacao>(`/api/anotacoes/${anotacaoId}/tags`, tags)
    return data
  }

  async function excluir(anotacaoId: string) {
    await $api.delete(`/api/anotacoes/${anotacaoId}`)
  }

  async function anexarImagem(anotacaoId: string, arquivo: File) {
    const formulario = new FormData()
    formulario.append('arquivo', arquivo)
    const { data } = await $api.post<ImagemAnotacao>(
      `/api/anotacoes/${anotacaoId}/imagens`,
      formulario,
      { timeout: TIMEOUT_UPLOAD }
    )
    return data
  }

  async function removerImagem(imagem: ImagemAnotacao) {
    await $api.delete(imagem.url)
  }

  async function listarImagensDaAula(aulaId: string) {
    const { data } = await $api.get<ImagemAula[]>(`/api/aulas/${aulaId}/imagens`)
    return data
  }

  async function adicionarImagemAula(aulaId: string, arquivo: File, legenda: string) {
    const formulario = new FormData()
    formulario.append('arquivo', arquivo)
    if (legenda) {
      formulario.append('legenda', legenda)
    }
    const { data } = await $api.post<ImagemAula>(
      `/api/aulas/${aulaId}/imagens`,
      formulario,
      { timeout: TIMEOUT_UPLOAD }
    )
    return data
  }

  // só o autor edita; legenda vazia deixa a imagem sem legenda
  async function editarLegenda(imagem: ImagemAula, legenda: string) {
    const { data } = await $api.patch<ImagemAula>(imagem.url, { legenda: legenda || null })
    return data
  }

  async function removerImagemAula(imagem: ImagemAula) {
    await $api.delete(imagem.url)
  }

  // <img src> não envia o token, então a imagem é baixada aqui e exibida com URL.createObjectURL.
  // Serve tanto para imagens de anotação quanto para imagens avulsas da aula.
  async function baixarImagem(imagem: { url: string }) {
    const { data } = await $api.get<Blob>(imagem.url, { responseType: 'blob' })
    return data
  }

  // Cadastra no backend uma aula recorrente (gerada no navegador), com a mesma data e o mesmo horário.
  // As telas escondem a aula recorrente quando existe uma aula real igual, então ela passa a ser uma aula normal.
  async function cadastrarAulaRecorrente(aula: AulaDaAnotacao) {
    if (!aula.disciplinaId) {
      throw new Error('A aula recorrente não informa a disciplina.')
    }
    const { data } = await $api.post<AulaDaAnotacao>('/api/aulas', {
      disciplinaId: aula.disciplinaId,
      data: aula.data,
      horario: aula.horario,
      topico: aula.topico || TOPICO_AULA_RECORRENTE
    })
    return data
  }

  return {
    listarPorAula,
    listarPorDisciplina,
    listarTags,
    criar,
    editar,
    acrescentarTags,
    excluir,
    anexarImagem,
    removerImagem,
    listarImagensDaAula,
    adicionarImagemAula,
    editarLegenda,
    removerImagemAula,
    baixarImagem,
    cadastrarAulaRecorrente
  }
}
