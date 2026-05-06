<template>
  <section>
    <div class="section-toolbar">
      <div>
        <h2>课程列表</h2>
        <p class="muted">课程数据来自 Gateway 的 /api/courses，选课会调用 enrollment-service 完成容量、教师分配和时间冲突校验。</p>
      </div>
      <button class="secondary-button" type="button" @click="loadCourses()">刷新</button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <p v-if="message" class="success-text">{{ message }}</p>

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>课程ID</th>
            <th>课程名称</th>
            <th>学分</th>
            <th>容量</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="course in courses" :key="course.courseId">
            <td>{{ course.courseId }}</td>
            <td>{{ course.courseName }}</td>
            <td>{{ course.credit }}</td>
            <td>{{ course.selectedCount }}/{{ course.capacity }}</td>
            <td>{{ course.status }}</td>
            <td>
              <button
                class="small-button"
                type="button"
                :disabled="loadingCourseId === course.courseId || course.status !== 'OPEN'"
                @click="submitEnroll(course.courseId)"
              >
                {{ loadingCourseId === course.courseId ? '处理中...' : '选课' }}
              </button>
            </td>
          </tr>
          <tr v-if="!courses.length">
            <td colspan="6">暂无课程</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listCourses } from '../api/course'
import { enroll } from '../api/enrollment'

const courses = ref([])
const error = ref('')
const message = ref('')
const loadingCourseId = ref(null)

onMounted(() => loadCourses())

async function loadCourses(clearNotice = true) {
  error.value = ''
  if (clearNotice) {
    message.value = ''
  }
  try {
    const res = await listCourses({ pageNo: 1, pageSize: 50, status: 'OPEN' })
    courses.value = res.data?.records || []
  } catch (err) {
    error.value = err.normalizedMessage || '课程加载失败'
  }
}

async function submitEnroll(courseId) {
  error.value = ''
  message.value = ''
  loadingCourseId.value = courseId
  try {
    const studentId = Number(localStorage.getItem('relatedId'))
    if (!studentId) {
      throw new Error('未获取到当前学生身份，请重新登录')
    }
    await enroll({ studentId, courseId })
    message.value = '选课成功'
    await loadCourses(false)
  } catch (err) {
    error.value = err.normalizedMessage || err.message || '选课失败，可能是课程已满、时间冲突或服务暂时不可用'
  } finally {
    loadingCourseId.value = null
  }
}
</script>
