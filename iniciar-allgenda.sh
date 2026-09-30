#!/bin/bash

echo "🚀 Preparando AllGenda..."

# Instalar PostgreSQL
echo "📦 Instalando PostgreSQL..."
sudo apt update -y
sudo apt install -y postgresql postgresql-contrib

# Criar banco
echo "🗄️ Configurando banco..."

mkdir -p ~/pgdata

if [ ! -d ~/pgdata/base ]; then
    /usr/lib/postgresql/16/bin/initdb -D ~/pgdata
    echo "port = 5433" >> ~/pgdata/postgresql.conf
    echo "unix_socket_directories = '/tmp'" >> ~/pgdata/postgresql.conf
fi

/usr/lib/postgresql/16/bin/pg_ctl -D ~/pgdata -l ~/postgres.log start

sleep 3

sudo -u $(whoami) psql -h /tmp -p 5433 -d postgres <<EOF
CREATE USER allgenda_user WITH PASSWORD 'alljgc';
CREATE DATABASE allgenda OWNER allgenda_user;
EOF

echo "✅ Banco configurado"


# Backend
echo "⚙️ Preparando backend..."

cd /workspaces/AllGenda/allgenda

./mvnw clean compile


# Frontend
echo "🌐 Preparando frontend..."

cd /workspaces/AllGenda/frontend

npm install


echo "✅ Tudo preparado!"
echo ""
echo "Agora rode:"
echo "BACKEND:"
echo "cd /workspaces/AllGenda/allgenda && ./mvnw spring-boot:run"
echo ""
echo "FRONTEND:"
echo "cd /workspaces/AllGenda/frontend && npm run dev"
