<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>我的课表</h2>
        <p class="muted">课表和退课都通过 Gateway 调用 enrollment-service。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadData()">刷新</button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>课程ID</th>
            <th>星期</th>
            <th>开始</th>
            <th>结束</th>
            <th>教室</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in timetable" :key="`${item.enrollmentId}-${item.scheduleId}`">
            <td>{{ item.courseId }}</td>
            <td>{{ weekdayText(item.weekday) }}</td>
            <td>{{ item.startTime }}</td>
            <td>{{ item.endTime }}</td>
            <td>{{ item.classroom }}</td>
          </tr>
          <tr v-if="!timetable.length">
            <td colspan="5">暂无课表</td>
          </tr>
        </tbody>
      </table>
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
import { onMounted, ref } from 'vue'
import { dropEnrollment, getStudentTimetable, listStudentEnrollments } from '../api/enrollment'

const timetable = ref([])
const enrollments = ref([])
const error = ref('')
const message = ref('')
const droppingId = ref(null)

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
    const [scheduleRes, enrollmentRes] = await Promise.all([
      getStudentTimetable(studentId),
      listStudentEnrollments(studentId)
    ])
    timetable.value = scheduleRes.data || []
    enrollments.value = enrollmentRes.data || []
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '课表加载失败'
  }
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

function weekdayText(value) {
  return ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'][value - 1] || value
}
</script>
