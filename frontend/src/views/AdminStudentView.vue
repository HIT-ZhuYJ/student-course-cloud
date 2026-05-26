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
    <p v-if="message" class="success-text">{{ message }}</p>

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

    <div v-if="detail" class="modal-mask" @click.self="closeDetail">
      <div class="modal-panel">
        <div class="modal-header">
          <h2>学生详情</h2>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeDetail">×</button>
        </div>
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
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { register } from '../api/auth'
import { getStudent, listStudents, updateStudent } from '../api/student'
import { downloadCsv, parseCsv } from '../utils/csv'

const students = ref([])
const detail = ref(null)
const keyword = ref('')
const error = ref('')
const message = ref('')

onMounted(loadStudents)

async function loadStudents() {
  error.value = ''
  message.value = ''
  try {
    const res = await listStudents({ pageNo: 1, pageSize: 100, keyword: keyword.value || undefined })
    students.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '学生加载失败'
  }
}

async function showDetail(studentId) {
  error.value = ''
  message.value = ''
  try {
    const res = await getStudent(studentId)
    detail.value = res.data
  } catch (err) {
    error.value = err.normalizedMessage || '学生详情加载失败'
  }
}

function closeDetail() {
  detail.value = null
}

function exportStudents() {
  error.value = ''
  message.value = ''
  if (!students.value.length) {
    error.value = '当前没有可导出的学生数据'
    return
  }
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
  message.value = '学生 CSV 已导出'
}

async function importStudents(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  message.value = ''
  try {
    const rows = mergeDuplicateRows(parseCsv(await file.text()), 'studentNo')
    const existing = await fetchAllStudents()
    let created = 0
    let updated = 0
    let skipped = 0
    for (const row of rows) {
      if (!row.studentNo || !row.name || !row.major || !row.grade) {
        skipped += 1
        continue
      }
      const matched = existing.find((student) => sameText(student.studentNo, row.studentNo))
      if (matched) {
        await updateStudent(matched.studentId, {
          name: row.name,
          major: row.major,
          grade: row.grade,
          phone: row.phone || '',
          email: row.email || '',
          status: normalizeStatus(row.status, matched.status || 'ACTIVE')
        })
        Object.assign(matched, {
          studentNo: row.studentNo || matched.studentNo,
          name: row.name,
          major: row.major,
          grade: row.grade,
          phone: row.phone || '',
          email: row.email || '',
          status: normalizeStatus(row.status, matched.status || 'ACTIVE')
        })
        updated += 1
      } else {
        if (!row.username || !row.password) {
          skipped += 1
          continue
        }
        const res = await register({
          studentNo: row.studentNo,
          name: row.name,
          major: row.major,
          grade: row.grade,
          phone: row.phone || '',
          email: row.email || '',
          username: row.username,
          password: row.password
        })
        existing.push({
          studentId: res.data?.studentId,
          studentNo: row.studentNo,
          name: row.name,
          major: row.major,
          grade: row.grade,
          phone: row.phone || '',
          email: row.email || '',
          status: 'ACTIVE'
        })
        created += 1
      }
    }
    await loadStudents()
    error.value = ''
    message.value = `学生导入完成：新增 ${created}，更新 ${updated}，跳过 ${skipped}`
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '学生导入失败'
  }
}

async function fetchAllStudents() {
  const res = await listStudents({ pageNo: 1, pageSize: 100 })
  return res.data?.records || []
}

function mergeDuplicateRows(rows, key) {
  return rows.reduce((merged, row) => {
    const index = merged.findIndex((item) => sameText(item[key], row[key]))
    if (index >= 0) {
      merged[index] = { ...merged[index], ...row }
    } else {
      merged.push(row)
    }
    return merged
  }, [])
}

function normalizeStatus(value, fallback) {
  const status = String(value || fallback || 'ACTIVE').trim().toUpperCase()
  return status === 'DISABLED' ? 'DISABLED' : 'ACTIVE'
}

function sameText(left, right) {
  return String(left || '').trim() !== '' && String(left || '').trim() === String(right || '').trim()
}
</script>
