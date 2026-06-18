package com.orderflow.analytics.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KafkaAdminService {

    @Value("${kafka.admin.bootstrap-servers}")
    private String bootstrapServers;

    public Map<String, ConsumerGroupLag> getConsumerGroupLags(String consumerGroup) {
        Map<String, ConsumerGroupLag> lags = new HashMap<>();
        AdminClient adminClient = null;

        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            adminClient = AdminClient.create(props);

            var groupOffsetsResult = adminClient.listConsumerGroupOffsets(consumerGroup,
                    new ListConsumerGroupOffsetsOptions());
            var groupOffsets = groupOffsetsResult.partitionsToOffsetAndMetadata().get();

            Set<TopicPartition> topicPartitions = groupOffsets.keySet();

            var logEndOffsetsResult = adminClient.listOffsets(
                    topicPartitions.stream()
                            .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest())));
            var logEndOffsets = logEndOffsetsResult.all().get();

            for (TopicPartition tp : topicPartitions) {
                String topic = tp.topic();
                long consumerOffset = groupOffsets.get(tp).offset();
                long logEndOffset = logEndOffsets.get(tp).offset();
                long lag = logEndOffset - consumerOffset;

                ConsumerGroupLag groupLag = ConsumerGroupLag.builder()
                        .topic(topic)
                        .partition(tp.partition())
                        .consumerOffset(consumerOffset)
                        .logEndOffset(logEndOffset)
                        .lag(lag)
                        .build();

                lags.put(topic + "-" + tp.partition(), groupLag);

                log.debug("Consumer group {} - Topic: {}, Partition: {}, Lag: {}",
                        consumerGroup, topic, tp.partition(), lag);
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error fetching consumer group lag for {}", consumerGroup, e);
        } finally {
            if (adminClient != null) {
                adminClient.close();
            }
        }

        return lags;
    }

    public static class ConsumerGroupLag {
        public String topic;
        public int partition;
        public long consumerOffset;
        public long logEndOffset;
        public long lag;

        private ConsumerGroupLag(Builder builder) {
            this.topic = builder.topic;
            this.partition = builder.partition;
            this.consumerOffset = builder.consumerOffset;
            this.logEndOffset = builder.logEndOffset;
            this.lag = builder.lag;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String topic;
            private int partition;
            private long consumerOffset;
            private long logEndOffset;
            private long lag;

            public Builder topic(String topic) {
                this.topic = topic;
                return this;
            }

            public Builder partition(int partition) {
                this.partition = partition;
                return this;
            }

            public Builder consumerOffset(long consumerOffset) {
                this.consumerOffset = consumerOffset;
                return this;
            }

            public Builder logEndOffset(long logEndOffset) {
                this.logEndOffset = logEndOffset;
                return this;
            }

            public Builder lag(long lag) {
                this.lag = lag;
                return this;
            }

            public ConsumerGroupLag build() {
                return new ConsumerGroupLag(this);
            }
        }
    }
}
