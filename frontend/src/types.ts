export interface Meme {
  id: number
  redditId: string
  title: string
  imageUrl: string
  danknessScore: number
  posted: boolean
  description: string | null
}

export interface ConfigCategory {
  [key: string]: string | number | boolean
}

export interface AppConfig {
  scheduling: { fetchIntervalMs: number; postIntervalMs: number }
  sqs: { enabled: boolean; queueUrl: string; dlqUrl: string; region: string; pollInterval: number }
  kafka: { enabled: boolean; bootstrapServers: string; topic: string; consumerGroup: string }
  discord: { botToken: string; channelId: string }
  redditSubreddits: Array<{ name: string; limit: number }>
}

export interface ValidationError {
  field: string
  message: string
}
