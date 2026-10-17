#!/usr/bin/env bash
# Prepara o AllGenda usando o mise (https://mise.jdx.dev).
# As ferramentas, variáveis e tarefas ficam em mise.toml.
set -euo pipefail

cd "$(dirname "$0")"

echo "🚀 Preparando AllGenda..."

# Verifica se o mise já está instalado
if ! command -v mise >/dev/null 2>&1; then
    
    # Detecta o Sistema Operacional
    OS_TYPE="$(uname -s)"
    
    if [[ "$OS_TYPE" == *"MINGW"* || "$OS_TYPE" == *"MSYS"* || "$OS_TYPE" == *"CYGWIN"* ]]; then
        echo "📦 Ambiente Windows (MinGW/MSYS) detectado. Instalando mise nativo..."
        mkdir -p "$HOME/.local/bin"
        
        # Baixa o binário oficial zipado do Windows
        curl -fsSL https://github.com -o "$HOME/.local/bin/mise.zip"
        
        # Descompacta e remove o lixo
        unzip -o "$HOME/.local/bin/mise.zip" -d "$HOME/.local/bin"
        rm "$HOME/.local/bin/mise.zip"
    else
        echo "📦 Ambiente Unix (Linux/macOS) detectado. Instalando via script oficial..."
        curl -fsSL https://mise.run | sh
    fi
    
    # Garante que o diretório de binários locais esteja no PATH desta sessão
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
