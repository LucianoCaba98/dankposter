<template>
  <div class="admin-panel">
    <h2 class="admin-title">Admin Panel</h2>

    <!-- Pipeline Stats Dashboard -->
    <section class="stats-dashboard">
      <h3 class="section-title">Pipeline Overview</h3>
      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">Total Memes</span>
        </div>
        <div class="stat-card stat-posted">
          <span class="stat-value">{{ stats.posted }}</span>
          <span class="stat-label">Posted</span>
        </div>
        <div class="stat-card stat-pending">
          <span class="stat-value">{{ stats.pending }}</span>
          <span class="stat-label">Pending</span>
        </div>
        <div class="stat-card stat-failed">
          <span class="stat-value">{{ stats.failed }}</span>
          <span class="stat-label">Failed</span>
        </div>
      </div>
      <div class="stats-actions">
        <button class="action-btn refresh-btn" @click="fetchStats">↻ Refresh Stats</button>
        <button
          class="action-btn danger-btn"
          :disabled="stats.failed === 0"
          @click="clearFailed"
        >🗑 Clear Failed ({{ stats.failed }})</button>
      </div>
    </section>

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
        title="Discord"
        :fields="discordFields"
        :errors="discordErrors"
        :saving="saving.discord"
        @update:fields="onUpdateDiscord"
        @save="saveCategory('discord')"
      />

      <!-- Reddit Subreddits -->
      <section class="config-section">
        <h3 class="config-section-title">Reddit Subreddits</h3>
        <div class="subreddit-list">
          <div v-for="(sub, index) in subreddits" :key="index" class="subreddit-entry">
            <div class="subreddit-fields">
              <div class="subreddit-field">
                <label class="subreddit-label">Name</label>
                <input type="text" class="field-input"
                  :class="{ 'field-input-error': subredditErrors[index]?.name }"
                  :value="sub.name"
                  @input="onSubredditNameChange(index, ($event.target as HTMLInputElement).value)" />
                <span v-if="subredditErrors[index]?.name" class="field-error">{{ subredditErrors[index].name }}</span>
              </div>
              <div class="subreddit-field">
                <label class="subreddit-label">Limit</label>
                <input type="number" class="field-input"
                  :class="{ 'field-input-error': subredditErrors[index]?.limit }"
                  :value="sub.limit"
                  @input="onSubredditLimitChange(index, Number(($event.target as HTMLInputElement).value))" />
                <span v-if="subredditErrors[index]?.limit" class="field-error">{{ subredditErrors[index].limit }}</span>
              </div>
            </div>
            <button class="remove-button" @click="removeSubreddit(index)">Remove</button>
          </div>
        </div>
        <button class="add-button" @click="addSubreddit">Add Subreddit</button>
        <button class="save-button" :disabled="hasSubredditErrors || saving.redditSubreddits"
          @click="saveCategory('redditSubreddits')">
          <span v-if="saving.redditSubreddits" class="saving-indicator"><span class="spinner"></span> Saving...</span>
          <span v-else>Save</span>
        </button>
      </section>

      <ConfigSection title="SQS" :fields="sqsFields" :errors="sqsErrors" :saving="saving.sqs"
        @update:fields="onUpdateSqs" @save="saveCategory('sqs')" />

      <ConfigSection title="Kafka" :fields="kafkaFields" :errors="kafkaErrors" :saving="saving.kafka"
        @update:fields="onUpdateKafka" @save="saveCategory('kafka')" />
    </template>

    <!-- Toast notifications -->
    <div class="toast-container">
      <div v-for="n in notifications" :key="n.id" class="toast" :class="'toast-' + n.type">
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

const loading = ref(true)
const loadError = ref(false)

// Stats
const stats = reactive({ total: 0, posted: 0, pending: 0, failed: 0 })

async function fetchStats() {
  try {
    const res = await fetch('/api/memes/stats')
    if (res.ok) Object.assign(stats, await res.json())
  } catch { /* silent */ }
}

async function clearFailed() {
  try {
    const res = await fetch('/api/memes/failed', { method: 'DELETE' })
    if (res.ok) {
      const data = await res.json()
      notify('success', `Cleared ${data.deleted} failed memes`)
      await fetchStats()
    }
  } catch {
    notify('error', 'Failed to clear')
  }
}

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

const saving = reactive<Record<string, boolean>>({
  scheduling: false, sqs: false, kafka: false, discord: false, redditSubreddits: false,
})

const hasSubredditErrors = computed(() =>
  subredditErrors.value.some((e) => e.name !== '' || e.limit !== '')
)

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
    validateScheduling(); validateSqs(); validateKafka(); validateDiscord(); validateSubreddits()
  } catch { loadError.value = true }
  finally { loading.value = false }
}

function validateScheduling() {
  const errors: Record<string, string> = {}
  const f = schedulingFields.value
  if (typeof f.fetchIntervalMs === 'number' && f.fetchIntervalMs <= 0) errors.fetchIntervalMs = 'Must be positive'
  if (typeof f.postIntervalMs === 'number' && f.postIntervalMs <= 0) errors.postIntervalMs = 'Must be positive'
  schedulingErrors.value = errors
}
function validateSqs() {
  const errors: Record<string, string> = {}
  const f = sqsFields.value
  if (f.enabled) {
    if (!f.queueUrl || String(f.queueUrl).trim() === '') errors.queueUrl = 'Required when enabled'
    if (!f.dlqUrl || String(f.dlqUrl).trim() === '') errors.dlqUrl = 'Required when enabled'
  }
  sqsErrors.value = errors
}
function validateKafka() {
  const errors: Record<string, string> = {}
  const f = kafkaFields.value
  if (f.enabled && (!f.bootstrapServers || String(f.bootstrapServers).trim() === ''))
    errors.bootstrapServers = 'Required when enabled'
  kafkaErrors.value = errors
}
function validateDiscord() {
  const errors: Record<string, string> = {}
  if (!discordFields.value.channelId || String(discordFields.value.channelId).trim() === '')
    errors.channelId = 'Channel ID is required'
  discordErrors.value = errors
}
function validateSubreddits() {
  subredditErrors.value = subreddits.value.map((sub) => ({
    name: sub.name.trim() === '' ? 'Name is required' : '',
    limit: sub.limit < 1 ? 'Limit must be at least 1' : '',
  }))
}

function onUpdateScheduling(fields: Record<string, string | number | boolean>) { schedulingFields.value = fields; validateScheduling() }
function onUpdateSqs(fields: Record<string, string | number | boolean>) { sqsFields.value = fields; validateSqs() }
function onUpdateKafka(fields: Record<string, string | number | boolean>) { kafkaFields.value = fields; validateKafka() }
function onUpdateDiscord(fields: Record<string, string | number | boolean>) { discordFields.value = fields; validateDiscord() }

function onSubredditNameChange(index: number, value: string) {
  subreddits.value[index] = { ...subreddits.value[index], name: value }; validateSubreddits()
}
function onSubredditLimitChange(index: number, value: number) {
  subreddits.value[index] = { ...subreddits.value[index], limit: value }; validateSubreddits()
}
function addSubreddit() {
  subreddits.value.push({ name: '', limit: 5 })
  subredditErrors.value.push({ name: 'Name is required', limit: '' })
}
function removeSubreddit(index: number) {
  subreddits.value.splice(index, 1); subredditErrors.value.splice(index, 1)
}

async function saveCategory(category: string) {
  saving[category] = true
  try {
    let body: unknown
    if (category === 'redditSubreddits') body = { subreddits: subreddits.value }
    else {
      const m: Record<string, Record<string, string | number | boolean>> = {
        scheduling: schedulingFields.value, sqs: sqsFields.value,
        kafka: kafkaFields.value, discord: discordFields.value,
      }
      body = m[category]
    }
    const res = await fetch(`/api/config/${category}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    })
    if (res.ok) notify('success', 'Settings saved')
    else if (res.status === 400) { const d = await res.json(); notify('error', d.error || 'Validation error') }
    else notify('error', 'Failed to save')
  } catch { notify('error', 'Failed to save') }
  finally { saving[category] = false }
}

onMounted(() => {
  fetchConfig().catch(e => console.error('[AdminPanel] fetchConfig error:', e))
  fetchStats().catch(e => console.error('[AdminPanel] fetchStats error:', e))
})
</script>

<style scoped>
.admin-panel { max-width: 900px; margin: 0 auto; padding: 2rem 1rem; }
.admin-title { font-size: 1.8rem; margin-bottom: 1.5rem; color: #e0e0e0; }
.stats-dashboard { background: #1e1e2e; border-radius: 12px; padding: 1.5rem; margin-bottom: 2rem; }
.section-title { font-size: 1.1rem; color: #ccc; margin-bottom: 1rem; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 1rem; }
.stat-card { background: #2a2a3d; border-radius: 10px; padding: 1.2rem; text-align: center; display: flex; flex-direction: column; gap: 0.3rem; border-left: 3px solid #555; }
.stat-card.stat-posted { border-left-color: #4caf50; }
.stat-card.stat-pending { border-left-color: #ff9800; }
.stat-card.stat-failed { border-left-color: #f44336; }
.stat-value { font-size: 1.8rem; font-weight: 700; color: #fff; }
.stat-label { font-size: 0.8rem; color: #999; text-transform: uppercase; letter-spacing: 0.05em; }
.stats-actions { display: flex; gap: 0.75rem; }
.action-btn { padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; font-size: 0.85rem; transition: background 0.2s; }
.refresh-btn { background: #3a3a5c; color: #ddd; }
.refresh-btn:hover { background: #4a4a6c; }
.danger-btn { background: #5c2a2a; color: #f88; }
.danger-btn:hover { background: #6c3a3a; }
.danger-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.loading-state { text-align: center; padding: 3rem; color: #999; }
.loading-spinner { width: 36px; height: 36px; border: 3px solid #333; border-top-color: #7c6fe0; border-radius: 50%; margin: 0 auto 1rem; animation: spin 0.8s linear infinite; }
.error-state { text-align: center; padding: 2rem; }
.error-message { color: #f88; margin-bottom: 1rem; }
.retry-button { background: #3a3a5c; color: #ddd; border: none; padding: 0.5rem 1.2rem; border-radius: 6px; cursor: pointer; }
.config-section { background: #1e1e2e; border-radius: 12px; padding: 1.5rem; margin-bottom: 1.5rem; }
.config-section-title { font-size: 1.1rem; color: #ccc; margin-bottom: 1rem; }
.subreddit-list { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1rem; }
.subreddit-entry { display: flex; align-items: flex-start; gap: 0.75rem; background: #2a2a3d; padding: 0.75rem; border-radius: 8px; }
.subreddit-fields { display: flex; gap: 0.75rem; flex: 1; }
.subreddit-field { display: flex; flex-direction: column; flex: 1; }
.subreddit-label { font-size: 0.75rem; color: #999; margin-bottom: 0.25rem; }
.field-input { background: #16161e; border: 1px solid #3a3a5c; color: #e0e0e0; padding: 0.5rem 0.75rem; border-radius: 6px; font-size: 0.9rem; outline: none; transition: border-color 0.2s; }
.field-input:focus { border-color: #7c6fe0; }
.field-input-error { border-color: #f44336; }
.field-error { font-size: 0.7rem; color: #f88; margin-top: 0.2rem; }
.remove-button { background: #5c2a2a; color: #f88; border: none; padding: 0.4rem 0.8rem; border-radius: 6px; cursor: pointer; font-size: 0.8rem; margin-top: 1.1rem; }
.remove-button:hover { background: #6c3a3a; }
.add-button { background: #2a3a2a; color: #8f8; border: none; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.85rem; margin-right: 0.75rem; }
.add-button:hover { background: #3a4a3a; }
.save-button { background: #7c6fe0; color: #fff; border: none; padding: 0.5rem 1.5rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; transition: background 0.2s; }
.save-button:hover { background: #6b5ed0; }
.save-button:disabled { opacity: 0.5; cursor: not-allowed; }
.saving-indicator { display: inline-flex; align-items: center; gap: 0.4rem; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.6s linear infinite; }
.toast-container { position: fixed; bottom: 1.5rem; right: 1.5rem; display: flex; flex-direction: column; gap: 0.5rem; z-index: 9999; }
.toast { display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem 1rem; border-radius: 8px; font-size: 0.9rem; animation: slideIn 0.3s ease; min-width: 250px; }
.toast-success { background: #1e3a1e; color: #8f8; }
.toast-error { background: #3a1e1e; color: #f88; }
.toast-dismiss { background: none; border: none; color: inherit; font-size: 1.2rem; cursor: pointer; margin-left: auto; opacity: 0.7; }
.toast-dismiss:hover { opacity: 1; }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
@media (max-width: 600px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .subreddit-fields { flex-direction: column; } }
</style>
