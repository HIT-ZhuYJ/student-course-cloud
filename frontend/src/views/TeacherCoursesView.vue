<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">教师端</p>
        <h1>我的任课</h1>
        <p class="muted">查看管理员已分配给当前教师的课程。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadCourses">刷新</button>
    </header>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>分配ID</th>
            <th>课程ID</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in courses" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.courseId }}</td>
            <td>{{ item.status }}</td>
          </tr>
          <tr v-if="!courses.length">
            <td colspan="3">暂无任课信息</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listTeacherCourses } from '../api/teacher'

const courses = ref([])
const error = ref('')

onMounted(loadCourses)

async function loadCourses() {
  error.value = ''
  try {
    const res = await listTeacherCourses(localStorage.getItem('relatedId'))
    courses.value = res.data || []
  } catch (err) {
    error.value = err.normalizedMessage || '任课加载失败'
  }
}
</script>
