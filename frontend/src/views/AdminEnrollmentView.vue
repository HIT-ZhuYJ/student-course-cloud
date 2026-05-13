<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>选课记录</h2>
      </div>
      <button class="secondary-button" type="button" @click="loadEnrollments">刷新</button>
    </div>

    <form class="filter-row" @submit.prevent="loadEnrollments">
      <input v-model.number="filters.studentId" type="number" min="1" placeholder="学生ID" />
      <input v-model.number="filters.courseId" type="number" min="1" placeholder="课程ID" />
      <input v-model.trim="filters.status" placeholder="ACTIVE / DROPPED" />
      <button class="secondary-button">查询</button>
      <button class="secondary-button" type="button" @click="exportEnrollments">导出CSV</button>
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

function exportEnrollments() {
  downloadCsv('enrollments.csv', [
    ['enrollmentId', 'studentId', 'courseId', 'status', 'createTime', 'updateTime'],
    ...enrollments.value.map((item) => [
      item.enrollmentId,
      item.studentId,
      item.courseId,
      item.status,
      formatTime(item.createTime),
      formatTime(item.updateTime)
    ])
  ])
}

function downloadCsv(filename, rows) {
  const csv = rows.map((row) => row.map((value) => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
</script>
