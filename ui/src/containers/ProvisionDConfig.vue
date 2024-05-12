<template>
  <div class="container">
    <BreadCrumbs :items="breadcrumbs" />
    <SnackbarWrapper />
    <ConfigurationHeader
      title="Configuration"
      headline="External Requisitions and Thread Pools"
    />
    <ConfigurationTableWrapper />
    <ThreadPools class="mt-4" />
  </div>
</template>

<script setup lang="ts">
import SnackbarWrapper from '@/components/Common/snackbar/SnackbarWrapper.vue'
import ConfigurationHeader from '@/components/Configuration/ConfigurationHeader.vue'
import ConfigurationTableWrapper from '@/components/Configuration/ConfigurationTableWrapper.vue'
import ThreadPools from '@/components/Configuration/ConfigurationThreadPools.vue'
import BreadCrumbs from '@/components/Layout/BreadCrumbs.vue'
import { useConfigurationStore } from '@/stores/configurationStore'
import { useMenuStore } from '@/stores/menuStore'
import { BreadCrumb } from '@/types'

const configurationStore = useConfigurationStore()
const menuStore = useMenuStore()
const homeUrl = computed<string>(() => menuStore.mainMenu.homeUrl)

const breadcrumbs = computed<BreadCrumb[]>(() => {
  return [
    { label: 'Home', to: homeUrl.value, isAbsoluteLink: true },
    { label: 'External Requisitions and Thread Pools', to: '#', position: 'last' }
  ]
})

configurationStore.getProvisionDService()
</script>
<style lang="scss" scoped>
</style>

