<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'

const users = ref([])
const keyword = ref('')
const page = ref(0)
const totalPages = ref(0)
const message = ref('')
const editing = ref()

onMounted(load)

async function load(nextPage = page.value) {
  page.value = nextPage
  const result = await request(`/api/users?page=${page.value}&size=10&keyword=${encodeURIComponent(keyword.value)}`)
  users.value = result.content
  totalPages.value = result.totalPages
}

function edit(user) {
  editing.value = { ...user }
}

async function save() {
  try {
    await request(`/api/users/${editing.value.userId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editing.value)
    })
    editing.value = undefined
    message.value = '用户信息已更新'
    await load()
  } catch (error) {
    message.value = error.message
  }
}

async function remove(user) {
  if (!confirm(`删除用户 ${user.userName}？`)) return
  try {
    await request(`/api/users/${user.userId}`, { method: 'DELETE' })
    message.value = '用户已删除'
    await load()
  } catch (error) {
    message.value = error.message
  }
}
</script>

<template>
  <h1>用户管理</h1>
  <p v-if="message" class="message">{{ message }}</p>
  <form class="search" @submit.prevent="load(0)"><input v-model="keyword" placeholder="搜索用户名" /><button>搜索</button></form>
  <table><thead><tr><th>ID</th><th>用户名</th><th>邮箱</th><th>角色</th><th>余额</th><th>空间</th><th>操作</th></tr></thead><tbody><tr v-for="user in users" :key="user.userId"><td>{{ user.userId }}</td><td>{{ user.userName }}</td><td>{{ user.email }}</td><td>{{ user.role === 1 ? '管理员' : '普通用户' }}</td><td>{{ user.balance }}</td><td>{{ user.ossSpace }}</td><td><button @click="edit(user)">编辑</button><button class="danger" @click="remove(user)">删除</button></td></tr></tbody></table>
  <div class="pagination"><button :disabled="page === 0" @click="load(page - 1)">上一页</button><span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span><button :disabled="page + 1 >= totalPages" @click="load(page + 1)">下一页</button></div>
  <form v-if="editing" class="modal" @submit.prevent="save"><h2>编辑 {{ editing.userName }}</h2><label>邮箱<input v-model="editing.email" type="email" required /></label><label>角色<select v-model.number="editing.role"><option :value="0">普通用户</option><option :value="1">管理员</option></select></label><label>余额<input v-model.number="editing.balance" min="0" required type="number" /></label><label>空间<input v-model.number="editing.ossSpace" min="0" required type="number" /></label><button>保存</button><button type="button" @click="editing = undefined">取消</button></form>
</template>
