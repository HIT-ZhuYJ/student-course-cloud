<template>
  <section class="page">
    <header class="page-header">
      <div>
        <h1>学生端</h1>
      </div>
      <div class="tabs">
        <RouterLink to="/student/courses">课程列表</RouterLink>
        <RouterLink to="/student/timetable">我的课表</RouterLink>
        <RouterLink to="/student/profile">个人信息</RouterLink>
      </div>
    </header>
    <RouterView />
  </section>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>
