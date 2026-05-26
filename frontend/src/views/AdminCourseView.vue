<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>课程管理</h2>
        <p class="muted">课程时间支持教学周、单双周、星期、节次和上课地点。</p>
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
        <template v-if="!editingCourseId">
          <h3 class="form-subtitle">首个课程时间</h3>
          <div class="form-grid">
            <label>星期
              <select v-model.number="courseScheduleForm.weekday" required>
                <option v-for="day in weekdays" :key="day.value" :value="day.value">{{ day.label }}</option>
              </select>
            </label>
            <label>起始周<input v-model.number="courseScheduleForm.startWeek" type="number" min="1" max="30" required /></label>
            <label>结束周<input v-model.number="courseScheduleForm.endWeek" type="number" min="1" max="30" required /></label>
            <label>周次类型
              <select v-model="courseScheduleForm.weekType" required>
                <option value="ALL">每周</option>
                <option value="ODD">单周</option>
                <option value="EVEN">双周</option>
              </select>
            </label>
            <label>开始节<input v-model.number="courseScheduleForm.startSection" type="number" min="1" max="12" required @change="syncTimeBySection(courseScheduleForm)" /></label>
            <label>结束节<input v-model.number="courseScheduleForm.endSection" type="number" min="1" max="12" required @change="syncTimeBySection(courseScheduleForm)" /></label>
            <label>教室<input v-model.trim="courseScheduleForm.classroom" required /></label>
            <label>开始时间<input v-model="courseScheduleForm.startTime" type="time" step="1" required /></label>
            <label>结束时间<input v-model="courseScheduleForm.endTime" type="time" step="1" required /></label>
          </div>
        </template>
        <div class="button-row">
          <button class="primary-button compact" :disabled="savingCourse">{{ savingCourse ? '保存中...' : '保存课程' }}</button>
          <button v-if="editingCourseId" class="secondary-button" type="button" @click="resetCourseForm">取消编辑</button>
        </div>
      </form>

      <form class="form-panel" @submit.prevent="addSchedule">
        <h3>新增课程时间</h3>
        <div class="form-grid">
          <label>课程ID<input v-model.number="scheduleForm.courseId" type="number" min="1" required /></label>
          <label>星期
            <select v-model.number="scheduleForm.weekday">
              <option v-for="day in weekdays" :key="day.value" :value="day.value">{{ day.label }}</option>
            </select>
          </label>
          <label>起始周<input v-model.number="scheduleForm.startWeek" type="number" min="1" max="30" required /></label>
          <label>结束周<input v-model.number="scheduleForm.endWeek" type="number" min="1" max="30" required /></label>
          <label>周次类型
            <select v-model="scheduleForm.weekType">
              <option value="ALL">每周</option>
              <option value="ODD">单周</option>
              <option value="EVEN">双周</option>
            </select>
          </label>
          <label>开始节<input v-model.number="scheduleForm.startSection" type="number" min="1" max="12" required @change="syncTimeBySection(scheduleForm)" /></label>
          <label>结束节<input v-model.number="scheduleForm.endSection" type="number" min="1" max="12" required @change="syncTimeBySection(scheduleForm)" /></label>
          <label>教室<input v-model.trim="scheduleForm.classroom" required /></label>
          <label>开始时间<input v-model="scheduleForm.startTime" type="time" step="1" required /></label>
          <label>结束时间<input v-model="scheduleForm.endTime" type="time" step="1" required /></label>
        </div>
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

    <div v-if="selectedCourseId" class="modal-mask" @click.self="closeSchedules">
      <div class="modal-panel wide">
        <div class="modal-header">
          <h2>课程 {{ selectedCourseId }} 时间</h2>
          <button class="icon-button" type="button" aria-label="关闭" @click="closeSchedules">×</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>周次</th>
              <th>星期</th>
              <th>节次</th>
              <th>时间</th>
              <th>教室</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in schedules" :key="item.scheduleId">
              <td>{{ item.scheduleId }}</td>
              <td>第{{ item.startWeek }}-{{ item.endWeek }}周 {{ weekTypeText(item.weekType) }}</td>
              <td>{{ weekdayText(item.weekday) }}</td>
              <td>第{{ item.startSection }}-{{ item.endSection }}节</td>
              <td>{{ item.startTime }}-{{ item.endTime }}</td>
              <td>{{ item.classroom }}</td>
            </tr>
            <tr v-if="!schedules.length"><td colspan="6">暂无课程时间</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { addCourseSchedule, createCourse, deleteCourse, listCourseSchedules, listCourses, updateCourse } from '../api/course'
import { downloadCsv, parseCsv } from '../utils/csv'

const weekdays = [
  { value: 1, label: '星期一' },
  { value: 2, label: '星期二' },
  { value: 3, label: '星期三' },
  { value: 4, label: '星期四' },
  { value: 5, label: '星期五' },
  { value: 6, label: '星期六' },
  { value: 7, label: '星期日' }
]

const sectionTimes = {
  1: '08:00:00',
  2: '09:45:00',
  3: '10:00:00',
  4: '11:45:00',
  5: '13:45:00',
  6: '15:30:00',
  7: '15:45:00',
  8: '17:30:00',
  9: '18:30:00',
  10: '20:15:00',
  11: '20:30:00',
  12: '22:15:00'
}

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
const courseScheduleForm = reactive(createDefaultSchedule())
const scheduleForm = reactive({
  courseId: null,
  ...createDefaultSchedule()
})

function createDefaultSchedule() {
  return {
  startWeek: 1,
  endWeek: 16,
  weekType: 'ALL',
  weekday: 1,
  startSection: 1,
  endSection: 2,
  startTime: '08:00:00',
  endTime: '09:45:00',
  classroom: ''
  }
}

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
  error.value = ''
  message.value = ''
  if (!courses.value.length) {
    error.value = '当前没有可导出的课程数据'
    return
  }
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
  message.value = '课程 CSV 已导出'
}

async function importCourses(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  message.value = ''
  try {
    const rows = mergeDuplicateRows(parseCsv(await file.text()), 'courseCode')
    const existing = await fetchAllCourses()
    let created = 0
    let updated = 0
    let skipped = 0
    for (const row of rows) {
      if (!row.courseCode || !row.courseName) {
        skipped += 1
        continue
      }
      const schedule = buildCsvSchedule(row)
      const matched = existing.find((course) => sameText(course.courseCode, row.courseCode))
      if (matched) {
        await updateCourse(matched.courseId, {
          courseName: row.courseName,
          credit: Number(row.credit || matched.credit || 3),
          capacity: Number(row.capacity || matched.capacity || 30),
          status: normalizeCourseStatus(row.status, matched.status || 'OPEN'),
          description: row.description || ''
        })
        await addScheduleIfMissing(matched.courseId, schedule)
        Object.assign(matched, {
          courseName: row.courseName,
          credit: Number(row.credit || matched.credit || 3),
          capacity: Number(row.capacity || matched.capacity || 30),
          status: normalizeCourseStatus(row.status, matched.status || 'OPEN'),
          description: row.description || ''
        })
        updated += 1
      } else {
        const res = await createCourse({
          courseCode: row.courseCode,
          courseName: row.courseName,
          credit: Number(row.credit || 3),
          capacity: Number(row.capacity || 30),
          status: normalizeCourseStatus(row.status, 'OPEN'),
          description: row.description || '',
          schedule
        })
        existing.push({
          courseId: res.data?.courseId,
          courseCode: row.courseCode,
          courseName: row.courseName,
          credit: Number(row.credit || 3),
          capacity: Number(row.capacity || 30),
          status: normalizeCourseStatus(row.status, 'OPEN'),
          description: row.description || ''
        })
        created += 1
      }
    }
    message.value = `课程导入完成：新增 ${created}，更新 ${updated}，跳过 ${skipped}`
    await loadCourses()
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '课程导入失败'
  }
}

async function fetchAllCourses() {
  const res = await listCourses({ pageNo: 1, pageSize: 100 })
  return res.data?.records || []
}

async function addScheduleIfMissing(courseId, schedule) {
  const res = await listCourseSchedules(courseId)
  const existingSchedules = res.data || []
  if (existingSchedules.some((item) => sameSchedule(item, schedule))) {
    return
  }
  await addCourseSchedule(courseId, schedule)
}

async function saveCourse() {
  if (!courseForm.courseName || !courseForm.credit || !courseForm.capacity) {
    error.value = '请填写课程名称、学分和容量'
    return
  }
  if (!editingCourseId.value && !isValidSchedule(courseScheduleForm)) {
    error.value = '新增课程必须填写有效的首个课程时间：周次、节次、时间和教室均为必填'
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
      const created = await createCourse({
        ...courseForm,
        schedule: buildSchedulePayload(courseScheduleForm)
      })
      message.value = '课程已新增'
      if (created.data?.courseId) {
        await showSchedules(created.data.courseId)
      }
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
  Object.assign(courseScheduleForm, createDefaultSchedule())
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
  if (!isValidSchedule(scheduleForm)) {
    error.value = '请填写有效的课程时间：周次、节次和时间必须前后有序'
    return
  }
  savingSchedule.value = true
  error.value = ''
  message.value = ''
  try {
    await addCourseSchedule(scheduleForm.courseId, buildSchedulePayload(scheduleForm))
    message.value = '课程时间已新增'
    await showSchedules(scheduleForm.courseId)
  } catch (err) {
    error.value = err.normalizedMessage || '新增课程时间失败'
  } finally {
    savingSchedule.value = false
  }
}

function isValidSchedule(form) {
  return form
    && (!Object.hasOwn(form, 'courseId') || form.courseId)
    && form.classroom
    && form.startWeek >= 1
    && form.endWeek >= form.startWeek
    && form.startSection >= 1
    && form.endSection >= form.startSection
    && normalizeTime(form.startTime) < normalizeTime(form.endTime)
}

function syncTimeBySection(form) {
  form.startTime = sectionTimes[form.startSection] || form.startTime
  form.endTime = sectionTimes[form.endSection] || form.endTime
}

function buildSchedulePayload(form) {
  return {
    startWeek: form.startWeek,
    endWeek: form.endWeek,
    weekType: form.weekType,
    weekday: form.weekday,
    startSection: form.startSection,
    endSection: form.endSection,
    startTime: normalizeTime(form.startTime),
    endTime: normalizeTime(form.endTime),
    classroom: form.classroom
  }
}

function buildCsvSchedule(row) {
  const schedule = {
    startWeek: Number(row.startWeek),
    endWeek: Number(row.endWeek),
    weekType: row.weekType,
    weekday: Number(row.weekday),
    startSection: Number(row.startSection),
    endSection: Number(row.endSection),
    startTime: row.startTime,
    endTime: row.endTime,
    classroom: row.classroom
  }
  if (!isValidSchedule(schedule)) {
    throw new Error(`课程 ${row.courseCode || row.courseName} 缺少有效课程时间`)
  }
  return buildSchedulePayload(schedule)
}

function sameSchedule(left, right) {
  return Number(left.startWeek) === Number(right.startWeek)
    && Number(left.endWeek) === Number(right.endWeek)
    && String(left.weekType || 'ALL').trim().toUpperCase() === String(right.weekType || 'ALL').trim().toUpperCase()
    && Number(left.weekday) === Number(right.weekday)
    && Number(left.startSection) === Number(right.startSection)
    && Number(left.endSection) === Number(right.endSection)
    && normalizeTime(left.startTime) === normalizeTime(right.startTime)
    && normalizeTime(left.endTime) === normalizeTime(right.endTime)
    && sameText(left.classroom, right.classroom)
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

function closeSchedules() {
  selectedCourseId.value = null
  schedules.value = []
}

function normalizeTime(value) {
  if (!value) return ''
  return value.length === 5 ? `${value}:00` : value
}

function weekdayText(value) {
  return weekdays.find((item) => item.value === value)?.label || value
}

function weekTypeText(value) {
  if (value === 'ODD') return '单周'
  if (value === 'EVEN') return '双周'
  return '每周'
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

function normalizeCourseStatus(value, fallback) {
  const status = String(value || fallback || 'OPEN').trim().toUpperCase()
  return ['OPEN', 'CLOSED', 'DISABLED'].includes(status) ? status : 'OPEN'
}

function sameText(left, right) {
  return String(left || '').trim() !== '' && String(left || '').trim() === String(right || '').trim()
}
</script>
