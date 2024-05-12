<template>
  <div>
    <div ref="modal" class="modal fade" :class="{ show: modelValue, 'd-block': modelValue }" tabindex="-1"
         role="dialog">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ props.title }}</h5>
            <button type="button"
                    class="btn-close"
                    aria-label="Close"
                    v-if="!hideClose"
                    @click="closeModal">
            </button>
          </div>
          <div class="modal-body">
            <slot />
          </div>
          <div class="modal-footer" v-if="slots.footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </div>
    <div v-if="modelValue" class="modal-backdrop fade" :class="{ show: modelValue}"></div>
  </div>
</template>

<script setup lang="ts">

const props = withDefaults(defineProps<{
  modelValue: boolean,
  title: string,
  hideClose?: boolean
}>(), {
  modelValue: false,
  title: ''
})

const emit = defineEmits<{
  (event: 'update:modelValue', payload: boolean): void;
}>()

const slots = useSlots()
const closeModal = () => {
  emit('update:modelValue', false)
}
</script>