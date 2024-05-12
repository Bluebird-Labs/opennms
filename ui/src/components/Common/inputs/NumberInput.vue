<template>
  <div>
    <div :class="{'form-floating': floating}">
      <input type="number"
             class="form-control"
             :value="modelValue"
             :id="id"
             :placeholder="placeholder || label"
             @input="propagateValueChange($event)">
      <label :for="id">{{ label }}</label>
    </div>
    <div class="text-small text-muted" v-if="hint">
      <span>{{ hint }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: string,
  label: string,
  hint?: string,
  id?: string,
  placeholder?: string,
  floating: boolean
}>(), {
  floating: true
})

const emit = defineEmits<{
  (event: 'update:modelValue', payload: number): void;
}>()

const propagateValueChange = ($event: any) => {
  const number = parseFloat($event.target.value)
  if (!isNaN(number)) {
    emit('update:modelValue', number)
  }
}
</script>


<style scoped lang="scss">

</style>