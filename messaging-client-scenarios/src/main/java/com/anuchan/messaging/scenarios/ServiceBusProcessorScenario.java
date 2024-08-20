package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.Constants;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.anuchan.messaging.util.Constants.TEST_DURATION;

@Service
public class ServiceBusProcessorScenario extends RunScenario {
    private final ClientLogger clientLogger = new ClientLogger(ServiceBusProcessorScenario.class);

    private final String connectionString = System.getenv(Constants.AZURE_SERVICEBUS_CONNECTION_STRING);
    private final String queueName = System.getenv(Constants.AZURE_SERVICEBUS_QUEUE_NAME);

    private ServiceBusProcessorClient client;

    @Override
    public void run() {
        try {
            client = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .processor()
                    .queueName(queueName)
                    .disableAutoComplete()
                    .maxAutoLockRenewDuration(Duration.ofMinutes(5))
                    .maxConcurrentCalls(45)
                    .prefetchCount(0)
                    .processMessage(this::process)
                    .processError(this::processError)
                    .buildProcessorClient();

            client.start();
            blockingWait(clientLogger, TEST_DURATION.plusSeconds(5));
            client.stop();
        } finally {
            close(client);
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

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
