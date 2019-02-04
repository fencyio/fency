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

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Gilles Robert
 */
public class IdempotencyBarrierTest {

  @Mock
  private ContextService mContextService;
  @Mock
  private MessageService mMessageService;
  @InjectMocks
  private IdempotencyBarrier aspect;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testExecuteWhenNoMessageFound() throws Throwable {
    // given
    ProceedingJoinPoint mProceedingJoinPoint = Mockito.mock(ProceedingJoinPoint.class);
    MessageContext messageContext = IdempotencyTestUtils.createIdempotentContext();
    given(mContextService.get()).willReturn(messageContext);

    given(mMessageService.find(messageContext.getMessageId(), messageContext.getConsumerQueueName()))
        .willReturn(Optional.empty());

    // when
    aspect.execute(mProceedingJoinPoint);

    // then
    verify(mMessageService).find(messageContext.getMessageId(), messageContext.getConsumerQueueName());
    verify(mProceedingJoinPoint).proceed();
    verify(mMessageService).save(any(Message.class));
  }

  @Test
  public void testExecuteWhenMessageFound() throws Throwable {
    // given
    ProceedingJoinPoint mProceedingJoinPoint = Mockito.mock(ProceedingJoinPoint.class);
    MessageContext messageContext = IdempotencyTestUtils.createIdempotentContext();
    Message message = IdempotencyTestUtils.createIdempotentMessage();
    given(mContextService.get()).willReturn(messageContext);

    given(mMessageService.find(messageContext.getMessageId(), messageContext.getConsumerQueueName()))
        .willReturn(Optional.of(message));

    // when
    aspect.execute(mProceedingJoinPoint);

    // then
    verify(mMessageService).find(messageContext.getMessageId(), messageContext.getConsumerQueueName());
    verify(mProceedingJoinPoint, never()).proceed();
  }
}