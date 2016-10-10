package com.heroku.kafka.demo;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

public class DemoProducer implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(DemoProducer.class);

  private final KafkaConfig config;

  private final MetricRegistry metrics;

  private Producer<String, String> producer;

  public DemoProducer(KafkaConfig config, MetricRegistry metrics) {
    this.config = config;
    this.metrics = metrics;
  }

  public void start() throws Exception {
    LOG.info("starting");
    Properties properties = config.getProperties();
    properties.put(ProducerConfig.ACKS_CONFIG, "all");
    properties.put(ProducerConfig.RETRIES_CONFIG, 0);
    properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
    properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    producer = new KafkaProducer<>(properties);

    producer.metrics().forEach((name, metric) -> {
      Gauge<Double> gauge = () -> metric.value();
      metrics.register(MetricRegistry.name(DemoProducer.class, name.name()), gauge);
    });
    LOG.info("started");
  }

  public Future<RecordMetadata> send(String message) {
    LOG.info("Producer sending message: " + message);
    Future<RecordMetadata> future = producer.send(new ProducerRecord<>(config.getTopic(), message, message),
            new Callback() {
              public void onCompletion(RecordMetadata metadata, Exception e) {
                LOG.info("\tSent: '%s'\n" +
                                "\t\ttopic = %s\n" +
                                "\t\tpartition = %d\n" +
                                "\t\toffset = %d\n",
                        message,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset());
              }

            });

    return future;
  }

  public void stop() throws Exception {
    LOG.info("stopping");
    Producer<String, String> producer = this.producer;
    this.producer = null;
    LOG.info("closing producer");
    producer.close();
    LOG.info("stopped");
  }
}
