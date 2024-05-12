<template>
  <Modal
    v-model="visible"
    :title="title"
  >
    <p>This will delete the definition titled: {{ options.configName }}</p>
    <template v-slot:footer>
      <button class="btn btn-danger" @click="props.onButtonClick(false)">No</button>
      <button class="btn btn-primary" @click="props.onButtonClick(true)">Yes</button>
    </template>
  </Modal>
</template>
<script lang="ts" setup>
import Modal from '@/components/Common/modal/Modal.vue'

export type ConfigurationDoubleCheckModalOptions = {
  active: boolean,
  index: number,
  configName: string
}
const props = defineProps<{
  onButtonClick: (confirm: boolean) => void,
  options: ConfigurationDoubleCheckModalOptions,
}>()
const title = 'Are you sure?'
let visible = ref(props?.options?.active || false)
watch(() => props.options.active, (active) => {
  visible.value = active
})
</script>
