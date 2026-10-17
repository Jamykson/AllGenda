#!/usr/bin/env bash
# Prepara o AllGenda usando o mise (https://mise.jdx.dev).
# As ferramentas, variáveis e tarefas ficam em mise.toml.
set -euo pipefail

cd "$(dirname "$0")"

echo "🚀 Preparando AllGenda..."

# 1. mise: instala no Linux/macOS; no Windows orienta a instalação.
if ! command -v mise >/dev/null 2>&1; then
    # O instalador do mise pode ter deixado o binário aqui sem alterar o PATH desta sessão.
    export PATH="$HOME/.local/bin:$PATH"
fi

if ! command -v mise >/dev/null 2>&1; then
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*)
            echo "📦 Windows detectado. O mise precisa ser instalado pelo winget (ou scoop)."
            if command -v winget >/dev/null 2>&1; then
                winget install --id jdx.mise -e \
                    --accept-package-agreements --accept-source-agreements
                echo ""
                echo "✅ mise instalado. FECHE e REABRA este terminal (Git Bash) e rode"
                echo "   ./iniciar-allgenda.sh de novo, para o PATH ser atualizado."
            else
                echo "❌ winget não encontrado. Instale o mise manualmente:"
                echo "   winget install jdx.mise   (ou: scoop install mise)"
                echo "   Depois reabra o terminal e rode ./iniciar-allgenda.sh de novo."
            fi
            exit 0
            ;;
        *)
            echo "📦 Instalando mise..."
            curl -fsSL https://mise.run | sh
            export PATH="$HOME/.local/bin:$PATH"
            ;;
    esac
fi

# 2. Ferramentas (Java, Node) e preparação do projeto.
# Não precisa de "mise activate": 'mise run' já carrega as ferramentas e o ambiente.
mise trust
mise install

if docker info >/dev/null 2>&1; then
    mise run setup
else
    echo "⚠️  Docker não está disponível: pulando o banco (db:up)."
    echo "    Use um PostgreSQL próprio na porta 5433 (veja o README)."
    mise run frontend:install
    mise run backend:compile
fi

echo "✅ Tudo preparado!"
echo ""
echo "Agora rode:"
echo "mise run dev        (com Docker)"
echo "Sem Docker: veja a seção 'Sem Docker' do README."
echo ""
echo "Dica: rode 'mise tasks' para ver todas as tarefas disponíveis."