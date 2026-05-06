<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">403</p>
      <h1>没有访问权限</h1>
      <p class="muted">当前账号不能访问这个页面，请返回对应角色的首页。</p>
      <RouterLink class="text-link" to="/">返回首页</RouterLink>
    </div>
  </section>
</template>

<script setup>
import { RouterLink } from 'vue-router'
</script>
