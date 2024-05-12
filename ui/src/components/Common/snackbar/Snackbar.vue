<template>
  <div class="snackbar-wrapper" v-if="modelValue || contentShow">
    <div
      class="snackbar"
      data-ref-id="snackbar"
      :class="{
        'fade-in': internalValue,
        'fade-out': contentShow && !internalValue,
        center: center,
        'btn-danger': error,
      }"
      @animationend="closed"
      @keydown="keyPressed"
      @mouseover="stopTimer"
      @mouseleave="resumeTimer"
      @focusin="stopTimer"
      @focusout="resumeTimer"
    >
      <div class="content-wrapper" role="status" aria-live="polite">
        <div
          class="content"
          v-if="contentShow"
          data-ref-id="snackbar-content"
        >
          <slot />
        </div>
      </div>
      <div class="button" data-ref-id="snackbar-button">
        <slot name="button"></slot>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { ref, inject, watch, toRef, nextTick, defineComponent, Ref } from 'vue'
import { Code } from '@/lib/utils'

export const props = {
  modelValue: {
    type: Boolean,
    default: false
  },
  center: {
    type: Boolean,
    default: false
  },
  timeout: {
    type: Number,
    default: 4000
  },
  error: {
    type: Boolean,
    default: false
  }
} as const
export const emits = {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  'update:modelValue': (value: boolean) => true,
  closed: () => true
}

interface ITimer {
  pause: () => void
  resume: () => void
}

export default defineComponent({
  emits,
  props,
  setup(props, context) {
    const hideTimeout = ref() as Ref<ITimer>
    const contentShow = ref(false)
    const internalValue = ref(false)
    const id = Date.now() + Math.floor(Math.random() * 1000)
    const incomingValue = toRef(props, 'modelValue')
    const queueSnackbar = inject(
      'queueSnackbar',
      false as false | ((id: number, internalVal: Ref<boolean>) => void)
    )
    const unqueueSnackbar = inject(
      'unqueueSnackbar',
      false as false | ((id: number) => void)
    )
    const nextSnackbar = inject('nextSnackbar', () => {
    })
    const timer = (callback: () => void, duration: number): ITimer => {
      let id: any
      let start: number
      let remaining = duration
      const pause = () => {
        if (!id) return
        clearTimeout(id)
        id = 0
        remaining -= Date.now() - start
      }
      const resume = () => {
        if (id) return
        start = Date.now()
        id = setTimeout(callback, remaining)
      }
      resume()
      return { pause: pause, resume: resume }
    }

    const closed = () => {
      contentShow.value = false
      context.emit('closed')
      if (nextSnackbar) nextSnackbar()
    }

    const keyPressed = (e: KeyboardEvent) => {
      if (e.code === Code.ESCAPE) {
        internalValue.value = false
      }
    }

    const stopTimer = () => {
      hideTimeout.value.pause()
    }

    const resumeTimer = () => {
      if (internalValue.value) hideTimeout.value.resume()
    }

    watch(
      incomingValue,
      (v) => {
        if (v) {
          nextTick(() => {
            queueSnackbar === false
              ? (internalValue.value = v)
              : queueSnackbar(id, internalValue)
          })
        } else {
          unqueueSnackbar === false
            ? (internalValue.value = v)
            : unqueueSnackbar(id)
        }
      },
      { immediate: true }
    )

    watch(internalValue, (v) => {
      if (v) {
        contentShow.value = true
        hideTimeout.value = timer(() => {
          internalValue.value = false
        }, props.timeout)
      } else {
        context.emit('update:modelValue', false)
        stopTimer()
      }
    })

    return {
      hideTimeout,
      contentShow,
      internalValue,
      id,
      incomingValue,
      closed,
      keyPressed,
      stopTimer,
      resumeTimer
    }
  }
})
</script>

<style lang="scss" scoped>
@use "sass:math";

.snackbar-wrapper {
  display: flex;
  position: fixed;
  width: 100%;
  bottom: 0;
  right: 0;
  padding: 15px;
  pointer-events: none;
  z-index: 9999;

  .snackbar {
    display: flex;
    flex-wrap: wrap;
    border-radius: 4px;
    background-color: var(--bs-gray);
    padding: 0.75rem 0 0.75rem 1rem;
    max-width: 540px;
    overflow: visible;
    margin-left: auto;
    opacity: 0;
    transform: translateY(12px);
    pointer-events: none;

    &.center {
      margin-right: auto;
    }

    .content-wrapper, .button {
      display: flex;
      align-items: center;
    }

    .content-wrapper {
      min-width: calc(160px + 1rem);
      max-width: calc(400px + 1rem);
      padding-right: 1rem;
    }

    @keyframes fadeout {
      from {
        opacity: 1;
        transform: translateY(0);
      }

      99% {
        opacity: 0;
        transform: translateY(0);
      }

      to {
        opacity: 0;
        transform: translateY(12px);
      }
    }

    &.fade-out {
      animation-duration: 280ms;
      animation-name: fadeout;
      animation-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
      opacity: 0;
      transform: translateY(12px);
    }

    &.fade-in {
      transition: all 280ms;
      transition-timing-function: cubic-bezier(0, 0, 0.2, 1);
      opacity: 1;
      transform: translateY(0);
      pointer-events: auto;
    }
  }
}
</style>
