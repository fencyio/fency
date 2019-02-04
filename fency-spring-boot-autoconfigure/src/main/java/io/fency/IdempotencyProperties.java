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

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Gilles Robert
 */
@ConfigurationProperties(prefix = "idempotency")
@Getter
@Setter
public class IdempotencyProperties {

  /**
   * Cron expression used to trigger the cleanup of stored messages metadata.
   * By default, will be triggered every 1st of the month at 5am.
   */
  private String cleanupCron = "0 0 5 1 1-12 *";
}
