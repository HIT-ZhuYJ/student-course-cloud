<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>学生管理</h2>
        <p class="muted">查询学生列表和学生详情，数据来自 /api/students。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadStudents">刷新</button>
    </div>

    <form class="filter-row" @submit.prevent="loadStudents">
      <input v-model.trim="keyword" placeholder="学号、姓名或专业" />
      <button class="secondary-button">查询</button>
    </form>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>学号</th>
            <th>姓名</th>
            <th>专业</th>
            <th>年级</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="student in students" :key="student.studentId">
            <td>{{ student.studentId }}</td>
            <td>{{ student.studentNo }}</td>
            <td>{{ student.name }}</td>
            <td>{{ student.major }}</td>
            <td>{{ student.grade }}</td>
            <td>{{ student.status }}</td>
            <td><button class="small-button" type="button" @click="showDetail(student.studentId)">详情</button></td>
          </tr>
          <tr v-if="!students.length"><td colspan="7">暂无学生</td></tr>
        </tbody>
      </table>
    </div>

    <section v-if="detail" class="detail-panel sub-section">
      <h2>学生详情</h2>
      <div class="detail-grid">
        <div><span>ID</span><strong>{{ detail.studentId }}</strong></div>
        <div><span>学号</span><strong>{{ detail.studentNo }}</strong></div>
        <div><span>姓名</span><strong>{{ detail.name }}</strong></div>
        <div><span>专业</span><strong>{{ detail.major }}</strong></div>
        <div><span>年级</span><strong>{{ detail.grade }}</strong></div>
        <div><span>电话</span><strong>{{ detail.phone || '-' }}</strong></div>
        <div><span>邮箱</span><strong>{{ detail.email || '-' }}</strong></div>
        <div><span>状态</span><strong>{{ detail.status }}</strong></div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getStudent, listStudents } from '../api/student'

const students = ref([])
const detail = ref(null)
const keyword = ref('')
const error = ref('')

onMounted(loadStudents)

async function loadStudents() {
  error.value = ''
  try {
    const res = await listStudents({ pageNo: 1, pageSize: 100, keyword: keyword.value || undefined })
    students.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '学生加载失败'
  }
}

async function showDetail(studentId) {
  error.value = ''
  try {
    const res = await getStudent(studentId)
    detail.value = res.data
  } catch (err) {
    error.value = err.normalizedMessage || '学生详情加载失败'
  }
}
</script>
