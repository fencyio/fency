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
package io.fency.sample;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gilles Robert
 */
@Component
@Transactional
public class SampleApplicationRunner implements ApplicationRunner {

  private final RabbitTemplate rabbitTemplate;

  public SampleApplicationRunner(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    sendMessageWithUniqueId();
    sendMessageWithFixedId(); // duplicate
  }

  private void sendMessageWithUniqueId() {
    rabbitTemplate.convertAndSend("hello world!");
  }

  private void sendMessageWithFixedId() {
    byte[] body = "message".getBytes(Charset.defaultCharset());
    MessageProperties properties = new MessageProperties();
    properties.setTimestamp(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    properties.setMessageId("12345");
    properties.setConsumerQueue("myQueue");
    properties.setType(String.class.getName());
    Message message = MessageBuilder.withBody(body)
        .andProperties(properties)
        .build();

    rabbitTemplate.send(message);
  }
}
