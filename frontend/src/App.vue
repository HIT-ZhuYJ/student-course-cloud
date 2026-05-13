<template>
  <div class="app">
    <aside v-if="isAuthed" class="sidebar">
      <div>
        <p class="brand">学生课程管理系统</p>
        <p class="role">{{ roleLabel }}</p>
      </div>
      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">{{ item.label }}</RouterLink>
      </nav>
      <button class="ghost-button" type="button" @click="logout">退出登录</button>
    </aside>
    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const token = computed(() => {
  route.fullPath
  return localStorage.getItem('token')
})
const role = computed(() => {
  route.fullPath
  return localStorage.getItem('role') || ''
})
const isAuthed = computed(() => Boolean(token.value))
const roleLabel = computed(() => {
  if (role.value === 'ADMIN') return '管理员'
  if (role.value === 'TEACHER') return '教师'
  if (role.value === 'STUDENT') return '学生'
  return ''
})

const navItems = computed(() => {
  if (role.value === 'ADMIN') {
    return [
      { to: '/admin/courses', label: '课程管理' },
      { to: '/admin/teachers', label: '教师管理' },
      { to: '/admin/students', label: '学生管理' },
      { to: '/admin/enrollments', label: '选课记录' }
    ]
  }
  if (role.value === 'TEACHER') {
    return [
      { to: '/teacher/courses', label: '我的任课' },
      { to: '/teacher/students', label: '选课学生' }
    ]
  }
  return [
    { to: '/student/courses', label: '课程列表' },
    { to: '/student/timetable', label: '我的课表' },
    { to: '/student/profile', label: '个人信息' }
  ]
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('tokenType')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  localStorage.removeItem('relatedId')
  router.push('/login')
}
</script>
