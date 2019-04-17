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
package io.fency.test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import io.fency.IdempotencyAutoConfiguration;
import io.fency.IdempotencyProperties;
import io.fency.MessageService;
import io.fency.NoOpMessageService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gilles Robert
 */
class IdempotencyAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class, RedisAutoConfiguration.class));

  @Test
  void testMessageService() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MessageService.class));
  }

  @Test
  void testPicksUserRedisConnectionFactory() {
    this.contextRunner.withUserConfiguration(UserConfiguration.class)
        .run((context) -> {
          assertThat(context).hasSingleBean(RedisConnectionFactory.class);
          assertThat(context.getBean(RedisConnectionFactory.class)).isSameAs(
              context.getBean(UserConfiguration.class).redisConnectionFactory());
        });
  }

  @Test
  void testUserProvidedCronCleanup() { // NOPMD
    String cronExpression = "5 * * * * *";
    this.contextRunner.withPropertyValues("idempotency.cleanup-cron=" + cronExpression)
        .run((context) -> {
          IdempotencyProperties properties = context.getBean(IdempotencyProperties.class);
          assertThat(properties.getCleanupCron()).isEqualTo(cronExpression);
        });
  }

  @Test
  void testRedisClassNotOnTheClassPath() {
    this.contextRunner
        .withClassLoader(new FilteredClassLoader(RedisConnectionFactory.class))
        .run((context) -> {
          assertThat(context.getBean(MessageService.class)).isInstanceOf(NoOpMessageService.class);
        });
  }

  @Configuration
  static class UserConfiguration {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
      return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory) {
      RedisTemplate redisTemplate = new RedisTemplate();
      redisTemplate.setConnectionFactory(factory);
      return redisTemplate;
    }
  }
}