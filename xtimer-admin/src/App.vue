<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">xtimer 控制台</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/timers">
          <el-icon><Timer /></el-icon>
          <span>定时器管理</span>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><List /></el-icon>
          <span>执行记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">{{ route.meta.title }}</span>
        <span class="header-sub">定时任务可视化管理 · 直连 xtimer :8082</span>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>
