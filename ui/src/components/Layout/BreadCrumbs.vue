<template>
  <div class="breadcrumbs bg-light gap-1 p-2 mt-4">
    <template v-for="item,index of items" :key="item.label">
      <a v-if="item.isAbsoluteLink" :href="item.to">{{ item.label }}</a>
      <router-link v-else :to="item.to">{{ item.label }}</router-link>
      <span v-if="index !== items.length - 1" class="slash">&sol;</span>
    </template>
  </div>
</template>

<script setup lang="ts">
import { BreadCrumb } from '@/types'
import { PropType } from 'vue'

defineProps({
  items: {
    required: true,
    type: Array as PropType<BreadCrumb[]>
  }
})
</script>

<style lang="scss" scoped>
.breadcrumbs {
  width: 100%;
  display: flex;

  .slash {
    padding: 0 8px;
    font-weight: 400;
  }
}
</style>
