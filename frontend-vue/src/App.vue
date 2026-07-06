<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import gsap from 'gsap'

onMounted(() => {
  // 全局 GSAP 初始化动画
  gsap.from('#app', {
    opacity: 0,
    duration: 0.5,
    ease: 'power2.out'
  })
})
</script>

<style>
#app {
  width: 100%;
  min-height: 100vh;
}
</style>
