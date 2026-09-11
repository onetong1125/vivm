<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'

const versions = ref([])
const pools = ref([])
const search = ref('')
const message = ref('')
const create = ref({ poolName: '', versionName: '', versionDescription: '' })
const restore = ref({ poolName: '', versionId: '' })

onMounted(async () => {
  pools.value = await request('/api/version/pools')
  await load()
})

async function load() {
  versions.value = await request(`/api/version/list?search=${encodeURIComponent(search.value)}`)
}

async function submit(path, form) {
  try {
    const result = await request(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(form)
    })
    message.value = result.message
    await load()
  } catch (error) {
    message.value = error.message
  }
}
</script>

<template>
  <h1>版本</h1>
  <p v-if="message" class="message">{{ message }}</p>
  <div class="forms">
    <form @submit.prevent="submit('/api/version/createBackup', create)">
      <h2>创建备份</h2>
      <label>存储池<select v-model="create.poolName" required><option disabled value="">请选择</option><option v-for="pool in pools" :key="pool.id" :value="pool.name">{{ pool.name }}</option></select></label>
      <label>版本名称<input v-model="create.versionName" required /></label>
      <label>描述<input v-model="create.versionDescription" required /></label>
      <button>创建</button>
    </form>
    <form @submit.prevent="submit('/api/version/restoreBackup', restore)">
      <h2>恢复备份</h2>
      <label>存储池<select v-model="restore.poolName" required><option disabled value="">请选择</option><option v-for="pool in pools" :key="pool.id" :value="pool.name">{{ pool.name }}</option></select></label>
      <label>版本 ID<input v-model="restore.versionId" required type="number" /></label>
      <button>恢复</button>
    </form>
  </div>
  <form class="search" @submit.prevent="load"><input v-model="search" placeholder="搜索版本" /><button>搜索</button></form>
  <table><thead><tr><th>ID</th><th>名称</th><th>存储池</th><th>描述</th><th>创建时间</th></tr></thead><tbody><tr v-for="version in versions" :key="version.id"><td>{{ version.id }}</td><td>{{ version.name }}</td><td>{{ version.poolName }}</td><td>{{ version.description }}</td><td>{{ version.createTime }}</td></tr></tbody></table>
</template>
