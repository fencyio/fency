package io.fency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

class NoOpMessageServiceTest {

  @InjectMocks
  private NoOpMessageService messageService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testSave() { // NOPMD: no assert
    // given
    Message message = IdempotencyTestUtils.createIdempotentMessage();

    // when
    messageService.save(message);

    // then nothing
  }

  @Test
  public void testFind() { // NOPMD: no assert
    // given
    Message message = IdempotencyTestUtils.createIdempotentMessage();

    // when
    messageService.find(message.getId(), message.getConsumerQueueName());

    // then nothing
  }

  @Test
  public void testClean() { // NOPMD: no assert
    // given

    // when
    messageService.clean();

    // then nothing
  }
}
