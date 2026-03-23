export interface Meme {
  id: number
  externalId: string
  title: string
  imageUrl: string
  posted: boolean
  description: string | null
  source: string | null
  liked: boolean
}

export interface ConfigCategory {
  [key: string]: string | number | boolean
}

export interface AppConfig {
  scheduling: { fetchIntervalMs: number; postIntervalMs: number }
  sqs: { enabled: boolean; queueUrl: string; dlqUrl: string; region: string; pollInterval: number }
  kafka: { bootstrapServers: string; topic: string; consumerGroup: string }
  discord: { botToken: string; channelId: string }
  redditSubreddits: Array<{ name: string; limit: number }>
}

export interface ValidationError {
  field: string
  message: string
}

export interface KafkaMessageEvent {
  topic: string
  partition: number | null
  offset: number | null
  key: string
  timestamp: string | null
  payload: string
  deliveryStatus: 'produced' | 'consumed' | 'delivered' | 'failed'
  capturedAt: string
}
