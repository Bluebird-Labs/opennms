<template>
  <FeatherAppLayout content-layout="full">
    <template v-slot:header>
      <Menubar />
    </template>

    <div class="main-content">
      <Spinner />
      <Snackbar />
      <router-view v-slot="{ Component }">
        <keep-alive include="MapKeepAlive">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </div>
    <template v-slot:footer>
      <Footer />
    </template>
  </FeatherAppLayout>
</template>

<script
  setup
  lang="ts"
>
import { FeatherAppLayout } from '@featherds/app-layout'
import Footer from './components/Layout/Footer.vue'
import Menubar from './components/Layout/Menubar.vue'
import Spinner from './components/Common/Spinner.vue'
import Snackbar from '@/components/Common/snackbar/Snackbar.vue'
import { useAuthStore } from '@/stores/authStore'
import { useInfoStore } from '@/stores/infoStore'
import { usePluginStore } from '@/stores/pluginStore'
import { useMenuStore } from '@/stores/menuStore'
import { useNodeStructureStore } from '@/stores/nodeStructureStore'

const authStore = useAuthStore()
const infoStore = useInfoStore()
const menuStore = useMenuStore()
const nodeStructureStore = useNodeStructureStore()
const pluginStore = usePluginStore()

onMounted(() => {
  authStore.getWhoAmI()
  infoStore.getInfo()
  menuStore.getMainMenu()
  menuStore.getNotificationSummary()
  nodeStructureStore.getCategories()
  nodeStructureStore.getMonitoringLocations()
  pluginStore.getPlugins()
})
</script>

<style lang="scss">
@import "@featherds/styles/lib/grid";
@import "@featherds/styles/mixins/typography";
@import "@featherds/styles/themes/open-mixins";

</style>
