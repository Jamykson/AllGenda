<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'

const props = defineProps<{
  disciplina: Disciplina
}>()

const { atualizarDescricao } = useDisciplinas()

const editando = ref(false)
const descricao = ref('')
const salvando = ref(false)
const erro = ref('')

function comecar() {
  descricao.value = props.disciplina.descricao ?? ''
  erro.value = ''
  editando.value = true
}

async function salvar() {
  salvando.value = true
  erro.value = ''
  try {
    const atualizada = await atualizarDescricao(props.disciplina.id, descricao.value)
    // muda o mesmo objeto da lista, assim o card também mostra a nova descrição
    props.disciplina.descricao = atualizada.descricao ?? null
    editando.value = false
  }
  catch (err) {
    console.error('[disciplina] falha ao salvar a descrição:', err)
    erro.value = mensagemDeErro(err, 'Não foi possível salvar a descrição.')
  }
  finally {
    salvando.value = false
  }
}
</script>

<template>
  <div class="editar-descricao">
    <button v-if="!editando" type="button" class="link-acao" @click="comecar">
      {{ disciplina.descricao ? 'Editar descrição' : 'Adicionar descrição' }}
    </button>

    <form v-else class="edicao-descricao" @submit.prevent="salvar">
      <label class="rotulo" for="descricao-disciplina">Descrição</label>
      <textarea
        id="descricao-disciplina"
        v-model="descricao"
        rows="3"
        maxlength="500"
        :disabled="salvando"
      />
      <p v-if="erro" class="erro" role="alert">{{ erro }}</p>
      <div class="acoes">
        <button type="submit" class="botao-primario" :disabled="salvando">
          {{ salvando ? 'Salvando...' : 'Salvar' }}
        </button>
        <button type="button" class="botao-secundario" :disabled="salvando" @click="editando = false">
          Cancelar
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.editar-descricao {
  margin-top: 6px;
}

.link-acao {
  padding: 0;
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.link-acao:hover {
  text-decoration: underline;
}

.edicao-descricao {
  display: grid;
  gap: 8px;
}

.rotulo {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

textarea {
  box-sizing: border-box;
  width: 100%;
  padding: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font: inherit;
  resize: vertical;
}

.erro {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.acoes {
  display: flex;
  gap: 8px;
}

.botao-primario,
.botao-secundario {
  padding: 6px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.botao-primario {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}
</style>
