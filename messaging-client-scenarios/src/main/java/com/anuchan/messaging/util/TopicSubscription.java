package com.anuchan.messaging.util;

import java.util.Objects;

public final class TopicSubscription {
    private final String topicName;
    private final String subscriptionName;

    public TopicSubscription(String topicName, String subscriptionName) {
        this.topicName = Objects.requireNonNull(topicName, "topicName cannot be null");
        this.subscriptionName = Objects.requireNonNull(subscriptionName, "subscriptionName cannot be null");
    }

    public String getTopicName() {
        return topicName;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }
}
