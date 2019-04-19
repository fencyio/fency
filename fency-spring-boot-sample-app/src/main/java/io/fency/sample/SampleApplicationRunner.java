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

  /*
  Sends messages at application startup: with unique ID's and fixed ID's.
  - Messages with unique ID's will all be received.
  - Only the first with fixed ID will be received.
  */
  @Override
  public void run(ApplicationArguments args) {
    for (int i = 1; i <= 5; i++) {
      String messageWithUniqueId = concat(i, "hello world");
      sendMessage(messageWithUniqueId);

      Message messageWithFixedId = createMessageWithFixedId(concat(i, "message"));
      sendMessage(messageWithFixedId); // first will be received, second one will be discarded
    }
  }

  private String concat(int i, String message) {
    return String.join("-", message, String.valueOf(i));
  }

  private void sendMessage(Object body) {
    rabbitTemplate.convertAndSend(body);
  }

  private Message createMessageWithFixedId(String message) {
    byte[] body = message.getBytes(Charset.defaultCharset());
    MessageProperties properties = new MessageProperties();
    properties.setTimestamp(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
    properties.setMessageId("12345");
    properties.setConsumerQueue("myQueue");
    properties.setType(String.class.getName());
    properties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);

    return MessageBuilder.withBody(body)
        .andProperties(properties)
        .build();
  }
}
