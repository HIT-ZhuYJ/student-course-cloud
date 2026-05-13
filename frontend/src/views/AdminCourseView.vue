<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>课程管理</h2>
      </div>
      <button class="secondary-button" type="button" @click="loadCourses">刷新</button>
    </div>

    <form class="filter-row" @submit.prevent="loadCourses">
      <input v-model.trim="filters.keyword" placeholder="课程编码或名称" />
      <select v-model="filters.status">
        <option value="">全部状态</option>
        <option value="OPEN">OPEN</option>
        <option value="CLOSED">CLOSED</option>
        <option value="DISABLED">DISABLED</option>
      </select>
      <button class="secondary-button">查询</button>
      <button class="secondary-button" type="button" @click="exportCourses">导出CSV</button>
      <label class="file-button">
        导入CSV
        <input type="file" accept=".csv,text/csv" @change="importCourses" />
      </label>
    </form>

    <div class="panel-grid two">
      <form class="form-panel" @submit.prevent="saveCourse">
        <h3>{{ editingCourseId ? '修改课程' : '新增课程' }}</h3>
        <label v-if="!editingCourseId">课程编码<input v-model.trim="courseForm.courseCode" required /></label>
        <label>课程名称<input v-model.trim="courseForm.courseName" required /></label>
        <label>学分<input v-model.number="courseForm.credit" type="number" min="0.5" step="0.5" required /></label>
        <label>容量<input v-model.number="courseForm.capacity" type="number" min="1" required /></label>
        <label>状态<input v-model.trim="courseForm.status" placeholder="OPEN / CLOSED / DISABLED" required /></label>
        <label>描述<input v-model.trim="courseForm.description" /></label>
        <div class="button-row">
          <button class="primary-button compact" :disabled="savingCourse">{{ savingCourse ? '保存中...' : '保存课程' }}</button>
          <button v-if="editingCourseId" class="secondary-button" type="button" @click="resetCourseForm">取消编辑</button>
        </div>
      </form>

      <form class="form-panel" @submit.prevent="addSchedule">
        <h3>新增课程时间</h3>
        <label>课程ID<input v-model.number="scheduleForm.courseId" type="number" min="1" required /></label>
        <label>星期<input v-model.number="scheduleForm.weekday" type="number" min="1" max="7" required /></label>
        <label>开始时间<input v-model="scheduleForm.startTime" type="time" step="1" required /></label>
        <label>结束时间<input v-model="scheduleForm.endTime" type="time" step="1" required /></label>
        <label>教室<input v-model.trim="scheduleForm.classroom" required /></label>
        <button class="primary-button compact" :disabled="savingSchedule">{{ savingSchedule ? '提交中...' : '新增时间' }}</button>
      </form>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>编码</th>
            <th>名称</th>
            <th>学分</th>
            <th>容量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.courseId">
            <td>{{ course.courseId }}</td>
            <td>{{ course.courseCode }}</td>
            <td>{{ course.courseName }}</td>
            <td>{{ course.credit }}</td>
            <td>{{ course.selectedCount }}/{{ course.capacity }}</td>
            <td>{{ course.status }}</td>
            <td class="actions">
              <button class="small-button" type="button" @click="editCourse(course)">编辑</button>
              <button class="small-button warn" type="button" @click="disableCourse(course.courseId)">禁用</button>
              <button class="small-button" type="button" @click="showSchedules(course.courseId)">时间</button>
            </td>
          </tr>
          <tr v-if="!courses.length"><td colspan="7">暂无课程</td></tr>
        </tbody>
      </table>
    </div>

    <section v-if="selectedCourseId" class="sub-section">
      <h2>课程 {{ selectedCourseId }} 时间</h2>
      <div class="table-card">
        <table>
          <thead>
            <tr><th>ID</th><th>星期</th><th>开始</th><th>结束</th><th>教室</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in schedules" :key="item.scheduleId">
              <td>{{ item.scheduleId }}</td>
              <td>{{ item.weekday }}</td>
              <td>{{ item.startTime }}</td>
              <td>{{ item.endTime }}</td>
              <td>{{ item.classroom }}</td>
            </tr>
            <tr v-if="!schedules.length"><td colspan="5">暂无课程时间</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { addCourseSchedule, createCourse, deleteCourse, listCourseSchedules, listCourses, updateCourse } from '../api/course'

const courses = ref([])
const schedules = ref([])
const selectedCourseId = ref(null)
const editingCourseId = ref(null)
const savingCourse = ref(false)
const savingSchedule = ref(false)
const error = ref('')
const message = ref('')

const filters = reactive({ keyword: '', status: '' })
const courseForm = reactive({ courseCode: '', courseName: '', credit: 3, capacity: 30, status: 'OPEN', description: '' })
const scheduleForm = reactive({ courseId: null, weekday: 1, startTime: '08:00:00', endTime: '09:40:00', classroom: '' })

onMounted(loadCourses)

async function loadCourses() {
  error.value = ''
  try {
    const res = await listCourses({
      pageNo: 1,
      pageSize: 100,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    courses.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '课程加载失败'
  }
}

function exportCourses() {
  downloadCsv('courses.csv', [
    ['courseId', 'courseCode', 'courseName', 'credit', 'capacity', 'selectedCount', 'status', 'description'],
    ...courses.value.map((course) => [
      course.courseId,
      course.courseCode,
      course.courseName,
      course.credit,
      course.capacity,
      course.selectedCount,
      course.status,
      course.description || ''
    ])
  ])
}

async function importCourses(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  message.value = ''
  try {
    const rows = parseCsv(await file.text())
    let count = 0
    for (const row of rows) {
      if (!row.courseCode || !row.courseName) continue
      await createCourse({
        courseCode: row.courseCode,
        courseName: row.courseName,
        credit: Number(row.credit || 3),
        capacity: Number(row.capacity || 30),
        status: row.status || 'OPEN',
        description: row.description || ''
      })
      count += 1
    }
    message.value = `已导入 ${count} 门课程`
    await loadCourses()
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '课程导入失败'
  }
}

async function saveCourse() {
  if (!courseForm.courseName || !courseForm.credit || !courseForm.capacity) {
    error.value = '请填写课程名称、学分和容量'
    return
  }
  savingCourse.value = true
  error.value = ''
  message.value = ''
  try {
    if (editingCourseId.value) {
      await updateCourse(editingCourseId.value, {
        courseName: courseForm.courseName,
        credit: courseForm.credit,
        capacity: courseForm.capacity,
        status: courseForm.status,
        description: courseForm.description
      })
      message.value = '课程已更新'
    } else {
      await createCourse(courseForm)
      message.value = '课程已新增'
    }
    resetCourseForm()
    await loadCourses()
  } catch (err) {
    error.value = err.normalizedMessage || '课程保存失败'
  } finally {
    savingCourse.value = false
  }
}

function editCourse(course) {
  editingCourseId.value = course.courseId
  Object.assign(courseForm, {
    courseCode: course.courseCode,
    courseName: course.courseName,
    credit: Number(course.credit),
    capacity: course.capacity,
    status: course.status,
    description: course.description || ''
  })
}

function resetCourseForm() {
  editingCourseId.value = null
  Object.assign(courseForm, { courseCode: '', courseName: '', credit: 3, capacity: 30, status: 'OPEN', description: '' })
}

async function disableCourse(courseId) {
  error.value = ''
  message.value = ''
  try {
    await deleteCourse(courseId)
    message.value = '课程已禁用'
    await loadCourses()
  } catch (err) {
    error.value = err.normalizedMessage || '课程禁用失败'
  }
}

async function addSchedule() {
  if (!scheduleForm.courseId || !scheduleForm.classroom || scheduleForm.startTime >= scheduleForm.endTime) {
    error.value = '请填写有效课程时间'
    return
  }
  savingSchedule.value = true
  error.value = ''
  message.value = ''
  try {
    await addCourseSchedule(scheduleForm.courseId, {
      weekday: scheduleForm.weekday,
      startTime: normalizeTime(scheduleForm.startTime),
      endTime: normalizeTime(scheduleForm.endTime),
      classroom: scheduleForm.classroom
    })
    message.value = '课程时间已新增'
    await showSchedules(scheduleForm.courseId)
  } catch (err) {
    error.value = err.normalizedMessage || '新增课程时间失败'
  } finally {
    savingSchedule.value = false
  }
}

async function showSchedules(courseId) {
  selectedCourseId.value = courseId
  scheduleForm.courseId = courseId
  try {
    const res = await listCourseSchedules(courseId)
    schedules.value = res.data || []
  } catch (err) {
    error.value = err.normalizedMessage || '课程时间加载失败'
  }
}

function normalizeTime(value) {
  return value.length === 5 ? `${value}:00` : value
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
