<template>
  <Snackbar
    v-model="isDisplayed"
    :center="isCentered"
    :error="hasError"
    :timeout="setTimeout"
  >
    {{ message }}
    <template v-slot:button>
      <button class="btn btn-sm btn-primary"
              @click="hideSnackbar">
        Dismiss
      </button>
    </template>
  </Snackbar>
</template>

<script
  setup
  lang="ts"
>
import useSnackbar from '@/composables/useSnackbar'
import Snackbar from '@/components/Common/snackbar/Snackbar.vue'

const { hideSnackbar, isDisplayed, isCentered, hasError, message, setTimeout } = useSnackbar()
</script>

