<template>
  <div class="bg-light p-1 px-2 rounded-4">
    <!-- Chip -->
    <div
      class="badge rounded-pill text-bg-primary me-2"
      :aria-disabled="disabled"
    >
      <slot name="icon">
      </slot>
    </div>
    <!-- Chip Body -->
    <slot />
  </div>
</template>
<script setup lang="ts">
const slots = useSlots()
const props = withDefaults(defineProps<{
  disabled: boolean,
  clickable: boolean,
  condensed: boolean
}>(), {
  disabled: false,
  clickable: false,
  condensed: false
})

</script>

<style lang="scss" scoped>
//@import "@featherds/styles/themes/variables";
//@import "@featherds/styles/themes/utils";
//@import "@featherds/styles/mixins/typography";
//@import "../../../scss/mixins";
//
//.chip {
//  margin: 0;
//  margin-right: 0.5rem;
//  margin-bottom: 0.5rem;
//  height: 2rem;
//  list-style: none;
//  display: inline-flex;
//  align-items: center;
//  background-color: var($border-light-on-surface);
//  border-radius: 16px;
//  border: 1px solid transparent;
//  min-width: 0;
//  max-width: 18rem;
//  padding: 0 0.25rem;
//  @include state-on-neutral($border-light-on-surface);
//  &:focus {
//    outline: none;
//  }
//  &.hover {
//    cursor: pointer;
//    :deep(.ripple) {
//      display: block;
//    }
//  }
//  :deep(.ripple) {
//    display: none;
//  }
//
//  & > :deep(span[role="gridcell"]:not(.chip-delete)) {
//    display: flex;
//    flex: 1;
//    min-width: 0;
//
//    & .chip-label-button {
//      min-width: 0;
//    }
//  }
//
//  &.focused:not(.disabled) {
//    :deep(.label) {
//      color: var($primary-text-on-surface);
//    }
//    border: 1px solid var($border-on-surface);
//    outline: 0;
//  }
//  &.disabled {
//    color: var($disabled-text-on-surface);
//    background-color: var($border-light-on-surface);
//    @include state-disabled($border-light-on-surface);
//    cursor: default;
//    :deep(.label) {
//      color: var($disabled-text-on-surface);
//      cursor: default;
//      &:hover {
//        color: var($disabled-text-on-surface);
//      }
//    }
//    :deep(.ripple) {
//      display: none;
//    }
//    :deep(.chip-icon > .feather-icon) {
//      color: var($disabled-text-on-surface);
//    }
//  }
//  &.condensed {
//    @include condensed-chip;
//  }
//}
</style>
