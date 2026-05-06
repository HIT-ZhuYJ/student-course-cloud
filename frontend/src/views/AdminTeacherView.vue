<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>教师管理</h2>
        <p class="muted">维护教师信息，并为课程分配教师。教师服务只记录 courseId，不直接访问课程库。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadTeachers">刷新</button>
    </div>

    <div class="panel-grid two">
      <form class="form-panel" @submit.prevent="saveTeacher">
        <h3>{{ editingTeacherId ? '修改教师' : '新增教师' }}</h3>
        <label v-if="!editingTeacherId">教师编号<input v-model.trim="teacherForm.teacherNo" required /></label>
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
        <label>教师ID<input v-model.number="assignForm.teacherId" type="number" min="1" required /></label>
        <label>课程ID<input v-model.number="assignForm.courseId" type="number" min="1" required /></label>
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
            <tr><th>分配ID</th><th>课程ID</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in teacherCourses" :key="item.id">
              <td>{{ item.id }}</td>
              <td>{{ item.courseId }}</td>
              <td>{{ item.status }}</td>
              <td><button class="small-button warn" type="button" @click="cancelAssignment(item.teacherId, item.courseId)">取消</button></td>
            </tr>
            <tr v-if="!teacherCourses.length"><td colspan="4">暂无任课</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { assignTeacherCourse, cancelTeacherCourse, createTeacher, deleteTeacher, listTeacherCourses, listTeachers, updateTeacher } from '../api/teacher'

const teachers = ref([])
const teacherCourses = ref([])
const selectedTeacherId = ref(null)
const editingTeacherId = ref(null)
const savingTeacher = ref(false)
const assigning = ref(false)
const error = ref('')
const message = ref('')

const teacherForm = reactive({ teacherNo: '', name: '', title: '', phone: '', email: '', status: 'ACTIVE' })
const assignForm = reactive({ teacherId: null, courseId: null })

onMounted(loadTeachers)

async function loadTeachers() {
  error.value = ''
  try {
    const res = await listTeachers({ pageNo: 1, pageSize: 100 })
    teachers.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '教师加载失败'
  }
}

async function saveTeacher() {
  if (!teacherForm.name || (!editingTeacherId.value && !teacherForm.teacherNo)) {
    error.value = '请填写教师编号和姓名'
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
      await createTeacher(teacherForm)
      message.value = '教师已新增'
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
  Object.assign(teacherForm, { teacherNo: '', name: '', title: '', phone: '', email: '', status: 'ACTIVE' })
}

async function disableTeacher(teacherId) {
  error.value = ''
  message.value = ''
  try {
    await deleteTeacher(teacherId)
    message.value = '教师已禁用'
    await loadTeachers()
  } catch (err) {
    error.value = err.normalizedMessage || '教师禁用失败'
  }
}

async function assignCourse() {
  if (!assignForm.teacherId || !assignForm.courseId) {
    error.value = '请填写教师ID和课程ID'
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
  try {
    await cancelTeacherCourse(teacherId, courseId)
    message.value = '已取消分配'
    await showTeacherCourses(teacherId)
  } catch (err) {
    error.value = err.normalizedMessage || '取消分配失败'
  }
}
</script>
