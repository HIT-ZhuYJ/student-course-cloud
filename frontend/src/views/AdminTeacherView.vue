<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>教师管理</h2>
      </div>
      <button class="secondary-button" type="button" @click="reload">刷新</button>
    </div>

    <form class="filter-row" @submit.prevent="loadTeachers">
      <input v-model.trim="filters.keyword" placeholder="教师编号、姓名或职称" />
      <select v-model="filters.status">
        <option value="">全部状态</option>
        <option value="ACTIVE">ACTIVE</option>
        <option value="DISABLED">DISABLED</option>
      </select>
      <button class="secondary-button">查询</button>
      <button class="secondary-button" type="button" @click="exportTeachers">导出CSV</button>
      <label class="file-button">
        导入CSV
        <input type="file" accept=".csv,text/csv" @change="importTeachers" />
      </label>
    </form>

    <div class="panel-grid two">
      <form class="form-panel" @submit.prevent="saveTeacher">
        <h3>{{ editingTeacherId ? '修改教师' : '新增教师' }}</h3>
        <label v-if="!editingTeacherId">教师编号<input v-model.trim="teacherForm.teacherNo" required /></label>
        <label v-if="!editingTeacherId">登录用户名<input v-model.trim="teacherForm.username" required autocomplete="off" /></label>
        <label v-if="!editingTeacherId">初始密码<input v-model.trim="teacherForm.password" type="password" required autocomplete="new-password" /></label>
        <label>姓名<input v-model.trim="teacherForm.name" required /></label>
        <label>职称<input v-model.trim="teacherForm.title" /></label>
        <label>手机号<input v-model.trim="teacherForm.phone" /></label>
        <label>邮箱<input v-model.trim="teacherForm.email" type="email" /></label>
        <label>状态<input v-model.trim="teacherForm.status" placeholder="ACTIVE / DISABLED" required /></label>
        <div class="button-row">
          <button class="primary-button compact" :disabled="savingTeacher">{{ savingTeacher ? '保存中...' : '保存教师' }}</button>
          <button v-if="editingTeacherId" class="secondary-button" type="button" @click="resetTeacherForm">取消编辑</button>
        </div>
      </form>

      <form class="form-panel" @submit.prevent="assignCourse">
        <h3>分配课程</h3>
        <label>
          教师
          <select v-model.number="assignForm.teacherId" required>
            <option disabled value="">请选择教师</option>
            <option v-for="teacher in teachers" :key="teacher.teacherId" :value="teacher.teacherId">
              {{ teacher.teacherId }} - {{ teacher.name }}（{{ teacher.status }}）
            </option>
          </select>
        </label>
        <label>
          课程
          <select v-model.number="assignForm.courseId" required>
            <option disabled value="">请选择课程</option>
            <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
              {{ course.courseId }} - {{ course.courseName }}（{{ course.status }}，已选 {{ course.selectedCount }}/{{ course.capacity }}）
            </option>
          </select>
        </label>
        <button class="primary-button compact" :disabled="assigning">{{ assigning ? '分配中...' : '分配教师' }}</button>
      </form>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>编号</th>
            <th>姓名</th>
            <th>职称</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="teacher in teachers" :key="teacher.teacherId">
            <td>{{ teacher.teacherId }}</td>
            <td>{{ teacher.teacherNo }}</td>
            <td>{{ teacher.name }}</td>
            <td>{{ teacher.title || '-' }}</td>
            <td>{{ teacher.status }}</td>
            <td class="actions">
              <button class="small-button" type="button" @click="editTeacher(teacher)">编辑</button>
              <button class="small-button warn" type="button" @click="disableTeacher(teacher.teacherId)">禁用</button>
              <button class="small-button" type="button" @click="showTeacherCourses(teacher.teacherId)">任课</button>
            </td>
          </tr>
          <tr v-if="!teachers.length"><td colspan="6">暂无教师</td></tr>
        </tbody>
      </table>
    </div>

    <section v-if="selectedTeacherId" class="sub-section">
      <h2>教师 {{ selectedTeacherId }} 任课</h2>
      <div class="table-card">
        <table>
          <thead>
            <tr><th>分配ID</th><th>课程ID</th><th>课程状态</th><th>已选人数</th><th>分配状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in teacherCourses" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.courseId }}</td>
              <td>{{ courseById(item.courseId)?.status || '-' }}</td>
              <td>{{ selectedCountText(item.courseId) }}</td>
              <td>{{ item.status }}</td>
              <td><button class="small-button warn" type="button" @click="cancelAssignment(item.teacherId, item.courseId)">取消</button></td>
            </tr>
            <tr v-if="!teacherCourses.length"><td colspan="6">暂无任课</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listCourses } from '../api/course'
import { assignTeacherCourse, cancelTeacherCourse, createTeacher, deleteTeacher, listTeacherCourses, listTeachers, updateTeacher } from '../api/teacher'

const teachers = ref([])
const courses = ref([])
const teacherCourses = ref([])
const selectedTeacherId = ref(null)
const editingTeacherId = ref(null)
const savingTeacher = ref(false)
const assigning = ref(false)
const error = ref('')
const message = ref('')

const filters = reactive({ keyword: '', status: '' })
const teacherForm = reactive({ teacherNo: '', username: '', password: '', name: '', title: '', phone: '', email: '', status: 'ACTIVE' })
const assignForm = reactive({ teacherId: '', courseId: '' })

onMounted(reload)

async function reload() {
  await Promise.all([loadTeachers(), loadCoursesForAssign()])
  if (selectedTeacherId.value) {
    await showTeacherCourses(selectedTeacherId.value)
  }
}

async function loadTeachers() {
  error.value = ''
  try {
    const res = await listTeachers({
      pageNo: 1,
      pageSize: 100,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    teachers.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '教师加载失败'
  }
}

function exportTeachers() {
  downloadCsv('teachers.csv', [
    ['teacherId', 'teacherNo', 'name', 'title', 'phone', 'email', 'status'],
    ...teachers.value.map((teacher) => [
      teacher.teacherId,
      teacher.teacherNo,
      teacher.name,
      teacher.title || '',
      teacher.phone || '',
      teacher.email || '',
      teacher.status
    ])
  ])
}

async function importTeachers(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  message.value = ''
  try {
    const rows = parseCsv(await file.text())
    let count = 0
    for (const row of rows) {
      if (!row.teacherNo || !row.username || !row.password || !row.name) continue
      await createTeacher({
        teacherNo: row.teacherNo,
        username: row.username,
        password: row.password,
        name: row.name,
        title: row.title || '',
        phone: row.phone || '',
        email: row.email || '',
        status: row.status || 'ACTIVE'
      })
      count += 1
    }
    message.value = `已导入 ${count} 名教师`
    await reload()
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '教师导入失败'
  }
}

async function loadCoursesForAssign() {
  error.value = ''
  try {
    const res = await listCourses({ pageNo: 1, pageSize: 100 })
    courses.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '课程加载失败'
  }
}

async function saveTeacher() {
  if (!teacherForm.name || (!editingTeacherId.value && (!teacherForm.teacherNo || !teacherForm.username || !teacherForm.password))) {
    error.value = '请填写教师编号、登录用户名、初始密码和姓名'
    return
  }
  savingTeacher.value = true
  error.value = ''
  message.value = ''
  try {
    if (editingTeacherId.value) {
      await updateTeacher(editingTeacherId.value, {
        name: teacherForm.name,
        title: teacherForm.title,
        phone: teacherForm.phone,
        email: teacherForm.email,
        status: teacherForm.status
      })
      message.value = '教师已更新'
    } else {
      await createTeacher({
        teacherNo: teacherForm.teacherNo,
        username: teacherForm.username,
        password: teacherForm.password,
        name: teacherForm.name,
        title: teacherForm.title,
        phone: teacherForm.phone,
        email: teacherForm.email,
        status: teacherForm.status
      })
      message.value = '教师已新增，教师账号已同步创建'
    }
    resetTeacherForm()
    await loadTeachers()
  } catch (err) {
    error.value = err.normalizedMessage || '教师保存失败'
  } finally {
    savingTeacher.value = false
  }
}

function editTeacher(teacher) {
  editingTeacherId.value = teacher.teacherId
  Object.assign(teacherForm, {
    teacherNo: teacher.teacherNo,
    name: teacher.name,
    title: teacher.title || '',
    phone: teacher.phone || '',
    email: teacher.email || '',
    status: teacher.status
  })
}

function resetTeacherForm() {
  editingTeacherId.value = null
  Object.assign(teacherForm, { teacherNo: '', username: '', password: '', name: '', title: '', phone: '', email: '', status: 'ACTIVE' })
}

async function disableTeacher(teacherId) {
  error.value = ''
  message.value = ''
  try {
    await deleteTeacher(teacherId)
    message.value = '教师已禁用'
    await reload()
  } catch (err) {
    error.value = err.normalizedMessage || '教师禁用失败'
  }
}

async function assignCourse() {
  const validationMessage = validateAssignment()
  if (validationMessage) {
    error.value = validationMessage
    return
  }
  assigning.value = true
  error.value = ''
  message.value = ''
  try {
    await assignTeacherCourse(assignForm.teacherId, assignForm.courseId)
    message.value = '教师分配成功'
    await showTeacherCourses(assignForm.teacherId)
  } catch (err) {
    error.value = err.normalizedMessage || '教师分配失败'
  } finally {
    assigning.value = false
  }
}

function validateAssignment() {
  if (!assignForm.teacherId || !assignForm.courseId) {
    return '请选择教师和课程'
  }
  const teacher = teacherById(assignForm.teacherId)
  if (!teacher) {
    return '教师不存在，请刷新后重试'
  }
  if (teacher.status !== 'ACTIVE') {
    return '该教师已禁用，不能分配课程'
  }
  const course = courseById(assignForm.courseId)
  if (!course) {
    return '课程不存在，请刷新后重试'
  }
  if (course.status === 'DISABLED') {
    return '该课程已禁用，不能分配教师'
  }
  if (Number(course.selectedCount) > 0) {
    return '该课程已有学生选课，不能调整任课教师；请先处理选课记录'
  }
  return ''
}

async function showTeacherCourses(teacherId) {
  selectedTeacherId.value = teacherId
  assignForm.teacherId = teacherId
  try {
    const res = await listTeacherCourses(teacherId)
    teacherCourses.value = res.data || []
  } catch (err) {
    error.value = err.normalizedMessage || '任课加载失败'
  }
}

async function cancelAssignment(teacherId, courseId) {
  const course = courseById(courseId)
  if (course && Number(course.selectedCount) > 0) {
    error.value = '该课程已有学生选课，不能取消任课教师'
    return
  }
  try {
    await cancelTeacherCourse(teacherId, courseId)
    message.value = '已取消分配'
    await showTeacherCourses(teacherId)
  } catch (err) {
    error.value = err.normalizedMessage || '取消分配失败'
  }
}

function teacherById(teacherId) {
  return teachers.value.find((teacher) => Number(teacher.teacherId) === Number(teacherId))
}

function courseById(courseId) {
  return courses.value.find((course) => Number(course.courseId) === Number(courseId))
}

function selectedCountText(courseId) {
  const course = courseById(courseId)
  return course ? `${course.selectedCount}/${course.capacity}` : '-'
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
