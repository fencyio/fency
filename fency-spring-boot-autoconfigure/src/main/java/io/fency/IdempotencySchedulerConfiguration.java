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

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import lombok.RequiredArgsConstructor;

/**
 * @author Gilles Robert
 */
@EnableScheduling
@RequiredArgsConstructor
public class IdempotencySchedulerConfiguration implements SchedulingConfigurer {

  private final IdempotencyProperties properties;
  private final MessageService messageService;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    CronTask task = new CronTask(messageService::clean, properties.getCleanupCron());
    taskRegistrar.addCronTask(task);
  }
}
