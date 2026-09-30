import type { Disciplina } from '~/types/disciplina'


export function useDisciplinas(){

const config = useRuntimeConfig()


async function listar(){

return await $fetch<Disciplina[]>(
`${config.public.apiBase}/api/disciplinas`
)

}


return {
listar
}


}