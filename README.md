[![Build Status][ci-img]][ci]
[![codecov](https://codecov.io/gh/ask4gilles/fency/branch/master/graph/badge.svg)](https://codecov.io/gh/ask4gilles/fency)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ee34d1388f549e1ad3298a967f388f0)](https://www.codacy.com/app/ask4gilles/fency?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ask4gilles/fency&amp;utm_campaign=Badge_Grade)
[![Released Version][maven-img]][maven]
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
# Fency: an idempotency barrier for RabbitMQ consumers
## Theoretical concept
Even when a sender application sends a message only once,
the receiver application may receive the message more than once.

The term idempotent is used in mathematics to describe a function that produces the same result 
if it is applied to itself: f(x) = f(f(x)). 
In Messaging this concepts translates into a message that has the same effect whether it is received 
once or multiple times. 
This means that a message can safely be resent without causing any problems even if the receiver receives 
duplicates of the same message.

The recipient can explicitly de-dupe messages by keeping track of messages that it already received. 
A unique message identifier simplifies this task and helps detect those cases where 
two legitimate messages with the same message content arrive.

In order to detect and eliminate duplicate messages based on the message identifier, 
the message recipient has to keep a list of already received message identifiers.

## Technical implementation

In order to store the processed message metadata, we have to be in a transactional context.
If something goes wrong, the transaction has to be roll backed.

1.  The **MessageInterceptor** creates an **MessageContext** and stores it in a ThreadLocal

2.  The **IdempotencyBarrier** is an aspect around the **@IdempotentConsumer** annotation. 
It retrieves the MessageContext and checks if the message already exists. 
The unique message key is composed by the messageId and the consumerQueueName.

If the message does not exist, the target method is invoked and the message metadata is stored in a datastore.

If the message already exists, an error message is logged and the target method is not invoked.

## Usage

* Maven
```xml
<dependency>
  <groupId>io.fency</groupId>
  <artifactId>fency-spring-boot-starter-redis</artifactId>
  <version>0.1.0</version>
</dependency>
```

* Gradle
```text
implementation 'io.fency:fency-spring-boot-starter-redis:+'
```

Annotate your consumers with @IdempotentConsumer.

See sample app: [fency-spring-boot-sample-app](https://github.com/ask4gilles/fency/tree/master/fency-spring-boot-sample-app)

## Note for spring integration users
If you are already using spring integration, have a look 
[here](https://docs.spring.io/spring-integration/docs/current/reference/html/#idempotent-receiver)

## References
* [Idempotent receiver](https://www.enterpriseintegrationpatterns.com/patterns/messaging/IdempotentReceiver.html)

[ci-img]: https://api.travis-ci.com/ask4gilles/fency.svg?branch=master
[ci]: https://travis-ci.com/ask4gilles/fency
[maven-img]: https://img.shields.io/maven-central/v/io.fency/fency-core.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Cio.fency
