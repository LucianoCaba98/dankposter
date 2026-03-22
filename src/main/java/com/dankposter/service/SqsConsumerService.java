package com.dankposter.service;

import com.dankposter.config.SqsProperties;
import com.dankposter.dto.MemeDeliveryEvent;
import com.dankposter.dto.MemeMessage;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;
import com.dankposter.model.Source;
import com.dankposter.repository.MemeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(name = "meme.queue.sqs.enabled", havingValue = "true")
public class SqsConsumerService {

    private final SqsTemplate sqsTemplate;
    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final SqsProperties sqsProperties;
    private final MemeRepository memeRepository;
    private final Optional<KafkaProducerService> kafkaProducerService;

    public SqsConsumerService(SqsTemplate sqsTemplate, SqsAsyncClient sqsAsyncClient,
                              ObjectMapper objectMapper, SqsProperties sqsProperties,
                              MemeRepository memeRepository,
                              Optional<KafkaProducerService> kafkaProducerService) {
        this.sqsTemplate = sqsTemplate;
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.sqsProperties = sqsProperties;
        this.memeRepository = memeRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Scheduled(fixedDelayString = "${meme.queue.sqs.poll-interval}")
    public void pollMessages() {
        log.debug("Polling SQS queue: {}", sqsProperties.getQueueUrl());
        try {
            Optional<Message<?>> optionalMessage = sqsTemplate.receive(
                    options -> options.queue(sqsProperties.getQueueUrl()));
            if (optionalMessage.isEmpty()) return;
            processMessage(optionalMessage.get());
        } catch (Exception e) {
            log.error("Error polling SQS queue: {}", sqsProperties.getQueueUrl(), e);
        }
    }

    private void processMessage(Message<?> message) {
        String body = (String) message.getPayload();
        String receiptHandle = message.getHeaders().get("Sqs_receiptHandle", String.class);
        try {
            MemeMessage memeMessage = objectMapper.readValue(body, MemeMessage.class);
            if (memeRepository.existsByExternalId(memeMessage.sourceSpecificId())) {
                log.debug("Duplicate meme, acknowledging: externalId={}", memeMessage.sourceSpecificId());
                deleteMessage(receiptHandle);
                return;
            }
            Meme meme = Meme.builder()
                    .externalId(memeMessage.sourceSpecificId())
                    .title(memeMessage.title())
                    .imageUrl(memeMessage.imageUrl())
                    .danknessScore(memeMessage.danknessScore())
                    .status(MemeStatus.FETCHED)
                    .source(Source.REDDIT)
                    .build();
            Meme savedMeme = memeRepository.save(meme);
            log.info("Persisted meme from SQS: id={}, externalId={}", savedMeme.getId(), memeMessage.sourceSpecificId());
            deleteMessage(receiptHandle);
            kafkaProducerService.ifPresent(producer -> {
                MemeDeliveryEvent event = new MemeDeliveryEvent(
                        savedMeme.getId(), savedMeme.getTitle(),
                        savedMeme.getImageUrl(), savedMeme.getDanknessScore());
                producer.publishDeliveryEvent(event);
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize MemeMessage from SQS: body={}", body, e);
        } catch (Exception e) {
            log.error("Failed to process SQS message: body={}", body, e);
        }
    }

    private void deleteMessage(String receiptHandle) {
        try {
            sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(sqsProperties.getQueueUrl())
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete SQS message: receiptHandle={}", receiptHandle, e);
        }
    }
}
