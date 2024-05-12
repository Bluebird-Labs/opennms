<template>
  <div class="my-4">
    <h5 class="text-secondary text-uppercase mb-2">{{ props.title }}</h5>
    <h2 class="fw-bold mb-3">{{ props.headline }}</h2>
    <p class="w-75">
      An external requisition imports the nodes, interfaces, and services from outside inventory sources.
      Thread pool sizes impact the performance of the provisioning subsystem.
    </p>
  </div>
</template>

<script lang="ts" setup>
const props = defineProps({
  title: String,
  headline: String,
  body: String
})
</script>

<style lang="scss" scoped>
</style>
