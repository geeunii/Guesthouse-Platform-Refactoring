<script setup>
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
} from 'chart.js'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const props = defineProps({
  labels: { type: Array, required: true },
  values: { type: Array, required: true },
  height: { type: Number, default: 260 },
  format: { type: String, default: '' },
  currency: { type: Boolean, default: true },
  unitLabel: { type: String, default: '' },
  tooltipLabel: { type: String, default: '' },
  maxXTicks: { type: Number, default: null }
})

const resolvedFormat = computed(() => props.format || (props.currency ? 'currency' : 'count'))

const formatValue = (value) => {
  const numeric = Number(value ?? 0)
  if (resolvedFormat.value === 'count') {
    const unit = props.unitLabel || '건'
    return unit ? `${numeric.toLocaleString()}${unit}` : numeric.toLocaleString()
  }
  return `₩${numeric.toLocaleString()}`
}

const formatTooltipLabel = (value) => {
  const prefix = props.tooltipLabel?.trim()
  return prefix ? `${prefix}: ${formatValue(value)}` : formatValue(value)
}

const formatXAxisLabel = (label) => {
  if (!label) return ''
  const text = String(label)
  if (text.length >= 10 && text[4] === '-' && text[7] === '-') {
    return text.slice(5)
  }
  return text
}

const maxTicks = computed(() => {
  const total = props.labels.length
  if (props.maxXTicks && Number.isFinite(props.maxXTicks)) {
    return Math.max(2, Math.min(Number(props.maxXTicks), total))
  }
  return total <= 8 ? total : 8
})

const showIndexes = computed(() => {
  const total = props.labels.length
  if (total === 0) return new Set()
  if (total <= maxTicks.value) {
    return new Set(Array.from({ length: total }, (_, idx) => idx))
  }
  const step = Math.ceil((total - 1) / (maxTicks.value - 1))
  const indexes = new Set([0, total - 1])
  for (let i = 0; i < total; i += step) {
    indexes.add(i)
  }
  return indexes
})

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      data: props.values,
      backgroundColor: 'rgba(20, 184, 166, 0.35)',
      hoverBackgroundColor: 'rgba(20, 184, 166, 0.85)',
      borderColor: '#0f766e',
      borderWidth: 1,
      hoverBorderWidth: 2,
      borderRadius: 8,
      barPercentage: 0.7,
      categoryPercentage: 0.8
    }
  ]
}))

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        title: (items) => items?.[0]?.label ?? '',
        label: (item) => formatTooltipLabel(item.parsed?.y ?? 0)
      }
    }
  },
  interaction: { mode: 'nearest', intersect: true },
  scales: {
    x: {
      grid: { display: false },
      ticks: {
        autoSkip: false,
        maxRotation: 0,
        minRotation: 0,
        callback: (value, index) => {
          if (!showIndexes.value.has(index)) return ''
          return formatXAxisLabel(props.labels[index])
        }
      }
    },
    y: {
      beginAtZero: true,
      min: 0,
      ticks: {
        callback: (value) => formatValue(value)
      }
    }
  }
}))
</script>

<template>
  <div class="admin-bar-chart" :style="{ height: `${height}px` }">
    <Bar :data="chartData" :options="chartOptions" />
  </div>
</template>

<style scoped>
.admin-bar-chart {
  position: relative;
  width: 100%;
}
</style>
