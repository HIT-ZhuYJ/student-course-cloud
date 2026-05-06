<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">学生端</p>
        <h1>{{ username }}</h1>
        <p class="muted">查询课程、完成选课，并查看个人课表。</p>
      </div>
      <div class="tabs">
        <RouterLink to="/student/courses">课程列表</RouterLink>
        <RouterLink to="/student/timetable">我的课表</RouterLink>
      </div>
    </header>
    <RouterView />
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'

const username = computed(() => localStorage.getItem('username') || '学生')
</script>
