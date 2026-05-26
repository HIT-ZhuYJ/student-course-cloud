<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>个人信息</h2>
      </div>
      <button class="secondary-button" type="button" @click="loadProfile">刷新</button>
    </div>

    <form class="form-panel" @submit.prevent="saveProfile">
      <div class="form-grid">
        <label>学号<input v-model.trim="form.studentNo" disabled /></label>
        <label>姓名<input v-model.trim="form.name" required /></label>
        <label>专业<input v-model.trim="form.major" required /></label>
        <label>年级<input v-model.trim="form.grade" required /></label>
        <label>手机号<input v-model.trim="form.phone" /></label>
        <label>邮箱<input v-model.trim="form.email" type="email" /></label>
      </div>
      <button class="primary-button compact" :disabled="saving">{{ saving ? '保存中...' : '保存信息' }}</button>
    </form>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getStudent, updateStudent } from '../api/student'

const form = reactive({ studentId: '', studentNo: '', name: '', major: '', grade: '', phone: '', email: '', status: 'ACTIVE' })
const saving = ref(false)
const error = ref('')
const message = ref('')

onMounted(loadProfile)

async function loadProfile(options = {}) {
  error.value = ''
  if (!options.keepMessage) {
    message.value = ''
  }
  try {
    const studentId = Number(localStorage.getItem('relatedId'))
    const res = await getStudent(studentId)
    Object.assign(form, {
      studentId: res.data.studentId,
      studentNo: res.data.studentNo,
      name: res.data.name,
      major: res.data.major,
      grade: res.data.grade,
      phone: res.data.phone || '',
      email: res.data.email || '',
      status: res.data.status
    })
  } catch (err) {
    error.value = err.normalizedMessage || '个人信息加载失败'
  }
}

async function saveProfile() {
  saving.value = true
  error.value = ''
  message.value = ''
  try {
    await updateStudent(form.studentId, {
      name: form.name,
      major: form.major,
      grade: form.grade,
      phone: form.phone,
      email: form.email,
      status: form.status
    })
    message.value = '个人信息已更新'
    await loadProfile({ keepMessage: true })
  } catch (err) {
    error.value = err.normalizedMessage || '个人信息保存失败'
  } finally {
    saving.value = false
  }
}
</script>
