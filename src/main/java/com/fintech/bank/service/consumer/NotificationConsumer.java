package com.fintech.bank.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.config.BankingTopics;
import com.fintech.bank.dto.DomainEvent;
import com.fintech.bank.dto.TransactionCompletedEventPayload;
import com.fintech.bank.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Notification Service Consumer.
 * Consumes TransactionCompleted events and dispatches SMS/Email notifications
 * to both source and destination account holders.
 */
@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final com.fintech.bank.repository.AccountRepository accountRepository;

    public NotificationConsumer(ObjectMapper objectMapper,
                                NotificationService notificationService,
                                com.fintech.bank.repository.AccountRepository accountRepository) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.accountRepository = accountRepository;
    }

    @KafkaListener(topics = BankingTopics.TRANSACTION_COMPLETED, groupId = BankingTopics.GROUP_NOTIFICATIONS)
    public void handleTransactionCompleted(DomainEvent<Object> event) {
        log.info("[NOTIFICATION SERVICE] Received TransactionCompleted event [EventId: {}, AggregateId: {}]",
                event.getEventId(), event.getAggregateId());

        try {
            TransactionCompletedEventPayload payload;
            if (event.getPayload() instanceof Map) {
                payload = objectMapper.convertValue(event.getPayload(), TransactionCompletedEventPayload.class);
            } else {
                payload = objectMapper.readValue(objectMapper.writeValueAsString(event.getPayload()), TransactionCompletedEventPayload.class);
            }

            String eventIdStr = event.getEventId() != null ? event.getEventId().toString() : payload.getTransactionReference();

            // 1. Notify Sender (Debit)
            accountRepository.findByAccountNumber(payload.getSourceAccountNumber()).ifPresent(srcAcc -> {
                java.util.UUID senderUserId = srcAcc.getCustomerProfile().getUserAuth().getId();
                String title = "Funds Debited: " + payload.getAmount() + " " + payload.getCurrency();
                String message = String.format("Your account %s was debited by %s %s for transfer to account %s [Ref: %s].",
                        payload.getSourceAccountNumber(), payload.getAmount(), payload.getCurrency(),
                        payload.getDestinationAccountNumber(), payload.getTransactionReference());

                notificationService.createNotification(
                        senderUserId,
                        com.fintech.bank.entity.NotificationType.TRANSFER_SUCCESSFUL,
                        title,
                        message,
                        com.fintech.bank.entity.NotificationChannel.IN_APP,
                        payload.getTransactionReference(),
                        eventIdStr + "-DEBIT"
                );
            });

            // 2. Notify Beneficiary (Credit)
            accountRepository.findByAccountNumber(payload.getDestinationAccountNumber()).ifPresent(dstAcc -> {
                java.util.UUID receiverUserId = dstAcc.getCustomerProfile().getUserAuth().getId();
                String title = "Funds Received: " + payload.getAmount() + " " + payload.getCurrency();
                String message = String.format("Your account %s was credited with %s %s from account %s [Ref: %s].",
                        payload.getDestinationAccountNumber(), payload.getAmount(), payload.getCurrency(),
                        payload.getSourceAccountNumber(), payload.getTransactionReference());

                notificationService.createNotification(
                        receiverUserId,
                        com.fintech.bank.entity.NotificationType.TRANSFER_SUCCESSFUL,
                        title,
                        message,
                        com.fintech.bank.entity.NotificationChannel.IN_APP,
                        payload.getTransactionReference(),
                        eventIdStr + "-CREDIT"
                );
            });

        } catch (Exception e) {
            log.error("[NOTIFICATION SERVICE] Error processing notification event: {}", event.getEventId(), e);
            throw new RuntimeException("Notification processing failed for event: " + event.getEventId(), e);
        }
    }
}
