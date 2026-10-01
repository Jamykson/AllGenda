<script setup lang="ts">

import type { Disciplina } from "~/types/disciplina"


defineProps<{

disciplina:Disciplina

}>()


const emit = defineEmits([

"fechar"

])


</script>



<template>
	<div class="modal-overlay" @click.self="emit('fechar')">
		<section
			class="modal-painel"
			role="dialog"
			aria-modal="true"
			aria-labelledby="titulo-disciplina"
		>
			<header class="modal-header">
				<h2 id="titulo-disciplina">{{ disciplina.nome }}</h2>
				<button
					type="button"
					class="botao-fechar"
					aria-label="Fechar detalhes da disciplina"
					@click="emit('fechar')"
				>
					×
				</button>
			</header>

			<div class="personalizacao-area">
				<h3>Personalização</h3>
				<div class="personalizacao-toolbar" aria-label="Personalizar disciplina">
					<EditarNome :disciplina="disciplina" />
					<EditarCor :disciplina="disciplina" />
					<EditarIcone :disciplina="disciplina" />
				</div>
			</div>

			<ListaAulasDisciplina :disciplina-id="disciplina.id" :cor="disciplina.cor || '#2563eb'" />
			<HorarioModal :disciplina="disciplina" />
		</section>
	</div>
</template>

<style scoped>
.modal-overlay {
	position: fixed;
	inset: 0;
	z-index: 60;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	overflow-y: auto;
	padding: 16px;
	background: rgb(15 23 42 / 45%);
}

.modal-painel {
	width: min(700px, 100%);
	max-height: calc(100dvh - 32px);
	box-sizing: border-box;
	overflow-y: auto;
	padding: 24px;
	border: 1px solid #e2e8f0;
	border-radius: 10px;
	background: #fff;
	box-shadow: 0 20px 60px rgb(15 23 42 / 20%);
}

.modal-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16px;
}

.modal-header h2 {
	min-width: 0;
	overflow-wrap: anywhere;
	color: #0f172a;
	font-size: 20px;
	font-weight: 700;
}

.botao-fechar {
	display: grid;
	width: 32px;
	height: 32px;
	flex: 0 0 32px;
	place-items: center;
	border: 0;
	border-radius: 5px;
	background: transparent;
	color: #64748b;
	font-size: 22px;
	cursor: pointer;
}

.botao-fechar:hover {
	background: #f1f5f9;
	color: #0f172a;
}

.personalizacao-toolbar {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-top: 20px;
}

.personalizacao-area h3 {
	color: #334155;
	font-size: 13px;
	font-weight: 600;
}

@media (max-width: 520px) {
	.modal-overlay {
		align-items: flex-start;
		padding: 10px;
	}

	.modal-painel {
		max-height: calc(100dvh - 20px);
		padding: 18px;
	}

}
</style>