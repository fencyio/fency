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
  public void testMessageService() {
    this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MessageService.class));
  }

  @Test
  public void testPicksUserRedisConnectionFactory() {
    this.contextRunner.withUserConfiguration(UserConfiguration.class)
        .run((context) -> {
          assertThat(context).hasSingleBean(RedisConnectionFactory.class);
          assertThat(context.getBean(RedisConnectionFactory.class)).isSameAs(
              context.getBean(UserConfiguration.class).redisConnectionFactory());
        });
  }

  @Test
  public void testUserProvidedCronCleanup() { // NOPMD
    String cronExpression = "5 * * * * *";
    this.contextRunner.withPropertyValues("idempotency.cleanup-cron=" + cronExpression)
        .run((context) -> {
          IdempotencyProperties properties = context.getBean(IdempotencyProperties.class);
          assertThat(properties.getCleanupCron()).isEqualTo(cronExpression);
        });
  }

  @Test
  public void testRedisClassNotOnTheClassPath() {
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