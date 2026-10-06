<script setup lang="ts">
import { Search } from '@lucide/vue'
</script>

<template>
  <aside class="sidebar">
    <div class="logo">
      <strong>AllGenda</strong>
      <span>Organização acadêmica</span>
    </div>

    <nav>
      <NuxtLink to="/agenda" class="nav-item">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3.5" y="3.5" width="7" height="7" rx="1.5" />
            <rect x="13.5" y="3.5" width="7" height="4.5" rx="1.5" />
            <rect x="13.5" y="13" width="7" height="7.5" rx="1.5" />
            <rect x="3.5" y="13" width="7" height="7.5" rx="1.5" />
          </svg>
        </span>
        <span>Agenda</span>
      </NuxtLink>

      <NuxtLink to="/pesquisar" class="nav-item">
        <span class="nav-icon" aria-hidden="true">
          <Search :size="16" :stroke-width="1.8" />
        </span>
        <span>Pesquisar</span>
      </NuxtLink>

      <NuxtLink to="/disciplinas" class="nav-item">
        <span class="nav-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 7.5A2.5 2.5 0 0 1 6.5 5h11A2.5 2.5 0 0 1 20 7.5v9A2.5 2.5 0 0 1 17.5 19h-11A2.5 2.5 0 0 1 4 16.5v-9Z" />
            <path d="M8 9.5h8M8 13h8" />
          </svg>
        </span>
        <span>Disciplinas</span>
      </NuxtLink>
    </nav>

    <NuxtLink to="/" class="rodape" aria-label="Sair e voltar para a tela de login">
      <span class="nav-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 7.5V6.8A3.8 3.8 0 0 1 12.8 3h.4A3.8 3.8 0 0 1 17 6.8v.7" />
          <path d="M5.5 10.5h13l-1.1 8.1A2.4 2.4 0 0 1 15 21H9a2.4 2.4 0 0 1-2.4-2.4L5.5 10.5Z" />
        </svg>
      </span>
      <span>Sair</span>
    </NuxtLink>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  min-width: 220px;
  flex-shrink: 0;
  height: auto;
  min-height: 100vh;
  align-self: stretch;
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 12px 10px 12px;
  box-sizing: border-box;
}

.logo {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 2px 6px 14px;
  padding: 8px 10px 14px;
  border-bottom: 1px solid #eef2f7;
  color: #1f2937;
}

.logo strong {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
}

.logo span {
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-item,
.rodape {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 8px 10px;
  border-radius: 8px;
  text-decoration: none;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.nav-item:hover,
.rodape:hover {
  background: #f1f5f9;
  color: #1f2937;
}

.nav-item.router-link-active {
  background: #eff6ff;
  color: #2563eb;
}

.nav-icon {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-icon svg {
  width: 16px;
  height: 16px;
  display: block;
}

.rodape {
  margin-top: auto;
  color: #475569;
  border-top: 1px solid #eef2f7;
  margin-top: auto;
  padding-top: 12px;
  cursor: pointer;
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    min-width: 100%;
    height: auto;
    min-height: 0;
    align-self: auto;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
  }

  .logo {
    display: none;
  }

  nav {
    flex: 1;
    flex-direction: row;
    gap: 8px;
  }

  .nav-item,
  .rodape {
    min-height: 36px;
    padding: 8px 10px;
  }

  .rodape {
    margin-top: 0;
    border-top: none;
    padding-top: 8px;
  }
}
</style>