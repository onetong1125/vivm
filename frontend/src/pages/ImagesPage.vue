<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'

const images = ref([])
const search = ref('')
const message = ref('')
const clone = ref({ imgName: '', parentName: '' })

onMounted(load)

async function load() {
  images.value = await request(`/api/imgs/list?search=${encodeURIComponent(search.value)}`)
}

async function submit() {
  try {
    const result = await request('/api/imgs/createClone', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(clone.value)
    })
    message.value = result.message
    clone.value = { imgName: '', parentName: '' }
    await load()
  } catch (error) {
    message.value = error.message
  }
}

async function remove(imgId) {
  if (!confirm(`删除镜像 #${imgId}？`)) return
  try {
    const result = await request('/api/imgs/deleteImage', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ imgId })
    })
    message.value = result.message
    await load()
  } catch (error) {
    message.value = error.message
  }
}
</script>

<template>
  <h1>镜像</h1>
  <p v-if="message" class="message">{{ message }}</p>
  <form class="compact-form" @submit.prevent="submit">
    <h2>创建克隆镜像</h2>
    <label>镜像名称<input v-model="clone.imgName" required /></label>
    <label>父镜像名称<input v-model="clone.parentName" required /></label>
    <button>创建</button>
  </form>
  <form class="search" @submit.prevent="load"><input v-model="search" placeholder="搜索镜像名称" /><button>搜索</button></form>
  <table>
    <thead><tr><th>ID</th><th>名称</th><th>类型</th><th>创建时间</th><th>状态</th><th>操作</th></tr></thead>
    <tbody><tr v-for="image in images" :key="image.imgId"><td>{{ image.imgId }}</td><td>{{ image.imgName }}</td><td>{{ image.imgType }}</td><td>{{ image.createDate }}</td><td>{{ image.isDelete === 1 ? '已删除' : '可用' }}</td><td><button class="danger" @click="remove(image.imgId)">删除</button></td></tr></tbody>
  </table>
</template>
