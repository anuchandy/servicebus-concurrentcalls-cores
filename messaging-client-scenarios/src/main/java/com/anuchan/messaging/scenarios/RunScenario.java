package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.CmdlineArgs;
import com.anuchan.messaging.util.Constants;
import com.anuchan.messaging.util.TopicSubscription;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public abstract class RunScenario {
    @Autowired
    protected CmdlineArgs cmdlineArgs;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    private void postConstruct() {
    }

    public abstract void run();

    protected boolean blockingWait(ClientLogger logger, Duration duration) {
        if (duration.toMillis() <= 0) {
            return true;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            logger.warning("wait interrupted");
            return false;
        }
        return true;
    }

    protected void close(List<ServiceBusProcessorClient> clients) {
        if (clients != null) {
            for (ServiceBusProcessorClient client : clients) {
                close(client);
            }
        }
    }

    protected boolean close(Disposable d) {
        if (d == null) {
            return true;
        }
        try {
            d.dispose();
        } catch (Exception e) {
            return false;
        }
        return true;

    }

    protected boolean close(AutoCloseable c) {
        if (c == null) {
            return true;
        }
        try {
            c.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected String getConnectionStringFromEnvironment() {
        final String connectionString = System.getenv(Constants.AZURE_SERVICEBUS_CONNECTION_STRING);
        if (CoreUtils.isNullOrEmpty(connectionString)) {
            throw new IllegalArgumentException("Environment variable 'AZURE_SERVICEBUS_CONNECTION_STRING' must be set.");
        }
        return connectionString;
    }

    protected List<TopicSubscription> getTopicSubscriptionsFromEnvironment() {
        final String topicSubscriptionEntries = System.getenv(Constants.AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES);
        return parse(topicSubscriptionEntries);
    }

    private List<TopicSubscription> parse(String topicSubscriptionEntries) {
        if (CoreUtils.isNullOrEmpty(topicSubscriptionEntries)) {
            throw new IllegalArgumentException("AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES cannot be null or empty");
        }
        final List<TopicSubscription> topicSubscriptions = new ArrayList<>();
        String[] arr = topicSubscriptionEntries.split(";");
        for (String a : arr) {
            final String entry = a.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid topic subscription entry in AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES: " + entry);
            }
            final String topicName = parts[0].trim();
            final String subscriptionName = parts[1].trim();
            if (topicName.isEmpty() || subscriptionName.isEmpty()) {
                throw new IllegalArgumentException("Invalid topic subscription entry in AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES: " + entry);
            }
            topicSubscriptions.add(new TopicSubscription(topicName, subscriptionName));
        }
        if (topicSubscriptions.isEmpty()) {
            throw new IllegalArgumentException("Invalid topic subscription entries AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION_ENTRIES: " + topicSubscriptionEntries);
        }
        return topicSubscriptions;
    }
}
