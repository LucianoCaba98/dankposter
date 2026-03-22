package com.shitpostengine.dank.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shitpostengine.dank.config.SqsProperties;
import com.shitpostengine.dank.dto.MemeDeliveryEvent;
import com.shitpostengine.dank.dto.MemeMessage;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.repository.MemeRepository;
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

    public SqsConsumerService(SqsTemplate sqsTemplate,
                              SqsAsyncClient sqsAsyncClient,
                              ObjectMapper objectMapper,
                              SqsProperties sqsProperties,
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
        log.debug("Polling SQS queue for messages: {}", sqsProperties.getQueueUrl());
        try {
            Optional<Message<?>> optionalMessage = sqsTemplate.receive(
                    options -> options.queue(sqsProperties.getQueueUrl()));
            if (optionalMessage.isEmpty()) {
                return;
            }
            Message<?> message = optionalMessage.get();
            processMessage(message);
        } catch (Exception e) {
            log.error("Error polling SQS queue: {}", sqsProperties.getQueueUrl(), e);
        }
    }

    private void processMessage(Message<?> message) {
        String body = (String) message.getPayload();
        String receiptHandle = message.getHeaders().get("Sqs_receiptHandle", String.class);

        try {
            MemeMessage memeMessage = objectMapper.readValue(body, MemeMessage.class);

            if (memeRepository.existsByRedditId(memeMessage.sourceSpecificId())) {
                log.debug("Duplicate meme detected, acknowledging: sourceSpecificId={}",
                        memeMessage.sourceSpecificId());
                deleteMessage(receiptHandle);
                return;
            }

            Meme meme = Meme.builder()
                    .redditId(memeMessage.sourceSpecificId())
                    .title(memeMessage.title())
                    .imageUrl(memeMessage.imageUrl())
                    .danknessScore(memeMessage.danknessScore())
                    .posted(false)
                    .build();

            Meme savedMeme = memeRepository.save(meme);
            log.info("Persisted meme from SQS: id={}, sourceSpecificId={}",
                    savedMeme.getId(), memeMessage.sourceSpecificId());

            deleteMessage(receiptHandle);

            kafkaProducerService.ifPresent(producer -> {
                MemeDeliveryEvent event = new MemeDeliveryEvent(
                        savedMeme.getId(),
                        savedMeme.getTitle(),
                        savedMeme.getImageUrl(),
                        savedMeme.getDanknessScore()
                );
                producer.publishDeliveryEvent(event);
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize MemeMessage from SQS: body={}", body, e);
            // Leave message on queue for SQS retry → DLQ routing
        } catch (Exception e) {
            log.error("Failed to process SQS message: body={}", body, e);
            // Leave message on queue for SQS retry → DLQ routing
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
