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
package io.fency;

import java.util.Optional;

import org.springframework.core.annotation.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Aspect on {@link IdempotentConsumer}.
 * Retrieves a {@link MessageContext} and uses a MessageService to verify if the message
 * metadata are already in the data store. Ignore the message if it's the case and store its metadata otherwise.
 *
 * @author Gilles Robert
 */
@Aspect
@Order
@Slf4j
@RequiredArgsConstructor
class IdempotencyBarrier {

  private final ContextService contextService;
  private final MessageService messageService;

  // CHECKSTYLE:OFF throwable for ProceedingJoinPoint
  @Around("@within(io.fency.IdempotentConsumer)")
  public Object execute(ProceedingJoinPoint pjp) throws Throwable {
    Object proceed = null;
    MessageContext context = contextService.get();

    Optional<Message> message = messageService.find(context.getMessageId(), context.getConsumerQueueName());

    if (message.isPresent()) {
      logError(context);
    } else {
      log.debug(String
          .format("Message with id %s for consumer queue %s not found, processing it",
              context.getMessageId(), context.getConsumerQueueName()));

      proceed = pjp.proceed();

      Message newMessage = new Message(context);
      messageService.save(newMessage);
    }

    return proceed;
  }

  private void logError(MessageContext context) {
    log.error(String
        .format("Discarding duplicate message with id %s for consumer queue %s",
            context.getMessageId(), context.getConsumerQueueName()));
  }
  // CHECKSTYLE:ON
}
