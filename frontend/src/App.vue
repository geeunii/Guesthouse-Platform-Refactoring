<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import TheHeader from './components/TheHeader.vue'
import TheFooter from './components/TheFooter.vue'
import ChatbotWidget from './components/chatbot/ChatbotWidget.vue'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
</script>

<template>
  <div class="app-layout">
    <TheHeader />
    <RouterView />
    <TheFooter v-if="!isAdminRoute" />
    <ChatbotWidget />
  </div>
</template>

<style>
@import './assets/style.css';
</style>

