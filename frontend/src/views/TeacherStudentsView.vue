<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">教师端</p>
        <h1>选课学生</h1>
      </div>
      <button class="secondary-button" type="button" @click="reload">刷新</button>
    </header>

    <form class="filter-row" @submit.prevent="loadStudents">
      <select v-model.number="selectedCourseId" required @change="loadStudents">
        <option disabled value="">请选择课程</option>
        <option v-for="item in courses" :key="item.id" :value="item.courseId">
          课程 {{ item.courseId }}（{{ item.status }}）
        </option>
      </select>
      <button class="secondary-button" :disabled="loading">查询</button>
    </form>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>选课记录ID</th>
            <th>学生ID</th>
            <th>学号</th>
            <th>姓名</th>
            <th>专业</th>
            <th>年级</th>
            <th>课程ID</th>
            <th>状态</th>
            <th>选课时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in students" :key="item.enrollmentId">
            <td>{{ item.enrollmentId }}</td>
            <td>{{ item.studentId }}</td>
            <td>{{ item.studentNo || '-' }}</td>
            <td>{{ item.studentName || '-' }}</td>
            <td>{{ item.major || '-' }}</td>
            <td>{{ item.grade || '-' }}</td>
            <td>{{ item.courseId }}</td>
            <td>{{ item.enrollmentStatus }}</td>
            <td>{{ formatTime(item.enrollmentTime) }}</td>
          </tr>
          <tr v-if="!students.length">
            <td colspan="9">暂无学生选课</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listTeacherCourseStudents } from '../api/enrollment'
import { listTeacherCourses } from '../api/teacher'

const courses = ref([])
const students = ref([])
const selectedCourseId = ref('')
const loading = ref(false)
const error = ref('')

onMounted(reload)

async function reload() {
  error.value = ''
  students.value = []
  try {
    const res = await listTeacherCourses(localStorage.getItem('relatedId'))
    courses.value = res.data || []
    if (!selectedCourseId.value && courses.value.length) {
      selectedCourseId.value = courses.value[0].courseId
    }
    if (selectedCourseId.value) {
      await loadStudents()
    }
  } catch (err) {
    error.value = err.normalizedMessage || '任课课程加载失败'
  }
}

async function loadStudents() {
  if (!selectedCourseId.value) {
    students.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await listTeacherCourseStudents(localStorage.getItem('relatedId'), selectedCourseId.value)
    students.value = res.data || []
  } catch (err) {
    error.value = err.normalizedMessage || '选课学生加载失败'
  } finally {
    loading.value = false
  }
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}
</script>
