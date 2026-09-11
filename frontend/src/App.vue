<script setup>
import { computed, onMounted, ref } from 'vue'
import { clearCsrfToken, request } from './api'
import ImagesPage from './pages/ImagesPage.vue'
import UsersPage from './pages/UsersPage.vue'
import VersionsPage from './pages/VersionsPage.vue'
import VmsPage from './pages/VmsPage.vue'

const currentUser = ref()
const page = ref('vms')
const loading = ref(true)
const error = ref('')
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ userName: '', email: '', userPassword: '', confirmPassword: '' })
const authMode = ref('login')

const isAdmin = computed(() => currentUser.value?.role === 1)

onMounted(loadCurrentUser)

async function loadCurrentUser() {
  loading.value = true
  try {
    currentUser.value = await request('/api/auth/me')
    page.value = isAdmin.value ? 'users' : 'vms'
  } catch (requestError) {
    if (!requestError.message.includes('401')) {
      error.value = requestError.message
    }
  } finally {
    loading.value = false
  }
}

async function login() {
  error.value = ''
  try {
    await request('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(loginForm.value)
    })
    await loadCurrentUser()
  } catch (requestError) {
    error.value = '用户名或密码错误'
  }
}

async function register() {
  error.value = ''
  try {
    await request('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(registerForm.value)
    })
    authMode.value = 'login'
    error.value = '注册成功，请登录'
  } catch (requestError) {
    error.value = requestError.message
  }
}

async function logout() {
  await request('/api/auth/logout', { method: 'POST' })
  clearCsrfToken()
  currentUser.value = undefined
  authMode.value = 'login'
}
</script>

<template>
  <main class="shell">
    <p v-if="loading">正在加载…</p>

    <section v-else-if="!currentUser" class="auth-card">
      <h1>VIVM</h1>
      <p class="hint">虚拟镜像版本管理</p>
      <form v-if="authMode === 'login'" @submit.prevent="login">
        <label>用户名<input v-model="loginForm.username" required autocomplete="username" /></label>
        <label>密码<input v-model="loginForm.password" required type="password" autocomplete="current-password" /></label>
        <button>登录</button>
      </form>
      <form v-else @submit.prevent="register">
        <label>用户名<input v-model="registerForm.userName" required /></label>
        <label>邮箱<input v-model="registerForm.email" required type="email" /></label>
        <label>密码<input v-model="registerForm.userPassword" required type="password" /></label>
        <label>确认密码<input v-model="registerForm.confirmPassword" required type="password" /></label>
        <button>注册</button>
      </form>
      <p v-if="error" class="message">{{ error }}</p>
      <button class="link" @click="authMode = authMode === 'login' ? 'register' : 'login'">
        {{ authMode === 'login' ? '没有账号？注册' : '已有账号？登录' }}
      </button>
    </section>

    <template v-else>
      <header>
        <strong>VIVM</strong>
        <span>{{ currentUser.userName }}</span>
        <button class="link" @click="logout">退出登录</button>
      </header>
      <div class="workspace">
        <nav>
          <button v-if="isAdmin" :class="{ active: page === 'users' }" @click="page = 'users'">用户管理</button>
          <button :class="{ active: page === 'vms' }" @click="page = 'vms'">虚拟机</button>
          <button :class="{ active: page === 'images' }" @click="page = 'images'">镜像</button>
          <button :class="{ active: page === 'versions' }" @click="page = 'versions'">版本</button>
        </nav>
        <section class="content">
          <UsersPage v-if="page === 'users' && isAdmin" />
          <VmsPage v-else-if="page === 'vms'" />
          <ImagesPage v-else-if="page === 'images'" />
          <VersionsPage v-else-if="page === 'versions'" />
        </section>
      </div>
    </template>
  </main>
</template>
