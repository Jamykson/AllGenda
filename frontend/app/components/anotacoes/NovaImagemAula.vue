<script setup lang="ts">
import { Image as ImageIcon } from '@lucide/vue'

// Formulário curto para adicionar uma imagem avulsa à aula, com legenda opcional.

const props = defineProps<{
  // garante que a aula exista no backend; uma aula recorrente é cadastrada na primeira imagem
  obterAulaId: () => Promise<string>
}>()

const emit = defineEmits<{
  salva: []
  cancelar: []
}>()

const { adicionarImagemAula } = useAnotacoes()

const arquivo = ref<File | null>(null)
const preview = ref<string | null>(null) // URL temporária; liberada ao trocar o arquivo ou fechar
const legenda = ref('')
const enviando = ref(false)
const erro = ref('')

function liberarPreview() {
  if (preview.value) {
    URL.revokeObjectURL(preview.value)
  }
  preview.value = null
}

// confere tipo e tamanho antes de enviar: um arquivo grande demais pode derrubar
// a conexão antes de o servidor responder com o erro
function escolherArquivo(event: Event) {
  const input = event.target as HTMLInputElement
  const escolhido = input.files?.[0]
  input.value = ''

  if (!escolhido) return

  if (!TIPOS_DE_IMAGEM.includes(escolhido.type)) {
    erro.value = `${escolhido.name} não é PNG, JPEG ou WebP.`
    return
  }
  if (escolhido.size > TAMANHO_MAXIMO_IMAGEM) {
    erro.value = `${escolhido.name} passa de 5 MB.`
    return
  }

  liberarPreview()
  arquivo.value = escolhido
  preview.value = URL.createObjectURL(escolhido)
  erro.value = ''
}

async function salvar() {
  if (enviando.value) return
  if (!arquivo.value) {
    erro.value = 'Escolha uma imagem antes de salvar.'
    return
  }

  enviando.value = true
  erro.value = ''

  try {
    const aulaId = await props.obterAulaId()
    await adicionarImagemAula(aulaId, arquivo.value, legenda.value.trim())
    emit('salva')
  }
  catch (falha) {
    console.error('[anotacao] erro ao enviar imagem avulsa:', falha)
    erro.value = mensagemDeErro(falha, 'Não foi possível enviar a imagem. Tente novamente.')
  }
  finally {
    enviando.value = false
  }
}

onBeforeUnmount(liberarPreview)
</script>

<template>
  <form class="nova-imagem" aria-label="Adicionar imagem à aula" @submit.prevent="salvar">
    <div class="cabecalho">
      <p class="titulo">Nova imagem</p>
      <label class="escolher-btn" title="PNG, JPEG ou WebP, até 5 MB">
        <input
          type="file"
          accept="image/png,image/jpeg,image/webp"
          aria-label="Escolher imagem"
          :disabled="enviando"
          @change="escolherArquivo"
        />
        <ImageIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
        {{ arquivo ? 'Trocar imagem' : 'Escolher imagem' }}
      </label>
    </div>

    <img v-if="preview && arquivo" class="previa" :src="preview" :alt="arquivo.name" />

    <label class="campo-legenda">
      <span>Legenda (opcional)</span>
      <input
        v-model="legenda"
        type="text"
        :maxlength="TAMANHO_MAXIMO_LEGENDA"
        placeholder="Descreva a imagem"
        :disabled="enviando"
      />
    </label>

    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>

    <div class="acoes">
      <button type="button" class="btn-cancelar" :disabled="enviando" @click="emit('cancelar')">Cancelar</button>
      <button type="submit" class="btn-salvar" :disabled="enviando">
        {{ enviando ? 'Enviando...' : 'Salvar imagem' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.nova-imagem {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.titulo {
  margin: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.escolher-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid #dfe3e8;
  border-radius: 7px;
  background: #fff;
  color: #344054;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.escolher-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #1f2937;
}

.escolher-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.previa {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border-radius: 8px;
  background: #e2e8f0;
}

.campo-legenda {
  display: grid;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.campo-legenda input {
  height: 34px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  background: #fff;
  color: #0f172a;
  font: inherit;
  font-size: 13px;
  font-weight: 400;
}

.campo-legenda input:focus {
  outline: 2px solid #bfdbfe;
  outline-offset: 1px;
}

.mensagem-erro {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.acoes {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn-cancelar,
.btn-salvar {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancelar {
  border: 1px solid #dfe3e8;
  background: #fff;
  color: #344054;
}

.btn-cancelar:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
}

.btn-salvar {
  border: 0;
  background: #2563eb;
  color: #fff;
}

.btn-salvar:hover {
  background: #1d4ed8;
}

.btn-cancelar:disabled,
.btn-salvar:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.escolher-btn:focus-within,
.btn-cancelar:focus-visible,
.btn-salvar:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}
</style>
