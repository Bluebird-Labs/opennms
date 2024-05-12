<template>
  <div class="pagination-wrapper d-flex justify-content-end gap-5 text-muted">
    <div class="d-flex align-items-center">
      {{ perPageText }}
    </div>
    <!-- TODO MVR wrap this into a select component? -->
    <select class="form-select pagination-selector"
            v-model="internalPageSize"
            @change="updatePageSize"
    >
      <option v-for="pageSize in pageSizes"
              v-bind:key="pageSize"
              :value="pageSize">
        {{ pageSize }}
      </option>
    </select>
    <div class="d-flex align-items-center">
      {{ rangeText }}
    </div>
    <nav>
      <ul class="pagination">
        <li class="page-item">
          <button class="page-link bg-white"
                  :class="{disabled: disablePrevious}"
                  :aria-label="LABELS.first"
                  :disabled="disablePrevious"
                  @click="onFirst"
          >
            <Icon :icon="IconRepository.Navigation.First_Page" />
          </button>
        </li>
        <li class="page-item">
          <button class="page-link bg-white"
                  :class="{disabled: disablePrevious}"
                  :aria-label="LABELS.previous"
                  :disabled="disablePrevious"
                  @click="onPrevious"
          >
            <Icon :icon="IconRepository.Navigation.Chevron_Left" />
          </button>
        </li>
        <li class="page-item" v-for="page of pages" v-bind:key="page">
          <button class="page-link" :class="{active: page + 1 == modelValue}">{{ page + 1 }}</button>
        </li>
        <li class="page-item">
          <button class="page-link bg-white"
                  :class="{disabled: disableNext}"
                  :aria-label="LABELS.next"
                  :disabled="disableNext"
                  @click="onNext"
          >
            <Icon :icon="IconRepository.Navigation.Chevron_Right" />
          </button>
        </li>
        <li class="page-item">
          <button class="page-link bg-white"
                  :class="{disabled: disableNext}"
                  :aria-label="LABELS.last"
                  :disabled="disableNext"
                  @click="onLast"
          >
            <Icon :icon="IconRepository.Navigation.Last_Page" />
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { IconRepository } from '@/assets/icons/IconRepository'
import Icon from '@/assets/icons/Icon.vue'

const props = withDefaults(defineProps<{
  modelValue: any, // TODO MVR
  total: number,
  labels?: Partial<typeof LABELS>,
  pageSizes?: number[]
  pageSize: number
}>(), {
  total: 0,
  pageSizes(): number[] {
    return [10, 20, 50]
  },
  pageSize: 20
})

const emits = defineEmits({
  'update:modelValue': (page: number) => true,
  'update:pageSize': (pageSize: number) => true
})

const LABELS = {
  rowsPerPage: 'Rows per page',
  range: '${start} - ${end} of ${total}',
  first: 'Go to first page',
  last: 'Go to last page',
  next: 'Go to next page',
  previous: 'Go to previous page',
  paginationLabel: 'Pagination controls'
}

const perPageText = computed(() => props.labels?.rowsPerPage ? props.labels.rowsPerPage : LABELS.rowsPerPage)
const rangeText = computed(() => {
  const resultTemplate = props.labels?.range ? props.labels?.range : LABELS.range
  let start = props.modelValue * props.pageSize - props.pageSize + 1
  if (start < 0 || start > props.total) {
    start = 0
  }
  let end = start + props.pageSize - 1
  if (end > props.total) {
    end = props.total
  }
  return resultTemplate
    .replace('${start}', start.toString())
    .replace('${end}', end.toString())
    .replace('${total}', props.total.toString())
})
const lastPage = computed(() => Math.ceil(props.total / props.pageSize))
const disablePrevious = computed(() => props.modelValue <= 1 || props.total <= 0)
const disableNext = computed(() => props.modelValue >= lastPage.value || props.total <= 0)
const pages = computed(() => Array.from(Array(lastPage.value).keys()))
const internalPageSize = computed(() => props.pageSize)

// Methods
const onFirst = () => emits('update:modelValue', 1)
const onLast = () => emits('update:modelValue', lastPage.value)
const onNext = () => {
  emits('update:modelValue', props.modelValue + 1)
}
const onPrevious = () => {
  emits('update:modelValue', props.modelValue - 1)
}
const updatePageSize = ($event: any | undefined) => {
  if ($event?.target?.value) {
    emits('update:pageSize', parseInt($event?.target?.value, 10))
  }
}
</script>

<style scoped lang="scss">
.pagination-selector {
  width: 5rem;
}

.page-item {
  margin-left: 0.15rem;
  margin-right: 0.15rem;
}
</style>
