<script setup lang="ts">

defineProps<{
  icon: any
}>()
</script>

<template>
  <component :is="icon" class="icon"></component>
</template>

<style lang="scss">

.icon-sm {
  height: 1rem;
}

.icon-md, .icon-x2 {
  height: 2rem;
}
.icon-lg, .icon-x3 {
  height: 3rem;
}
.icon-xl, .icon-x4 {
  height: 4rem;
}
.icon-xxl, .icon-x5 {
  height: 5rem;
}

.icon {
  height: 1.5rem;
  aspect-ratio: auto;
  color: inherit;
  fill: currentColor;
  display: inline-block;
  overflow: visible;

  :deep(svg) {
    height: 1rem;
    aspect-ratio: auto;
    color: inherit;
    fill: currentColor;
    display: inline-block;
    overflow: visible;
  }
}
</style>