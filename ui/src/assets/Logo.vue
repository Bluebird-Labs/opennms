<template>
  <div class="logo">
    <LogoDark v-if="props.mode == 'dark'" />
    <LogoLight v-else />
  </div>
</template>

<script setup lang="ts">
import LogoLight from './OpenNMS_Horizontal-Logo_Light-BG.svg'
import LogoDark from './OpenNMS_Horizontal-Logo_Dark-BG.svg'

export type LogoType = 'light' | 'dark';

const props = defineProps<{
  mode: LogoType
}>()


</script>

<style scoped lang="scss">
.logo {
  width: 8em !important;
  color: #FFF !important;
}
</style>
