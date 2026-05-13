<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">教师端</p>
        <h1>我的任课</h1>
      </div>
      <button class="secondary-button" type="button" @click="loadCourses">刷新</button>
    </header>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>分配ID</th>
            <th>课程ID</th>
            <th>课程名称</th>
            <th>编码</th>
            <th>学分</th>
            <th>容量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in courses" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.courseId }}</td>
            <td>{{ item.course?.courseName || '-' }}</td>
            <td>{{ item.course?.courseCode || '-' }}</td>
            <td>{{ item.course?.credit || '-' }}</td>
            <td>{{ item.course ? `${item.course.selectedCount}/${item.course.capacity}` : '-' }}</td>
            <td>{{ item.status }}</td>
            <td><button class="small-button" type="button" @click="showSchedules(item.courseId)">上课时间</button></td>
          </tr>
          <tr v-if="!courses.length">
            <td colspan="8">暂无任课信息</td>
          </tr>
        </tbody>
      </table>
    </div>

    <section v-if="selectedCourseId" class="sub-section">
      <h2>课程 {{ selectedCourseId }} 上课时间</h2>
      <div class="table-card">
        <table>
          <thead>
            <tr><th>星期</th><th>开始</th><th>结束</th><th>教室</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in schedules" :key="item.scheduleId">
              <td>{{ weekdayText(item.weekday) }}</td>
              <td>{{ item.startTime }}</td>
              <td>{{ item.endTime }}</td>
              <td>{{ item.classroom }}</td>
            </tr>
            <tr v-if="!schedules.length"><td colspan="4">暂无上课时间</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getCourse, listCourseSchedules } from '../api/course'
import { listTeacherCourses } from '../api/teacher'

const courses = ref([])
const schedules = ref([])
const selectedCourseId = ref(null)
const error = ref('')

onMounted(loadCourses)

async function loadCourses() {
  error.value = ''
  try {
    const res = await listTeacherCourses(localStorage.getItem('relatedId'))
    const assignments = res.data || []
    courses.value = await Promise.all(assignments.map(async (assignment) => {
      try {
        const courseRes = await getCourse(assignment.courseId)
        return { ...assignment, course: courseRes.data }
      } catch {
        return { ...assignment, course: null }
      }
    }))
    if (!selectedCourseId.value && courses.value.length) {
      await showSchedules(courses.value[0].courseId)
    }
  } catch (err) {
    error.value = err.normalizedMessage || '任课加载失败'
  }
}

async function showSchedules(courseId) {
  selectedCourseId.value = courseId
  try {
    const res = await listCourseSchedules(courseId)
    schedules.value = res.data || []
  } catch (err) {
    error.value = err.normalizedMessage || '上课时间加载失败'
  }
}

function weekdayText(value) {
  return ['星期一', '星期二', '星期三', '星期四', '星期五', '星期六', '星期日'][value - 1] || value
}
</script>
