/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fency.redis;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import io.fency.IdempotencyAutoConfiguration;
import io.fency.IdempotencyTestUtils;
import io.fency.Message;
import io.fency.MessageListener;
import io.fency.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Gilles Robert
 */
@EnableAutoConfiguration
@SpringBootTest(classes = {RedisAutoConfiguration.class, IdempotencyAutoConfiguration.class,
    IdempotencyMessagingItTest.TestConfig.class})
@ContextConfiguration(initializers = IdempotencyMessagingItTest.Initializer.class)
@Testcontainers
class IdempotencyMessagingItTest {

  private static final int RABBIT_MQ_PORT = 5672;
  private static final int REDIS_PORT = 6379;

  @Container
  private static final GenericContainer REDIS = new GenericContainer("redis:3-alpine")
      .withExposedPorts(REDIS_PORT);

  @Container
  private static final GenericContainer RABBIT = new GenericContainer("rabbitmq:3.5.7")
      .withExposedPorts(RABBIT_MQ_PORT);

  @Autowired
  private RedisConnectionFactory factory;
  @Autowired
  private MessageService messageService;
  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private RedisOperations<String, Message> operations;

  @AfterEach
  void tearDown() {
    factory.getConnection().flushDb();
  }

  @Test
  @DisplayName("Save and retrieve a message from Redis")
  void testSaveAndRetrieveMessage() {
    // given
    Message message = IdempotencyTestUtils.createIdempotentMessage();

    // when
    messageService.save(message);

    // then
    assertThat(messageService.find(message.getId(), message.getConsumerQueueName())).isNotNull();
  }

  @Test
  @DisplayName("Send a new message to RabbitMQ")
  void testSendOneMessageToRabbit() { // NOPMD: no assert.
    // given
    String message = "one message";

    // when
    rabbitTemplate.convertAndSend(message);

    // then
    await()
        .until(() -> operations.keys("*").size() == 1);
  }

  @Test
  @DisplayName("Send two messages to RabbitMQ")
  void testSendTwoMessagesToRabbit() { // NOPMD: no assert.
    // given
    String message1 = "one message of two";
    String message2 = "second message";

    // when
    rabbitTemplate.convertAndSend(message1);
    rabbitTemplate.convertAndSend(message2);

    // then
    await()
        .until(() -> operations.keys("*").size() == 2);
  }

  @Test
  @DisplayName("Send message and creates and exception")
  void testSendMessageWhenException() {
    // given
    String message = "exception";

    // when
    rabbitTemplate.convertAndSend(message);

    // then
    assertThat(operations.keys("*")).isEmpty();
  }

  @Configuration
  static class TestConfig {

    @Bean
    public RabbitTransactionManager transactionManager(CachingConnectionFactory connectionFactory) {
      return new RabbitTransactionManager(connectionFactory);
    }

    @Bean
    public Queue queue() {
      return new Queue("myQueue", false);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
      SimpleMessageConverter messageConverter = new SimpleMessageConverter();
      messageConverter.setCreateMessageIds(true);

      RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
      rabbitTemplate.setExchange("myExchange");
      rabbitTemplate.setMessageConverter(messageConverter);

      return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(Queue myQueue, ConnectionFactory connectionFactory) {
      TopicExchange myExchange = new TopicExchange("myExchange", true, false);

      RabbitAdmin admin = new RabbitAdmin(connectionFactory);
      admin.declareQueue(myQueue);
      admin.declareExchange(myExchange);
      admin.declareBinding(BindingBuilder.bind(myQueue).to(myExchange).with("#"));

      return admin;
    }

    @Bean
    public MessageListener messageListenerTest() {
      return new MessageListener();
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(
        CachingConnectionFactory cachingConnectionFactory, Queue myQueue, MessageListener messageListener,
        TransactionInterceptor interceptor, PlatformTransactionManager transactionManager) {
      SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cachingConnectionFactory);
      container.setQueues(myQueue);
      container.setMessageListener(new MessageListenerAdapter(messageListener));
      container.setAdviceChain(interceptor);
      container.setTransactionManager(transactionManager);

      return container;
    }
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues values = TestPropertyValues.of(
          "spring.rabbitmq.host=" + RABBIT.getContainerIpAddress(),
          "spring.rabbitmq.port=" + RABBIT.getMappedPort(RABBIT_MQ_PORT),
          "spring.rabbitmq.user=" + "guest",
          "spring.rabbitmq.password=" + "guest",
          "spring.rabbitmq.virtual-host=" + "/",
          "spring.redis.host=" + REDIS.getContainerIpAddress(),
          "spring.redis.port=" + REDIS.getMappedPort(REDIS_PORT)
      );
      values.applyTo(configurableApplicationContext);
    }
  }
}
