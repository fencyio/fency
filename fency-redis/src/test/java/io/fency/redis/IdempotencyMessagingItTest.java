/*
 * Copyright 2016-2019 the original author or authors.
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
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import io.fency.IdempotencyAutoConfiguration;
import io.fency.IdempotencyTestUtils;
import io.fency.Message;
import io.fency.MessageListener;
import io.fency.MessageService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Gilles Robert
 */
@EnableAutoConfiguration
@SpringBootTest(classes = {RedisAutoConfiguration.class, IdempotencyAutoConfiguration.class,
    IdempotencyMessagingItTest.TestConfig.class}, properties = {
    "spring.datasource.url=jdbc:tc:postgresql:11-alpine:///foo",
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
    "spring.datasource.username:foo",
    "spring.datasource.password:foo",
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class IdempotencyMessagingItTest {

  private static final int RABBIT_MQ_PORT = 5672;

  @Container
  private static final GenericContainer REDIS = new GenericContainer("redis:3-alpine")
      .withExposedPorts(6379);

  @Container
  private static final GenericContainer RABBIT = new GenericContainer("rabbitmq:3.5.3")
      .withExposedPorts(RABBIT_MQ_PORT);

  @Autowired
  private RedisConnectionFactory factory;
  @Autowired
  private MessageService messageService;
  @Autowired
  private RabbitTemplate rabbitTemplate;
  @Autowired
  private RedisOperations<String, Message> operations;

  @BeforeAll
  public static void setUp() {
    REDIS.start();
    RABBIT.start();
  }

  @AfterAll
  public static void close() {
    REDIS.stop();
    RABBIT.stop();
  }

  @AfterEach
  public void tearDown() {
    factory.getConnection().flushDb();
  }

  @Test
  @DisplayName("Save and retrieve a message from Redis")
  public void testSaveAndRetrieveMessage() {
    // given
    Message message = IdempotencyTestUtils.createIdempotentMessage();

    // when
    messageService.save(message);

    // then
    assertThat(messageService.find(message.getId(), message.getConsumerQueueName())).isNotNull();
  }

  @Test
  @DisplayName("Send a new message to RabbitMQ")
  public void testSendOneMessageToRabbit() { // NOPMD: no assert.
    // given
    String message = "hello world message!";

    // when
    rabbitTemplate.convertAndSend(message);

    // then
    await()
        .until(() -> operations.keys("*").size() == 1);
  }

  @Test
  @DisplayName("Send two messages to RabbitMQ")
  public void testSendTwoMessagesToRabbit() { // NOPMD: no assert.
    // given
    String message1 = "hello world message!";
    String message2 = "hello world message!";

    // when
    rabbitTemplate.convertAndSend(message1);
    rabbitTemplate.convertAndSend(message2);

    // then
    await()
        .until(() -> operations.keys("*").size() == 2);
  }

  @Test
  @DisplayName("Send message and creates and exception")
  public void testSendMessageWhenException() {
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
    public RabbitTemplate rabbitTemplate(RabbitConnectionFactoryBean rabbitConnectionFactoryBean)
        throws Exception {
      final CachingConnectionFactory cachingConnectionFactory =
          new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
      SimpleMessageConverter messageConverter = new SimpleMessageConverter();
      messageConverter.setCreateMessageIds(true);
      RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
      rabbitTemplate.setExchange("myExchange");
      rabbitTemplate.setMessageConverter(messageConverter);
      return rabbitTemplate;
    }

    @Bean
    public RabbitConnectionFactoryBean rabbitConnectionFactoryBean() {
      RabbitConnectionFactoryBean rabbitConnectionFactoryBean = new RabbitConnectionFactoryBean();
      rabbitConnectionFactoryBean.setUsername("guest");
      rabbitConnectionFactoryBean.setPassword("guest");
      rabbitConnectionFactoryBean.setHost("localhost");
      rabbitConnectionFactoryBean.setVirtualHost("/");
      rabbitConnectionFactoryBean.setPort(RABBIT_MQ_PORT);
      rabbitConnectionFactoryBean.afterPropertiesSet();
      return rabbitConnectionFactoryBean;
    }

    @Bean
    public CachingConnectionFactory cachingConnectionFactory(
        RabbitConnectionFactoryBean rabbitConnectionFactoryBean) throws Exception {
      return new CachingConnectionFactory(rabbitConnectionFactoryBean.getObject());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(Queue queue, CachingConnectionFactory cachingConnectionFactory) {
      final TopicExchange exchange = new TopicExchange("myExchange", true, false);

      final RabbitAdmin admin = new RabbitAdmin(cachingConnectionFactory);
      admin.declareQueue(queue);
      admin.declareExchange(exchange);
      admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));

      return admin;
    }

    @Bean
    public MessageListener messageListenerTest() {
      return new MessageListener();
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer(
        CachingConnectionFactory cachingConnectionFactory, Queue queue, MessageListener messageListener,
        TransactionInterceptor interceptor, PlatformTransactionManager transactionManager) {
      SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cachingConnectionFactory);
      container.setQueues(queue);
      container.setMessageListener(new MessageListenerAdapter(messageListener));
      container.setAdviceChain(interceptor);
      container.setTransactionManager(transactionManager);

      return container;
    }
  }
}
