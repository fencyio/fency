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

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Creates and stores a new {@link MessageContext} before method invocation with message properties.
 *
 * @author Gilles Robert
 */
@RequiredArgsConstructor
class MessageInterceptor implements MethodInterceptor {

  private final IdempotentMessageContextService idempotentMessageContextService;

  // CHECKSTYLE:OFF throwable implied by MethodInterceptor
  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Message message = (Message) invocation.getArguments()[1];
    MessageProperties messageProperties = message.getMessageProperties();

    MessageContext messageContext = new MessageContext(messageProperties);

    idempotentMessageContextService.clear();
    idempotentMessageContextService.set(messageContext);

    return invocation.proceed();
  }
  // CHECKSTYLE:ON
}
