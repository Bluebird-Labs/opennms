<script setup lang="ts">
import {useAuthStore} from "@/stores/authStore.ts";

const store = useAuthStore();
store.getWhoAmI()
</script>

<template>
  <div class="container">
    <h1>Welcome</h1>
    {{store.whoAmI}}
    {{store.login}}
  </div>
</template>

<style scoped lang="scss">

</style>
