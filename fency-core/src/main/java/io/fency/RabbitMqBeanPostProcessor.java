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

import java.lang.reflect.Field;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import lombok.RequiredArgsConstructor;
import org.aopalliance.aop.Advice;

/**
 * Instrument amqp containers with a {@link MessageInterceptor}.
 *
 * @author Gilles Robert
 */
@RequiredArgsConstructor
class RabbitMqBeanPostProcessor implements BeanPostProcessor {

  private final MessageInterceptor messageInterceptor;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    if (bean instanceof SimpleRabbitListenerContainerFactory) {
      SimpleRabbitListenerContainerFactory factory = (SimpleRabbitListenerContainerFactory) bean;
      registerIdempotentInterceptor(factory);
    } else if (bean instanceof AbstractMessageListenerContainer) {
      AbstractMessageListenerContainer container = (AbstractMessageListenerContainer) bean;
      registerIdempotentInterceptor(container);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

    return bean;
  }

  private void registerIdempotentInterceptor(SimpleRabbitListenerContainerFactory factory) {
    Advice[] chain = factory.getAdviceChain();
    Advice[] adviceChainWithTracing = getAdviceChainOrAddInterceptorToChain(chain);
    factory.setAdviceChain(adviceChainWithTracing);
  }

  private void registerIdempotentInterceptor(AbstractMessageListenerContainer container) {
    Field adviceChainField =
        ReflectionUtils.findField(AbstractMessageListenerContainer.class, "adviceChain");
    ReflectionUtils.makeAccessible(adviceChainField);
    Advice[] chain = (Advice[]) ReflectionUtils.getField(adviceChainField, container);
    Advice[] newAdviceChain = getAdviceChainOrAddInterceptorToChain(chain);
    container.setAdviceChain(newAdviceChain);
  }

  private Advice[] getAdviceChainOrAddInterceptorToChain(Advice... existingAdviceChain) {
    if (existingAdviceChain == null) {
      return new Advice[] {messageInterceptor};
    }

    Advice[] newChain = new Advice[existingAdviceChain.length + 1];
    System.arraycopy(existingAdviceChain, 0, newChain, 0, existingAdviceChain.length);
    newChain[existingAdviceChain.length] = messageInterceptor;

    return newChain;
  }
}