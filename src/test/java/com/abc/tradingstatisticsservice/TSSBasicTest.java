package com.abc.tradingstatisticsservice;

import com.abc.tradingstatisticsservice.dto.TradingStats;
import com.abc.tradingstatisticsservice.exception.InsufficientDataException;
import com.abc.tradingstatisticsservice.service.TradingStatsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TSSBasicTest {

    private TradingStatsService tradingStatsService;

    @BeforeEach
    void setUp() {
        tradingStatsService = new TradingStatsService();
    }

    @Test
    void testAddBatchAndCalculateStatsForExactlyK10() throws InsufficientDataException {
        // Adding exactly 10 values for k=10
        Float[] values = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f};
        tradingStatsService.addBatch("AAPL", values);

        // Calculate stats for k=1 (10 items)
        TradingStats stats = tradingStatsService.calculateStats("AAPL", 1);

        assertEquals(5.5, stats.getAvg(), 0.001, "Average for k=10 should be 5.5");
        assertEquals(8.25, stats.getVariance(), 0.001, "Variance for k=10 should be 8.25");
        assertEquals(1.0, stats.getMin(), "Min should be 1.0");
        assertEquals(10.0, stats.getMax(), "Max should be 10.0");
        assertEquals(10.0, stats.getLast(), "Last value should be 10.0");
    }


    @Test
    void testAddBatchAndCalculateStatsForK100() throws InsufficientDataException {
        Float[] values = new Float[100];
        for (int i = 0; i < 100; i++) {
            values[i] = i + 1f;  // Values from 1.0 to 100.0
        }

        tradingStatsService.addBatch("AAPL", values);
        TradingStats stats = tradingStatsService.calculateStats("AAPL", 2);

        assertEquals(50.5, stats.getAvg(), 0.001, "Average should be approximately 50.5");
        assertEquals(833.25, stats.getVariance(), 0.001, "Variance should be approximately 833.25");
        assertEquals(1.0, stats.getMin(), "Min should be 1.0");
        assertEquals(100.0, stats.getMax(), "Max should be 100.0");
        assertEquals(100.0, stats.getLast(), "Last value should be 100.0");
    }

    @Test
    void testAddBatchAndCalculateStatsForK1000() throws InsufficientDataException {
        Float[] values = new Float[1000];
        for (int i = 0; i < 1000; i++) {
            values[i] = i + 1f;  // Values from 1.0 to 1000.0
        }

        tradingStatsService.addBatch("AAPL", values);
        TradingStats stats = tradingStatsService.calculateStats("AAPL", 3);

        assertEquals(500.5, stats.getAvg(), 0.001, "Average should be approximately 500.5");
        assertEquals(83333.25, stats.getVariance(), 0.001, "Variance should be approximately 83333.25");
        assertEquals(1.0, stats.getMin(), "Min should be 1.0");
        assertEquals(1000.0, stats.getMax(), "Max should be 1000.0");
        assertEquals(1000.0, stats.getLast(), "Last value should be 1000.0");
    }


    @Test
    void testCalculateStatsForLargeBatch() throws InsufficientDataException {
        Float[] values = new Float[10000];
        for (int i = 0; i < 10000; i++) {
            values[i] = i + 1f;  // Values from 1.0 to 10000.0
        }

        tradingStatsService.addBatch("AAPL", values);
        TradingStats stats = tradingStatsService.calculateStats("AAPL", 4);

        assertEquals(5000.5, stats.getAvg(), 0.001, "Average should be approximately 5000.5");
        assertEquals(8333333.25, stats.getVariance(), 0.001, "Variance should be approximately 8333333.25");
        assertEquals(1.0, stats.getMin(), "Min should be 1.0");
        assertEquals(10000.0, stats.getMax(), "Max should be 10000.0");
        assertEquals(10000.0, stats.getLast(), "Last value should be 10000.0");
    }

    @Test
    void testAddBatchAndCalculateStatsForK10000() throws InsufficientDataException {
        Float[] values = new Float[10000];
        for (int i = 0; i < 10000; i++) {
            values[i] = i + 1f;  // Values from 1.0 to 100000.0
        }
        long beforeTime = System.currentTimeMillis();
        System.out.println("start: " + beforeTime);

        tradingStatsService.addBatch("AAPL", values);

        TradingStats stats = tradingStatsService.calculateStats("AAPL", 4);

        assertEquals(5000.5, stats.getAvg(), 0.001, "Average should be approximately 5000.5");
        assertEquals(8333333.25, stats.getVariance(), 0.001, "Variance should be approximately 8333333.25");
        assertEquals(1.0, stats.getMin(), "Min should be 1.0");
        assertEquals(10000.0, stats.getMax(), "Max should be 100000.0");
        assertEquals(10000.0, stats.getLast(), "Last value should be 100000.0");
        tradingStatsService.addBatch("AAPL", values);
    }

    @Test
    void testMinAndMaxForMultipleBatches() throws InsufficientDataException {
        // First batch
        Float[] values1 = {100.0f, 200.0f, 300.0f};
        tradingStatsService.addBatch("AAPL", values1);

        // Second batch
        Float[] values2 = {400.0f, 500.0f, 600.0f, 700.0f, 500.0f, 500.0f, 500.0f};
        tradingStatsService.addBatch("AAPL", values2);

        TradingStats stats = tradingStatsService.calculateStats("AAPL", 1);

        assertEquals(100.0, stats.getMin(), "Min should be 100.0");
        assertEquals(700.0, stats.getMax(), "Max should be 700.0");
        assertEquals(500.0, stats.getLast(), "Last value should be 700.0");
    }

    @Test
    public void testCalculateStats10ButNot100() throws InsufficientDataException {
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f});
        tradingStatsService.addBatch("AAPL", new Float[]{100.0f, 101.0f, 102.0f, 103.0f, 104.0f, 105.0f, 106.0f,
                107.0f, 108.0f, 109.0f});

        TradingStats response = tradingStatsService.calculateStats("AAPL", 1);
        assertEquals(100.0, response.getMin());
        assertEquals(109.0, response.getMax());
        assertEquals(109.0, response.getLast());
        assertEquals(104.5, response.getAvg());

        //test 100

        Assertions.assertThrows(InsufficientDataException.class,
                () -> tradingStatsService.calculateStats("AAPL", 2));
    }

    @Test
    public void testStatisticsForMultipleSymbols() throws InsufficientDataException {
        // Batch for Symbol1 (10 repeating values of 10.0)
        Float[] batchSymbol1 = new Float[] {10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f, 10.0f};
        tradingStatsService.addBatch("Symbol1", batchSymbol1);

        // Batch for Symbol2 (10 consecutive values from 1.0 to 10.0)
        Float[] batchSymbol2 = new Float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f};
        tradingStatsService.addBatch("Symbol2", batchSymbol2);

        // Batch for Symbol3 (10 repeating values of 15.5)
        Float[] batchSymbol3 = new Float[] {15.5f, 15.5f, 15.5f, 15.5f, 15.5f, 15.5f, 15.5f, 15.5f, 15.5f, 15.5f};
        tradingStatsService.addBatch("Symbol3", batchSymbol3);

        // Verify stats for Symbol1 (all values are the same, so variance should be zero)
        TradingStats statsSymbol1 = tradingStatsService.calculateStats("Symbol1", 1);
        assertEquals(10.0, statsSymbol1.getAvg(), 0.001);
        assertEquals(0.0, statsSymbol1.getVariance(), 0.001);
        assertEquals(10.0, statsSymbol1.getMin(), 0.001);
        assertEquals(10.0, statsSymbol1.getMax(), 0.001);

        // Verify stats for Symbol2 (sequence from 1 to 10)
        TradingStats statsSymbol2 = tradingStatsService.calculateStats("Symbol2", 1);
        assertEquals(5.5, statsSymbol2.getAvg(), 0.001);
        assertEquals(8.25, statsSymbol2.getVariance(), 0.001);
        assertEquals(1.0, statsSymbol2.getMin(), 0.001);
        assertEquals(10.0, statsSymbol2.getMax(), 0.001);

        // Verify stats for Symbol3 (all values are the same, so variance should be zero)
        TradingStats statsSymbol3 = tradingStatsService.calculateStats("Symbol3", 1);
        assertEquals(15.5, statsSymbol3.getAvg(), 0.001);
        assertEquals(0.0, statsSymbol3.getVariance(), 0.001);
        assertEquals(15.5, statsSymbol3.getMin(), 0.001);
        assertEquals(15.5, statsSymbol3.getMax(), 0.001);
    }
}
