<template>
  <section class="auth-page">
    <form class="auth-card wide" @submit.prevent="submit">
      <p class="eyebrow">学生注册</p>
      <h1>创建学生账号</h1>
      <p class="muted">注册成功后返回登录页，用新账号进入学生端。</p>

      <div class="form-grid">
        <label>学号<input v-model.trim="form.studentNo" required /></label>
        <label>姓名<input v-model.trim="form.name" required /></label>
        <label>专业<input v-model.trim="form.major" required /></label>
        <label>年级<input v-model.trim="form.grade" required /></label>
        <label>手机号<input v-model.trim="form.phone" /></label>
        <label>邮箱<input v-model.trim="form.email" type="email" /></label>
        <label>用户名<input v-model.trim="form.username" required /></label>
        <label>密码<input v-model="form.password" type="password" required /></label>
      </div>

      <p v-if="message" class="success-text">{{ message }}</p>
      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="primary-button" :disabled="loading">
        {{ loading ? '提交中...' : '注册' }}
      </button>
      <RouterLink class="text-link" to="/login">返回登录</RouterLink>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { register } from '../api/auth'

const loading = ref(false)
const error = ref('')
const message = ref('')
const form = reactive({
  studentNo: '',
  name: '',
  major: '',
  grade: '',
  phone: '',
  email: '',
  username: '',
  password: ''
})

async function submit() {
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    await register(form)
    message.value = '注册成功，请返回登录'
  } catch (err) {
    error.value = err.normalizedMessage || '注册失败，请检查表单信息'
  } finally {
    loading.value = false
  }
}
</script>
