<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>选课记录</h2>
        <p class="muted">展示 enrollment-service 中的全部选课记录，可按学生、课程和状态筛选。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadEnrollments">刷新</button>
    </div>

    <form class="filter-row" @submit.prevent="loadEnrollments">
      <input v-model.number="filters.studentId" type="number" min="1" placeholder="学生ID" />
      <input v-model.number="filters.courseId" type="number" min="1" placeholder="课程ID" />
      <input v-model.trim="filters.status" placeholder="ACTIVE / DROPPED" />
      <button class="secondary-button">查询</button>
    </form>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>记录ID</th>
            <th>学生ID</th>
            <th>课程ID</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in enrollments" :key="item.enrollmentId">
            <td>{{ item.enrollmentId }}</td>
            <td>{{ item.studentId }}</td>
            <td>{{ item.courseId }}</td>
            <td>{{ item.status }}</td>
            <td>{{ formatTime(item.createTime) }}</td>
            <td>{{ formatTime(item.updateTime) }}</td>
          </tr>
          <tr v-if="!enrollments.length"><td colspan="6">暂无选课记录</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listEnrollments } from '../api/enrollment'

const enrollments = ref([])
const error = ref('')
const filters = reactive({ studentId: null, courseId: null, status: '' })

onMounted(loadEnrollments)

async function loadEnrollments() {
  error.value = ''
  try {
    const res = await listEnrollments({
      pageNo: 1,
      pageSize: 100,
      studentId: filters.studentId || undefined,
      courseId: filters.courseId || undefined,
      status: filters.status || undefined
    })
    enrollments.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '选课记录加载失败'
  }
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}
</script>
