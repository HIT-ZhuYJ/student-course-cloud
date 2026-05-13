<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">管理员端</p>
        <h1>管理首页</h1>
      </div>
    </header>
    <div class="dashboard-grid">
      <RouterLink to="/admin/courses">课程管理</RouterLink>
      <RouterLink to="/admin/teachers">教师管理</RouterLink>
      <RouterLink to="/admin/students">学生管理</RouterLink>
      <RouterLink to="/admin/enrollments">选课记录</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
