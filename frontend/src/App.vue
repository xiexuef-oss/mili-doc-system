<template>
  <router-view />
  <AiChatFloating v-if="!isLoginPage" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AiChatFloating from '@/components/AiChatFloating.vue'

const route = useRoute()
const isLoginPage = computed(() => route.name === 'Login')
</script>
