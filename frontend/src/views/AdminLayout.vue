<template>
  <section class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">管理员端</p>
        <h1>课堂演示管理台</h1>
      </div>
      <div class="tabs">
        <RouterLink to="/admin/courses">课程管理</RouterLink>
        <RouterLink to="/admin/teachers">教师管理</RouterLink>
        <RouterLink to="/admin/students">学生管理</RouterLink>
        <RouterLink to="/admin/enrollments">选课记录</RouterLink>
      </div>
    </header>
    <RouterView />
  </section>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>
