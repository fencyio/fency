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

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import io.fency.FencyAutoConfiguration;
import io.fency.FencyProperties;
import io.fency.IdempotentMessageService;
import io.fency.redis.FencyRedisConfiguration;
import io.fency.redis.RedisIdempotentMessageService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gilles Robert
 */
class FencyAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations
          .of(FencyAutoConfiguration.class, FencyRedisConfiguration.class,
              RedisAutoConfiguration.class));

  @Test
  void testIdempotentMessageService() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(IdempotentMessageService.class);

      assertThat(context).hasSingleBean(IdempotentMessageService.class);
      IdempotentMessageService idempotentMessageService = (context).getBean(IdempotentMessageService.class);
      Object targetObject = getTargetObject(idempotentMessageService);
      assertThat(targetObject)
          .isInstanceOf(RedisIdempotentMessageService.class);
    });
  }

  @Test
  void testPicksUserRedisConnectionFactory() {
    this.contextRunner
        .withUserConfiguration(UserConfiguration.class)
        .run((context) -> {
          assertThat(context).hasSingleBean(RedisConnectionFactory.class);
          assertThat(context.getBean(RedisConnectionFactory.class)).isSameAs(
              context.getBean(UserConfiguration.class).redisConnectionFactory());
        });
  }

  @Test
  void testUserProvidedCronCleanup() { // NOPMD
    String cronExpression = "5 * * * * *";
    this.contextRunner.withPropertyValues("fency.cleanup-cron=" + cronExpression)
        .run((context) -> {
          FencyProperties properties = context.getBean(FencyProperties.class);
          assertThat(properties.getCleanupCron()).isEqualTo(cronExpression);
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

  @SuppressWarnings("unchecked")
  private <T> T getTargetObject(Object candidate) {
    try {
      if (AopUtils.isAopProxy(candidate) && (candidate instanceof Advised)) {
        return (T) ((Advised) candidate).getTargetSource().getTarget();
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to unwrap proxied object", ex);
    }
    return (T) candidate;
  }
}