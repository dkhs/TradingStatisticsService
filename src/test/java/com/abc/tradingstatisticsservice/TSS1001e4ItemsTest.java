package com.abc.tradingstatisticsservice;

import com.abc.tradingstatisticsservice.dto.TradingStats;
import com.abc.tradingstatisticsservice.exception.InsufficientDataException;

import com.abc.tradingstatisticsservice.service.TradingStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TSS1001e4ItemsTest {

    private final TradingStatsService tradingStatsService = new TradingStatsService();

    @Test
    public void testAddBatchAndCalculateStats_WithLargeSequentialBatch() throws InsufficientDataException {
        String symbol = "TEST_SYMBOL";
        int totalItems = 100010000; // 100 01e4 items
        int batchSize = 10000; // Batch size of 10,000
        Float[] batch = new Float[batchSize];


        Float lastFloat = null;
        // Generate batch of floats from 1.0 to 10,000.0
        for (int i = 0; i < batchSize; i++) {
            batch[i] = Float.valueOf( i + 1);
            lastFloat = batch[i];
        }

        long beforeTime = System.currentTimeMillis();
        System.out.println("start all batch: "+beforeTime);
        // Add data sequentially in batches of 10,000
        // Add the sequential batches to the service
        int numBatches = totalItems / batchSize;
        for (int i = 0; i < numBatches; i++) {
            long beforebTime = System.currentTimeMillis();
            tradingStatsService.addBatch(symbol, batch);
            System.out.println("single Batch time: "+System.currentTimeMillis()+"; duration ms: "+(System.currentTimeMillis() - beforebTime+"; last float: "+lastFloat));
        }
        System.out.println("end ALL BATCH CHA CHA: "+System.currentTimeMillis()+"; duration ms: "+(System.currentTimeMillis() - beforeTime));

        // Now let's calculate the stats for the k=6 level (which should use 1,000,000 values)
        TradingStats stats = tradingStatsService.calculateStats(symbol, 6);

        // Expected results for this particular test
        double expectedAvg = 5000.5f; // Average of 1 to 10,000 is 5000.5
        double expectedVariance = 8333333.25; // Variance of 1 to 10,000 (precomputed)

        // Assert that stats are as expected
        assertEquals(expectedAvg, stats.getAvg(), 0.001, "Average should be approximately 5000.5");
        assertEquals(expectedVariance, stats.getVariance(), 0.001, "Variance should be approximately 8333333.25");
        assertEquals(1.0, stats.getMin(), "Min should be 1.0");
        assertEquals(10000.0, stats.getMax(), "Max should be 10000.0");
    }
}
