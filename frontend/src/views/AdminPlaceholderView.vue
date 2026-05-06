<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">管理员端</p>
        <h1>{{ title }}</h1>
        <p class="muted">该模块已经接入 Gateway，请通过左侧导航进入正式演示页面。</p>
      </div>
    </header>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const names = {
  courses: '课程管理',
  teachers: '教师管理',
  students: '学生管理',
  enrollments: '选课记录'
}
const title = computed(() => names[route.params.section] || '管理模块')
</script>
