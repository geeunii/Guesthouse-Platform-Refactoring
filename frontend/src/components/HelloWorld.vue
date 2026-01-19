<script setup>
const props = defineProps({
  msg: {
    type: String,
    default: 'Hello'
  }
})
</script>

<template>
  <section class="hello">
    <h1>{{ props.msg }}</h1>
    <p>GuestHouse scaffold placeholder.</p>
  </section>
</template>

<style scoped>
.hello {
  text-align: center;
  padding: 1rem;
}

h1 {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
}
</style>
