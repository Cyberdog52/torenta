package bt.processor;

import bt.processor.listener.ListenerSource;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ChainProcessorTest {

    @Test
    void cancellationCompletesOnlyAfterProcessingTaskTerminates() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ProcessingStage<ProcessingContext> stage = new ProcessingStage<>() {
            @Override
            public bt.processor.listener.ProcessingEvent after() {
                return null;
            }

            @Override
            public ProcessingStage<ProcessingContext> execute(ProcessingContext context) {
                entered.countDown();
                while (release.getCount() > 0) {
                    try {
                        release.await();
                    } catch (InterruptedException ignored) {
                        // Keep the task alive to verify that cancellation waits for actual termination.
                    }
                }
                return null;
            }
        };
        ChainProcessor<ProcessingContext> processor = new ChainProcessor<>(stage, executor);
        CompletableFuture<?> processing = processor.process(
                mock(ProcessingContext.class), new ListenerSource<>(ProcessingContext.class));

        assertTrue(entered.await(1, TimeUnit.SECONDS));
        assertTrue(processing.cancel(true));
        assertFalse(processing.isDone());

        release.countDown();
        assertThrows(CancellationException.class, () -> processing.get(1, TimeUnit.SECONDS));
        executor.shutdownNow();
    }
}
