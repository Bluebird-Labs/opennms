<template>
  <div class="card card-body">
    <div role="heading">
      <a
        href="#"
        role="button"
        class="d-flex justify-content-between"
        :class="{ expanded: expanded, disabled: disabled }"
        :aria-expanded="expanded ? 'true' : 'false'"
        :aria-disabled="disabled ? 'true' : 'false'"
        :aria-busy="loading ? 'true' : 'false'"
        @click.prevent="handleClick"
      >
        <span class="">
          <slot name="title"> {{ title }}</slot></span
        >
        <Icon
          :icon="IconRepository.Navigation.Expand_More"
          :class="{ rotated: expanded }"
        />
      </a>
    </div>

    <!-- TODO MVR get transitions to work properly -->
    <div class="panel-content" v-if="expanded">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { IconRepository } from '@/assets/icons/IconRepository'
import Icon from '@/assets/icons/Icon.vue'

const props = defineProps<{
  modelValue: boolean,
  title?: string,
  disabled?: boolean,
  loading?: boolean
}>()
const emits = defineEmits({
  'update:modelValue': (value: boolean) => true
})

const expanded = computed(() => {
  if (props.disabled) {
    return false
  }
  return props.modelValue
})
const handleClick = () => {
  if (!props.disabled) {
    emits('update:modelValue', !props.modelValue)
  }
}

</script>


<style scoped lang="scss">
.rotated {
  transform: rotate(180deg);
}

.panel-content {
  height: auto;
}

</style>