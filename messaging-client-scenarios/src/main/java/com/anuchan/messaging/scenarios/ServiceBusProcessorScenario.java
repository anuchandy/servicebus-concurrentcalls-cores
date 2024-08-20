package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.TopicSubscription;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.anuchan.messaging.util.Constants.TEST_DURATION;

@Service
public class ServiceBusProcessorScenario extends RunScenario {
    private final ClientLogger clientLogger = new ClientLogger(ServiceBusProcessorScenario.class);

    @Override
    public void run() {
        final String connectionString = super.getConnectionStringFromEnvironment();
        final List<TopicSubscription> topicSubscriptionEntries = super.getTopicSubscriptionsFromEnvironment();
        final List<ServiceBusProcessorClient> clients = new ArrayList<>(topicSubscriptionEntries.size());

        try {
            for (TopicSubscription topicSubscription : topicSubscriptionEntries) {
                final ServiceBusProcessorClient client = new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .processor()
                        .topicName(topicSubscription.getTopicName())
                        .subscriptionName(topicSubscription.getSubscriptionName())
                        .disableAutoComplete()
                        .maxAutoLockRenewDuration(Duration.ofMinutes(5))
                        .maxConcurrentCalls(45)
                        .prefetchCount(0)
                        .processMessage(this::process)
                        .processError(this::processError)
                        .buildProcessorClient();
                clients.add(client);
            }

            for (ServiceBusProcessorClient client : clients) {
                client.start();
            }
            blockingWait(clientLogger, TEST_DURATION.plusSeconds(5));
            for (ServiceBusProcessorClient client : clients) {
                client.close();
            }
        } finally {
            close(clients);
        }
    }

    private void process(ServiceBusReceivedMessageContext messageContext) {
        settleMessage(messageContext);
    }

    private void settleMessage(ServiceBusReceivedMessageContext messageContext) {
        try {
            messageContext.complete();
        } catch (Throwable e) {
            clientLogger.atError().log("Error occurred while settling message.", e);
        }
    }

    private void processError(ServiceBusErrorContext errorContext) {
        clientLogger.atError().log("processError", errorContext.getException());
    }
}
