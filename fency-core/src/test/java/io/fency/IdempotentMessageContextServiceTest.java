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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Gilles Robert
 */
@ExtendWith(MockitoExtension.class)
class IdempotentMessageContextServiceTest {

  @InjectMocks
  private IdempotentMessageContextService idempotentMessageContextService;

  @AfterEach
  void tearDown() {
    idempotentMessageContextService.clear();
  }

  @Test
  void testGetWhenContextIsNotInitialized() {
    // given no initialization

    // when
    MessageContext context = idempotentMessageContextService.get();

    // then
    assertThat(context, nullValue());
  }

  @Test
  void testSet() {
    // given no initialization
    MessageContext expectedContext = IdempotencyTestUtils.createIdempotentContext();

    // when
    idempotentMessageContextService.set(expectedContext);

    // then
    MessageContext actualContext = idempotentMessageContextService.get();
    assertThat(actualContext, sameInstance(expectedContext));
  }

  @Test
  void testClear() {
    // given

    // when
    idempotentMessageContextService.clear();

    // then
    MessageContext context = idempotentMessageContextService.get();
    assertThat(context, nullValue());
  }

  @Test
  void testSetWhenNotEmpty() {
    // given no initialization
    MessageContext expectedContext = IdempotencyTestUtils.createIdempotentContext();
    idempotentMessageContextService.set(expectedContext);

    // when / then
    assertThrows(IllegalArgumentException.class, () -> idempotentMessageContextService.set(expectedContext));
  }
}