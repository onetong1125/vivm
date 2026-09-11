<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../api'

const vms = ref([])
const search = ref('')
const message = ref('')
const model = ref({ vmName: '', size: '', isoFileName: '' })
const clone = ref({ vmName: '', imgId: '' })

onMounted(load)

async function load() {
  vms.value = await request(`/api/vms/list?search=${encodeURIComponent(search.value)}`)
}

async function submitModel() {
  await command('/api/vms/createModel', model.value)
  model.value = { vmName: '', size: '', isoFileName: '' }
}

async function submitClone() {
  await command('/api/vms/createClone', clone.value)
  clone.value = { vmName: '', imgId: '' }
}

async function operate(vmName, operation) {
  await command(`/api/vms/${encodeURIComponent(vmName)}/${operation}`, undefined, 'POST')
}

async function remove(vmName) {
  if (confirm(`删除虚拟机 ${vmName}？`)) {
    await command(`/api/vms/${encodeURIComponent(vmName)}/delete`, undefined, 'DELETE')
  }
}

async function command(path, data, method = 'POST') {
  try {
    const result = await request(path, {
      method,
      headers: data ? { 'Content-Type': 'application/x-www-form-urlencoded' } : undefined,
      body: data ? new URLSearchParams(data) : undefined
    })
    message.value = result?.message || '操作成功'
    await load()
  } catch (error) {
    message.value = error.message
  }
}
</script>

<template>
  <h1>虚拟机</h1>
  <p v-if="message" class="message">{{ message }}</p>
  <div class="forms">
    <form @submit.prevent="submitModel">
      <h2>创建模板虚拟机</h2>
      <label>名称<input v-model="model.vmName" required /></label>
      <label>磁盘大小（GB）<input v-model="model.size" min="1" required type="number" /></label>
      <label>ISO 文件名<input v-model="model.isoFileName" required /></label>
      <button>创建</button>
    </form>
    <form @submit.prevent="submitClone">
      <h2>克隆虚拟机</h2>
      <label>名称<input v-model="clone.vmName" required /></label>
      <label>镜像 ID<input v-model="clone.imgId" required type="number" /></label>
      <button>创建</button>
    </form>
  </div>
  <form class="search" @submit.prevent="load">
    <input v-model="search" placeholder="搜索虚拟机名称" />
    <button>搜索</button>
  </form>
  <table>
    <thead><tr><th>ID</th><th>名称</th><th>类型</th><th>状态</th><th>大小</th><th>创建时间</th><th>操作</th></tr></thead>
    <tbody>
      <tr v-for="vm in vms" :key="vm.vmId">
        <td>{{ vm.vmId }}</td><td>{{ vm.vmName }}</td><td>{{ vm.vmType }}</td>
        <td>{{ vm.vmStatus === 1 ? '运行中' : '已关闭' }}</td>
        <td>{{ Math.round(vm.vmSize / 1024 / 1024 / 1024) }} GB</td><td>{{ vm.vmCreateTime }}</td>
        <td class="actions"><button @click="operate(vm.vmName, 'start')">启动</button><button @click="operate(vm.vmName, 'shutdown')">关闭</button><button @click="operate(vm.vmName, 'restart')">重启</button><button class="danger" @click="remove(vm.vmName)">删除</button></td>
      </tr>
    </tbody>
  </table>
</template>
