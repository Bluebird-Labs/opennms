<template>
  <div class="logo">
    <img src="@/assets/opennms-logo-dark.svg" v-if="props.mode == 'dark'" >
    <img src="@/assets/opennms-logo-light.svg" v-else >
  </div>
</template>

<script setup lang="ts">
export type LogoType = 'light' | 'dark';

const props = defineProps<{
  mode: LogoType
}>()


</script>

<style scoped lang="scss">
.logo {
  width: 8em !important;
}
</style>
