#!/usr/bin/env bash
# Prepara o AllGenda usando o mise (https://mise.jdx.dev).
# As ferramentas, variáveis e tarefas ficam em mise.toml.
set -euo pipefail

cd "$(dirname "$0")"

echo "🚀 Preparando AllGenda..."

if ! command -v mise >/dev/null 2>&1; then
    echo "📦 Instalando mise..."
    curl -fsSL https://mise.run | sh
    export PATH="$HOME/.local/bin:$PATH"
fi

mise trust
mise install
mise run setup

echo "✅ Tudo preparado!"
echo ""
echo "Agora rode:"
echo "mise run dev"
echo ""
echo "Dica: rode 'mise tasks' para ver todas as tarefas disponíveis."
