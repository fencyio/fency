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
package io.fency;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;

/**
 * @author Gilles Robert
 */
public class MessageInterceptorTest {

  private static final String MESSAGE_ID = "message id";
  private static final String CONSUMER_QUEUE = "queue";
  private static final Date TIMESTAMP = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));

  @Mock
  private ContextService mContextService;

  private MethodInterceptor interceptor;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    interceptor = new MessageInterceptor(mContextService);
  }

  @Test
  public void testInvoke() throws Throwable {
    // given
    MethodInvocation invocation = createMethodInvocation();

    // when
    interceptor.invoke(invocation);

    // then
    verify(mContextService).clear();

    ArgumentCaptor<MessageContext> idempotentContextArgumentCaptor =
        ArgumentCaptor.forClass(MessageContext.class);
    verify(mContextService).set(idempotentContextArgumentCaptor.capture());

    MessageContext messageContext = idempotentContextArgumentCaptor.getValue();
    assertThat(messageContext, notNullValue());
    assertThat(messageContext.getMessageId(), equalTo(MESSAGE_ID));
    assertThat(messageContext.getConsumerQueueName(), equalTo(CONSUMER_QUEUE));
    assertThat(messageContext.getMessageDate(), equalTo(TIMESTAMP));
  }

  private MethodInvocation createMethodInvocation() {
    return new MethodInvocation() {
      @Override
      public Method getMethod() {
        return null;
      }

      @Override
      public Object[] getArguments() {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setMessageId(MESSAGE_ID);
        messageProperties.setConsumerQueue(CONSUMER_QUEUE);
        messageProperties.setTimestamp(TIMESTAMP);

        return new Object[] {
            null,
            new Message("".getBytes(), messageProperties)
        };
      }

      @Override
      public Object proceed() {
        return null;
      }

      @Override
      public Object getThis() {
        return null;
      }

      @Override
      public AccessibleObject getStaticPart() {
        return null;
      }
    };
  }
}