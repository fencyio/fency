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
package io.fency.sample;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @author Gilles Robert
 */
@Configuration
public class RabbitSpringConfig {

  private static final String EXCHANGE = "myExchange";

  @Bean
  public RabbitTransactionManager transactionManager(CachingConnectionFactory connectionFactory){
    return new RabbitTransactionManager(connectionFactory);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(CachingConnectionFactory cachingConnectionFactory) {
    SimpleMessageConverter messageConverter = new SimpleMessageConverter();
    messageConverter.setCreateMessageIds(true); //important

    RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
    rabbitTemplate.setExchange(EXCHANGE);
    rabbitTemplate.setMessageConverter(messageConverter);

    return rabbitTemplate;
  }

  @Bean
  public MessageListener messageListener() {
    return new MessageListener();
  }

  @Bean
  public SimpleMessageListenerContainer messageListenerContainer(AmqpAdmin admin,
      CachingConnectionFactory cachingConnectionFactory, MessageListener messageListener,
      TransactionInterceptor interceptor, PlatformTransactionManager transactionManager) {

    Queue queue = new Queue("myQueue", false);
    declareExchangeAndQueue(admin, queue);

    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cachingConnectionFactory);
    container.setQueues(queue);
    container.setMessageListener(new MessageListenerAdapter(messageListener));
    container.setAdviceChain(interceptor);
    container.setConcurrentConsumers(5);
    container.setTransactionManager(transactionManager);

    return container;
  }

  private void declareExchangeAndQueue(AmqpAdmin admin, Queue queue) {
    TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);

    admin.declareQueue(queue);
    admin.declareExchange(exchange);
    admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));
  }
}
