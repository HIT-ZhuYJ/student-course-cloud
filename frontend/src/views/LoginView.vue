<template>
  <section class="auth-page">
    <form class="auth-card" @submit.prevent="submit">
      <p class="eyebrow">登录</p>
      <h1>学生课程管理系统</h1>
      <p class="muted">请使用学生、教师或管理员账号进入演示系统。</p>

      <label>
        用户名
        <input v-model.trim="form.username" autocomplete="username" required />
      </label>
      <label>
        密码
        <input v-model="form.password" type="password" autocomplete="current-password" required />
      </label>

      <p v-if="error" class="error-text">{{ error }}</p>
      <button class="primary-button" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <RouterLink class="text-link" to="/register">注册学生账号</RouterLink>
    </form>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: '',
  password: ''
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const res = await login(form)
    const data = res.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('tokenType', data.tokenType || 'Bearer')
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    if (data.relatedId !== null && data.relatedId !== undefined) {
      localStorage.setItem('relatedId', String(data.relatedId))
    } else {
      localStorage.removeItem('relatedId')
    }
    router.push('/')
  } catch (err) {
    error.value = err.normalizedMessage || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>
