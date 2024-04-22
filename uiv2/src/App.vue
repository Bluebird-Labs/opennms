<template>
  <div class="main-content">
    <router-view v-slot="{ Component }">
      <keep-alive include="MapKeepAlive">
        <component :is="Component"/>
      </keep-alive>
    </router-view>
  </div>

  <!--  <FeatherAppLayout content-layout="full">-->
  <!--    <template v-slot:header>-->
  <!--      <Menubar />-->
  <!--    </template>-->
  <!--    <div class="main-content">-->
  <!--      <Spinner />-->
  <!--      <Snackbar />-->
  <!--      <router-view v-slot="{ Component }">-->
  <!--        <keep-alive include="MapKeepAlive">-->
  <!--          <component :is="Component" />-->
  <!--        </keep-alive>-->
  <!--      </router-view>-->
  <!--    </div>-->
  <!--    <template v-slot:footer>-->
  <!--      <Footer />-->
  <!--    </template>-->
  <!--  </FeatherAppLayout>-->
</template>

<script
    setup
    lang="ts"
>
// Init the axios instances
import {useAuthStore} from "@/stores/authStore.ts";
const authStore = useAuthStore()
import {rest, v1, v2} from '@/services/axiosInstances';

// import Footer from '@/components/Layout/Footer.vue'
// import Menubar from '@/components/Layout/Menubar.vue'
// import Spinner from '@/components/Common/Spinner.vue'
// import Snackbar from '@/components/Common/Snackbar.vue'
// import {useAuthStore} from './stores/authStore'
// import {useInfoStore} from './stores/infoStore'
// import {usePluginStore} from './stores/pluginStore'
// import {useMenuStore} from './stores/menuStore'
// import {useNodeStructureStore} from './stores/nodeStructureStore'

// const infoStore = useInfoStore()
// const menuStore = useMenuStore()
// const nodeStructureStore = useNodeStructureStore()
// const pluginStore = usePluginStore()


  // authStore.getWhoAmI()
  // infoStore.getInfo()
  // menuStore.getMainMenu()
  // menuStore.getNotificationSummary()
  // nodeStructureStore.getCategories()
  // nodeStructureStore.getMonitoringLocations()
  // pluginStore.getPlugins()
</script>

<style lang="scss">

</style>
