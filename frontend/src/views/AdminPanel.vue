<template>
  <div class="admin-panel">
    <h2 class="admin-title">Admin Panel</h2>

    <!-- Loading state -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading configuration...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="loadError" class="error-state">
      <p class="error-message">Failed to load configuration</p>
      <button class="retry-button" @click="fetchConfig">Retry</button>
    </div>

    <!-- Config sections -->
    <template v-else>
      <ConfigSection
        title="Scheduling"
        :fields="schedulingFields"
        :errors="schedulingErrors"
        :saving="saving.scheduling"
        @update:fields="onUpdateScheduling"
        @save="saveCategory('scheduling')"
      />

      <ConfigSection
        title="SQS"
        :fields="sqsFields"
        :errors="sqsErrors"
        :saving="saving.sqs"
        @update:fields="onUpdateSqs"
        @save="saveCategory('sqs')"
      />

      <ConfigSection
        title="Kafka"
        :fields="kafkaFields"
        :errors="kafkaErrors"
        :saving="saving.kafka"
        @update:fields="onUpdateKafka"
        @save="saveCategory('kafka')"
      />

      <ConfigSection
        title="Discord"
        :fields="discordFields"
        :errors="discordErrors"
        :saving="saving.discord"
        @update:fields="onUpdateDiscord"
        @save="saveCategory('discord')"
      />

      <!-- Reddit Subreddits (custom section, not ConfigSection) -->
      <section class="config-section">
        <h3 class="config-section-title">Reddit Subreddits</h3>
        <div class="subreddit-list">
          <div
            v-for="(sub, index) in subreddits"
            :key="index"
            class="subreddit-entry"
          >
            <div class="subreddit-fields">
              <div class="subreddit-field">
                <label class="subreddit-label">Name</label>
                <input
                  type="text"
                  class="field-input"
                  :class="{ 'field-input-error': subredditErrors[index]?.name }"
                  :value="sub.name"
                  @input="onSubredditNameChange(index, ($event.target as HTMLInputElement).value)"
                />
                <span v-if="subredditErrors[index]?.name" class="field-error">
                  {{ subredditErrors[index].name }}
                </span>
              </div>
              <div class="subreddit-field">
                <label class="subreddit-label">Limit</label>
                <input
                  type="number"
                  class="field-input"
                  :class="{ 'field-input-error': subredditErrors[index]?.limit }"
                  :value="sub.limit"
                  @input="onSubredditLimitChange(index, Number(($event.target as HTMLInputElement).value))"
                />
                <span v-if="subredditErrors[index]?.limit" class="field-error">
                  {{ subredditErrors[index].limit }}
                </span>
              </div>
            </div>
            <button class="remove-button" @click="removeSubreddit(index)">Remove</button>
          </div>
        </div>
        <button class="add-button" @click="addSubreddit">Add Subreddit</button>
        <button
          class="save-button"
          :disabled="hasSubredditErrors || saving.redditSubreddits"
          @click="saveCategory('redditSubreddits')"
        >
          <span v-if="saving.redditSubreddits" class="saving-indicator">
            <span class="spinner"></span>
            Saving...
          </span>
          <span v-else>Save</span>
        </button>
      </section>
    </template>

    <!-- Toast notifications -->
    <div class="toast-container">
      <div
        v-for="n in notifications"
        :key="n.id"
        class="toast"
        :class="'toast-' + n.type"
      >
        <span>{{ n.message }}</span>
        <button class="toast-dismiss" @click="dismiss(n.id)">&times;</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import ConfigSection from '../components/ConfigSection.vue'
import { useNotification } from '../composables/useNotification'

const { notifications, notify, dismiss } = useNotification()

// Loading / error state
const loading = ref(true)
const loadError = ref(false)

// Category data
const schedulingFields = ref<Record<string, string | number | boolean>>({})
const sqsFields = ref<Record<string, string | number | boolean>>({})
const kafkaFields = ref<Record<string, string | number | boolean>>({})
const discordFields = ref<Record<string, string | number | boolean>>({})
const subreddits = ref<Array<{ name: string; limit: number }>>([])

// Validation errors
const schedulingErrors = ref<Record<string, string>>({})
const sqsErrors = ref<Record<string, string>>({})
const kafkaErrors = ref<Record<string, string>>({})
const discordErrors = ref<Record<string, string>>({})
const subredditErrors = ref<Array<{ name: string; limit: string }>>([])

// Saving state per category
const saving = reactive<Record<string, boolean>>({
  scheduling: false,
  sqs: false,
  kafka: false,
  discord: false,
  redditSubreddits: false,
})

const hasSubredditErrors = computed(() =>
  subredditErrors.value.some((e) => e.name !== '' || e.limit !== '')
)

// --- Fetch config ---
async function fetchConfig() {
  loading.value = true
  loadError.value = false
  try {
    const res = await fetch('/api/config')
    if (!res.ok) throw new Error('Failed to fetch')
    const data = await res.json()

    schedulingFields.value = { ...data.scheduling }
    sqsFields.value = { ...data.sqs }
    kafkaFields.value = { ...data.kafka }
    discordFields.value = { ...data.discord }
    subreddits.value = (data.redditSubreddits || []).map((s: { name: string; limit: number }) => ({ ...s }))
    subredditErrors.value = subreddits.value.map(() => ({ name: '', limit: '' }))

    validateScheduling()
    validateSqs()
    validateKafka()
    validateDiscord()
    validateSubreddits()
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// --- Validation ---
function validateScheduling() {
  const errors: Record<string, string> = {}
  const f = schedulingFields.value
  if (typeof f.fetchIntervalMs === 'number' && f.fetchIntervalMs <= 0) {
    errors.fetchIntervalMs = 'Must be a positive number'
  }
  if (typeof f.postIntervalMs === 'number' && f.postIntervalMs <= 0) {
    errors.postIntervalMs = 'Must be a positive number'
  }
  schedulingErrors.value = errors
}

function validateSqs() {
  const errors: Record<string, string> = {}
  const f = sqsFields.value
  if (f.enabled) {
    if (!f.queueUrl || String(f.queueUrl).trim() === '') {
      errors.queueUrl = 'Required when SQS is enabled'
    }
    if (!f.dlqUrl || String(f.dlqUrl).trim() === '') {
      errors.dlqUrl = 'Required when SQS is enabled'
    }
  }
  if (typeof f.pollInterval === 'number' && f.pollInterval <= 0) {
    errors.pollInterval = 'Must be a positive number'
  }
  sqsErrors.value = errors
}

function validateKafka() {
  const errors: Record<string, string> = {}
  const f = kafkaFields.value
  if (f.enabled) {
    if (!f.bootstrapServers || String(f.bootstrapServers).trim() === '') {
      errors.bootstrapServers = 'Required when Kafka is enabled'
    }
  }
  kafkaErrors.value = errors
}

function validateDiscord() {
  const errors: Record<string, string> = {}
  const f = discordFields.value
  if (!f.channelId || String(f.channelId).trim() === '') {
    errors.channelId = 'Channel ID is required'
  }
  discordErrors.value = errors
}

function validateSubreddits() {
  subredditErrors.value = subreddits.value.map((sub) => ({
    name: sub.name.trim() === '' ? 'Name is required' : '',
    limit: sub.limit < 1 ? 'Limit must be at least 1' : '',
  }))
}

// --- Field update handlers ---
function onUpdateScheduling(fields: Record<string, string | number | boolean>) {
  schedulingFields.value = fields
  validateScheduling()
}

function onUpdateSqs(fields: Record<string, string | number | boolean>) {
  sqsFields.value = fields
  validateSqs()
}

function onUpdateKafka(fields: Record<string, string | number | boolean>) {
  kafkaFields.value = fields
  validateKafka()
}

function onUpdateDiscord(fields: Record<string, string | number | boolean>) {
  discordFields.value = fields
  validateDiscord()
}

function onSubredditNameChange(index: number, value: string) {
  subreddits.value[index] = { ...subreddits.value[index], name: value }
  validateSubreddits()
}

function onSubredditLimitChange(index: number, value: number) {
  subreddits.value[index] = { ...subreddits.value[index], limit: value }
  validateSubreddits()
}

function addSubreddit() {
  subreddits.value.push({ name: '', limit: 5 })
  subredditErrors.value.push({ name: 'Name is required', limit: '' })
}

function removeSubreddit(index: number) {
  subreddits.value.splice(index, 1)
  subredditErrors.value.splice(index, 1)
}

// --- Save ---
async function saveCategory(category: string) {
  saving[category] = true
  try {
    let body: unknown
    if (category === 'redditSubreddits') {
      body = { subreddits: subreddits.value }
    } else {
      const fieldMap: Record<string, Record<string, string | number | boolean>> = {
        scheduling: schedulingFields.value,
        sqs: sqsFields.value,
        kafka: kafkaFields.value,
        discord: discordFields.value,
      }
      body = fieldMap[category]
    }

    const res = await fetch(`/api/config/${category}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })

    if (res.ok) {
      notify('success', 'Settings saved')
    } else if (res.status === 400) {
      const data = await res.json()
      notify('error', data.error || 'Validation error')
    } else {
      notify('error', 'Failed to save settings. Please try again.')
    }
  } catch {
    notify('error', 'Failed to save settings. Please try again.')
  } finally {
    saving[category] = false
  }
}

onMounted(fetchConfig)
</script>

<style scoped>
.admin-panel {
  position: relative;
}

.admin-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #e0e0e0;
  margin-bottom: 24px;
}

/* Loading state */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
  color: #a0a0b8;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #2a2a4a;
  border-top-color: #bb86fc;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* Error state */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
}

.error-message {
  color: #cf6679;
  font-size: 1.1rem;
}

.retry-button {
  padding: 10px 28px;
  background-color: #bb86fc;
  color: #121212;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-button:hover {
  background-color: #ce9efc;
}

/* Config section (for subreddits custom section) */
.config-section {
  background-color: #1e1e36;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 20px;
}

.config-section-title {
  margin: 0 0 16px 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #e0e0e0;
  border-bottom: 1px solid #2a2a4a;
  padding-bottom: 12px;
}

/* Subreddit list */
.subreddit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.subreddit-entry {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background-color: #16162b;
  border: 1px solid #2a2a4a;
  border-radius: 8px;
}

.subreddit-fields {
  display: flex;
  gap: 12px;
  flex: 1;
}

.subreddit-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.subreddit-field:last-child {
  max-width: 120px;
}

.subreddit-label {
  font-size: 0.8rem;
  color: #a0a0b8;
  font-weight: 500;
}

.field-input {
  width: 100%;
  padding: 8px 12px;
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 6px;
  color: #e0e0e0;
  font-size: 0.9rem;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}

.field-input:focus {
  border-color: #bb86fc;
}

.field-input-error {
  border-color: #cf6679;
}

.field-error {
  font-size: 0.78rem;
  color: #cf6679;
  line-height: 1.3;
}

.remove-button {
  padding: 8px 14px;
  background-color: transparent;
  color: #cf6679;
  border: 1px solid #cf6679;
  border-radius: 6px;
  font-size: 0.8rem;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: 20px;
}

.remove-button:hover {
  background-color: rgba(207, 102, 121, 0.15);
}

.add-button {
  margin-top: 12px;
  padding: 8px 20px;
  background-color: transparent;
  color: #bb86fc;
  border: 1px solid #bb86fc;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.add-button:hover {
  background-color: rgba(187, 134, 252, 0.1);
}

.save-button {
  margin-top: 16px;
  margin-left: 12px;
  padding: 10px 28px;
  background-color: #bb86fc;
  color: #121212;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, opacity 0.2s;
}

.save-button:hover:not(:disabled) {
  background-color: #ce9efc;
}

.save-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.saving-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #121212;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* Toast notifications */
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  border-radius: 8px;
  font-size: 0.9rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  animation: slideIn 0.25s ease-out;
}

.toast-success {
  background-color: #1b5e20;
  color: #c8e6c9;
  border: 1px solid #2e7d32;
}

.toast-error {
  background-color: #4a1c24;
  color: #f8bbd0;
  border: 1px solid #cf6679;
}

.toast-dismiss {
  background: none;
  border: none;
  color: inherit;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0 4px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.toast-dismiss:hover {
  opacity: 1;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
