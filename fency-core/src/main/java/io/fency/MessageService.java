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

/**
 * @author Gilles Robert
 */
public interface MessageService {

  /**
   * Save a Message metadata in a data store.
   * @param message The message to save.
   */
  void save(Message message);

  /**
   * Retrieve a Message metadata in a data store.
   * @param messageId The message id.
   * @param consumerQueueName The target consumer queue name.
   */
  Optional<Message> find(String messageId, String consumerQueueName);

  /**
   * Clean the message metadata in a data store.
   */
  void clean();
}
