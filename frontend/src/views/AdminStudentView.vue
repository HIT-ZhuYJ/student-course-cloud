<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>学生管理</h2>
      </div>
      <button class="secondary-button" type="button" @click="loadStudents">刷新</button>
    </div>

    <form class="filter-row" @submit.prevent="loadStudents">
      <input v-model.trim="keyword" placeholder="学号、姓名或专业" />
      <button class="secondary-button">查询</button>
      <button class="secondary-button" type="button" @click="exportStudents">导出CSV</button>
      <label class="file-button">
        导入CSV
        <input type="file" accept=".csv,text/csv" @change="importStudents" />
      </label>
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
import { register } from '../api/auth'
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

function exportStudents() {
  downloadCsv('students.csv', [
    ['studentId', 'studentNo', 'name', 'major', 'grade', 'phone', 'email', 'status'],
    ...students.value.map((student) => [
      student.studentId,
      student.studentNo,
      student.name,
      student.major,
      student.grade,
      student.phone || '',
      student.email || '',
      student.status
    ])
  ])
}

async function importStudents(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  try {
    const rows = parseCsv(await file.text())
    let count = 0
    for (const row of rows) {
      if (!row.studentNo || !row.name || !row.major || !row.grade || !row.username || !row.password) continue
      await register({
        studentNo: row.studentNo,
        name: row.name,
        major: row.major,
        grade: row.grade,
        phone: row.phone || '',
        email: row.email || '',
        username: row.username,
        password: row.password
      })
      count += 1
    }
    await loadStudents()
    error.value = ''
    alert(`已导入 ${count} 名学生`)
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '学生导入失败'
  }
}

function parseCsv(text) {
  const lines = text.trim().split(/\r?\n/).filter(Boolean)
  if (lines.length < 2) return []
  const headers = lines[0].split(',').map((item) => item.trim())
  return lines.slice(1).map((line) => {
    const values = line.split(',').map((item) => item.trim())
    return Object.fromEntries(headers.map((header, index) => [header, values[index] || '']))
  })
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
