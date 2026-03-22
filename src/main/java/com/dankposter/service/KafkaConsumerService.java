package com.dankposter.service;

import com.dankposter.dto.MemeDeliveryEvent;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "meme.stream.kafka.enabled", havingValue = "true")
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;
    private final MemeRepository memeRepository;
    private final DiscordPosterService discordPosterService;

    @KafkaListener(
            topics = "#{@kafkaProperties.topic}",
            groupId = "#{@kafkaProperties.consumerGroup}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.debug("Received Kafka message: key={}, topic={}, partition={}, offset={}",
                record.key(), record.topic(), record.partition(), record.offset());
        MemeDeliveryEvent event;
        try {
            event = objectMapper.readValue(record.value(), MemeDeliveryEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize MemeDeliveryEvent: value={}", record.value(), e);
            acknowledgment.acknowledge();
            return;
        }
        Optional<Meme> memeOpt = memeRepository.findById(event.memeId());
        if (memeOpt.isEmpty()) {
            log.warn("Meme not found for delivery event: memeId={}", event.memeId());
            acknowledgment.acknowledge();
            return;
        }
        Meme meme = memeOpt.get();
        if (meme.getStatus() == MemeStatus.POSTED) {
            log.debug("Meme already posted, skipping: memeId={}", event.memeId());
            acknowledgment.acknowledge();
            return;
        }
        try {
            discordPosterService.post(meme).block();
            meme.setStatus(MemeStatus.POSTED);
            memeRepository.save(meme);
            log.info("Successfully delivered meme via Kafka: memeId={}", event.memeId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to post meme to Discord: memeId={}", event.memeId(), e);
        }
    }
}
