<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>我的课表</h2>
        <p class="muted">可查看全部课程安排，也可按教学周筛选当前周课表。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadData()">刷新</button>
    </div>

    <div class="filter-row">
      <button
        class="secondary-button"
        type="button"
        :class="{ active: mode === 'all' }"
        @click="switchMode('all')"
      >
        总课表
      </button>
      <button
        class="secondary-button"
        type="button"
        :class="{ active: mode === 'week' }"
        @click="switchMode('week')"
      >
        周课表
      </button>
      <label class="inline-field">
        教学周
        <input v-model.number="weekNo" type="number" min="1" max="30" :disabled="mode !== 'week'" @change="loadData()" />
      </label>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>

    <div class="timetable-card">
      <div class="timetable-title">
        {{ mode === 'all' ? '总课表' : `第 ${weekNo} 周课表` }}
      </div>
      <div class="timetable-grid">
        <div class="grid-head time-head">节次</div>
        <div v-for="day in weekdays" :key="day.value" class="grid-head">{{ day.label }}</div>

        <template v-for="slot in sections" :key="slot.key">
          <div class="time-cell">
            <strong>第{{ slot.label }}节</strong>
            <span>{{ slot.time }}</span>
          </div>
          <div v-for="day in weekdays" :key="`${slot.key}-${day.value}`" class="course-cell">
            <article v-for="item in cellCourses(slot, day.value)" :key="`${item.enrollmentId}-${item.scheduleId}`" class="course-block">
              <strong>{{ item.courseName || `课程 ${item.courseId}` }}</strong>
              <span>{{ item.courseCode || `ID ${item.courseId}` }}</span>
              <span>{{ weekText(item) }} {{ weekTypeText(item.weekType) }}</span>
              <span>第{{ item.startSection }}-{{ item.endSection }}节 {{ item.startTime }}-{{ item.endTime }}</span>
              <span>{{ item.classroom }}</span>
            </article>
          </div>
        </template>
      </div>
    </div>

    <h2>选课记录</h2>
    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>记录ID</th>
            <th>课程ID</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in enrollments" :key="item.enrollmentId">
            <td>{{ item.enrollmentId }}</td>
            <td>{{ item.courseId }}</td>
            <td>{{ item.status }}</td>
            <td>
              <button
                class="small-button"
                type="button"
                :disabled="item.status !== 'ACTIVE' || droppingId === item.enrollmentId"
                @click="drop(item.enrollmentId)"
              >
                {{ droppingId === item.enrollmentId ? '处理中...' : '退课' }}
              </button>
            </td>
          </tr>
          <tr v-if="!enrollments.length">
            <td colspan="4">暂无选课记录</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dropEnrollment, getStudentTimetable, listStudentEnrollments } from '../api/enrollment'

const weekdays = [
  { value: 1, label: '星期一' },
  { value: 2, label: '星期二' },
  { value: 3, label: '星期三' },
  { value: 4, label: '星期四' },
  { value: 5, label: '星期五' },
  { value: 6, label: '星期六' },
  { value: 7, label: '星期日' }
]

const sections = [
  { key: '1-2', start: 1, end: 2, label: '1,2', time: '08:00-09:45' },
  { key: '3-4', start: 3, end: 4, label: '3,4', time: '10:00-11:45' },
  { key: '5-6', start: 5, end: 6, label: '5,6', time: '13:45-15:30' },
  { key: '7-8', start: 7, end: 8, label: '7,8', time: '15:45-17:30' },
  { key: '9-10', start: 9, end: 10, label: '9,10', time: '18:30-20:15' },
  { key: '11-12', start: 11, end: 12, label: '11,12', time: '20:30-22:15' }
]

const timetable = ref([])
const enrollments = ref([])
const error = ref('')
const message = ref('')
const droppingId = ref(null)
const mode = ref('all')
const weekNo = ref(1)

const sortedTimetable = computed(() => {
  return [...timetable.value].sort((a, b) => {
    return a.weekday - b.weekday || a.startSection - b.startSection || a.startWeek - b.startWeek
  })
})

onMounted(() => loadData())

async function loadData(clearNotice = true) {
  error.value = ''
  if (clearNotice) {
    message.value = ''
  }
  try {
    const studentId = Number(localStorage.getItem('relatedId'))
    if (!studentId) {
      throw new Error('未获取到当前学生身份，请重新登录')
    }
    const timetableParams = mode.value === 'week' ? { weekNo: weekNo.value } : {}
    const [scheduleRes, enrollmentRes] = await Promise.all([
      getStudentTimetable(studentId, timetableParams),
      listStudentEnrollments(studentId)
    ])
    timetable.value = scheduleRes.data || []
    enrollments.value = enrollmentRes.data || []
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '课表加载失败'
  }
}

function switchMode(nextMode) {
  mode.value = nextMode
  loadData()
}

function cellCourses(slot, weekday) {
  return sortedTimetable.value.filter((item) => {
    return item.weekday === weekday && item.startSection <= slot.end && item.endSection >= slot.start
  })
}

async function drop(enrollmentId) {
  error.value = ''
  message.value = ''
  droppingId.value = enrollmentId
  try {
    await dropEnrollment(enrollmentId)
    message.value = '退课成功'
    await loadData(false)
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '退课失败，服务可能暂时不可用'
  } finally {
    droppingId.value = null
  }
}

function weekText(item) {
  return `第${item.startWeek}-${item.endWeek}周`
}

function weekTypeText(value) {
  if (value === 'ODD') return '单周'
  if (value === 'EVEN') return '双周'
  return '每周'
}
</script>
