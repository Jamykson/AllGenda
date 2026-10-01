<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'
import { PencilLine, Trash2 } from '@lucide/vue'

const props = defineProps<{
  disciplina: Disciplina
}>()

const emit = defineEmits<{
  abrir: []
  editar: []
  excluir: []
}>()

const textoCard = computed(() => {
  const hex = props.disciplina.cor || '#f8fafc'
  const correspondencia = /^#?([\da-f]{2})([\da-f]{2})([\da-f]{2})$/i.exec(hex)

  if (!correspondencia) return '#0f172a'

  const canais = correspondencia.slice(1).map(canal => {
    const valor = Number.parseInt(canal, 16) / 255
    return valor <= 0.04045 ? valor / 12.92 : ((valor + 0.055) / 1.055) ** 2.4
  })
  const luminancia = 0.2126 * (canais[0] ?? 0)
    + 0.7152 * (canais[1] ?? 0)
    + 0.0722 * (canais[2] ?? 0)

  return luminancia > 0.42 ? '#0f172a' : '#ffffff'
})
</script>

<template>
  <article
    class="disciplina-card"
    :style="{
      backgroundColor: disciplina.cor || '#f8fafc',
      color: textoCard
    }"
  >
    <div
      class="conteudo-card"
      role="button"
      tabindex="0"
      :aria-label="`Abrir aulas de ${disciplina.nome}`"
      @click="emit('abrir')"
      @keydown.enter.prevent="emit('abrir')"
      @keydown.space.prevent="emit('abrir')"
    >
      <div class="icone-disciplina" aria-hidden="true">
        {{ disciplina.icone || disciplina.nome[0] }}
      </div>

      <div class="texto-disciplina">
        <h3>{{ disciplina.nome }}</h3>
        <p v-if="disciplina.descricao">{{ disciplina.descricao }}</p>
      </div>
    </div>

    <div class="acoes-card" aria-label="Ações da disciplina">
      <button
        type="button"
        class="botao-acao"
        :aria-label="`Editar ${disciplina.nome}`"
        title="Editar disciplina"
        @click="emit('editar')"
      >
        <PencilLine :size="15" :stroke-width="1.8" aria-hidden="true" />
      </button>
      <button
        type="button"
        class="botao-acao botao-excluir"
        :aria-label="`Excluir ${disciplina.nome}`"
        title="Excluir disciplina"
        @click="emit('excluir')"
      >
        <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
      </button>
    </div>
  </article>
</template>

<style scoped>
.disciplina-card {
  position: relative;
  display: flex;
  min-height: 90px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  overflow: hidden;
  padding: 18px 20px;
  border: 1px solid rgb(15 23 42 / 12%);
  border-radius: 12px;
  transition: box-shadow 150ms ease, transform 150ms ease;
}

.disciplina-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgb(15 23 42 / 12%);
}

.conteudo-card {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.conteudo-card:focus-visible {
  border-radius: 6px;
  outline: 2px solid currentColor;
  outline-offset: 4px;
}

.icone-disciplina {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  place-items: center;
  border-radius: 12px;
  background: rgb(255 255 255 / 20%);
  font-size: 22px;
}

.texto-disciplina {
  min-width: 0;
}

.texto-disciplina h3 {
  overflow-wrap: anywhere;
  font-weight: 700;
}

.texto-disciplina p {
  margin-top: 2px;
  color: currentColor;
  opacity: 0.78;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.acoes-card {
  display: flex;
  flex: 0 0 auto;
  gap: 6px;
  opacity: 0;
  transition: opacity 140ms ease;
}

.disciplina-card:hover .acoes-card,
.disciplina-card:focus-within .acoes-card {
  opacity: 1;
}

.botao-acao {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid rgb(15 23 42 / 18%);
  border-radius: 6px;
  background: rgb(255 255 255 / 82%);
  color: #334155;
  cursor: pointer;
}

.botao-acao:hover {
  background: #fff;
  color: #0f172a;
}

.botao-excluir:hover {
  color: #b91c1c;
}

.botao-acao:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}

@media (hover: none) {
  .acoes-card {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .disciplina-card,
  .acoes-card {
    transition: none;
  }
}
</style>