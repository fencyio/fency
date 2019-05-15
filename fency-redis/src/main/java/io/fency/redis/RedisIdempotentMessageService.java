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

import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import io.fency.IdempotentMessageService;
import io.fency.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis implementation of {@link IdempotentMessageService}.
 *
 * @author Gilles Robert
 */
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotentMessageService implements IdempotentMessageService {

  private static final String PREFIX = "message";
  private final RedisTemplate<String, Message> fencyRedisTemplate;

  /**
   * Save a Message metadata in a data store.
   *
   * @param message The message to save.
   */
  @Override
  public void save(Message message) {
    log.debug("Saving metadata for message with id {} and consumer queue name {}", message.getId(),
        message.getConsumerQueueName());
    String key = getKey(message.getId(), message.getConsumerQueueName());
    fencyRedisTemplate.opsForValue().set(key, message);
  }

  /**
   * Retrieve a Message metadata in a data store.
   *
   * @param messageId         The message id.
   * @param consumerQueueName The target consumer queue name.
   */
  @Override
  public Optional<Message> find(String messageId, String consumerQueueName) {
    log.debug("Retrieving metadata for message with id {} and consumer queue name {}", messageId, consumerQueueName);
    String key = getKey(messageId, consumerQueueName);
    return Optional.ofNullable(fencyRedisTemplate.opsForValue().get(key));
  }

  /**
   * Clean the message metadata in a data store.
   */
  @Override
  public void clean() {
    log.debug("Cleaning up messages metadata stored in Redis...");
    Set<String> keys = fencyRedisTemplate.keys(PREFIX + "*");
    fencyRedisTemplate.delete(keys);
    log.debug("{} messages deleted.", keys.size());
  }

  private String getKey(String messageId, String consumerQueueName) {
    return String.join(":", PREFIX, messageId, consumerQueueName);
  }
}
