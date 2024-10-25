package com.abc.tradingstatisticsservice;

import com.abc.tradingstatisticsservice.exception.InsufficientDataException;
import java.util.stream.IntStream;


import com.abc.tradingstatisticsservice.service.TradingStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class TSSCornerCasesTest {

    private TradingStatsService tradingStatsService;

    @BeforeEach
    public void setUp() {
        tradingStatsService = new TradingStatsService();
    }

    @Test
    public void testAddBatchWithLessThanRequiredDataShouldThrowException() {
        Float[] insufficientData = {1.0f, 2.0f}; // Less than the minimum 10 required
        assertThrows(InsufficientDataException.class, () -> {
            tradingStatsService.addBatch("AAPL", insufficientData);
            tradingStatsService.calculateStats("AAPL", 1);
        });
    }

    @Test
    public void testCalculateStatsWithInsufficientDataForLargerKShouldThrowException() {
        Float[] batch = IntStream.rangeClosed(1, 50).mapToObj(i -> 1.0f).toArray(Float[]::new);


        // Since 50 < 100, this should throw an InsufficientDataException
        assertThrows(InsufficientDataException.class, () -> {tradingStatsService.addBatch("AAPL", batch);tradingStatsService.calculateStats("AAPL", 2);});
    }

    @Test
    public void testAddBatchExceedingMaxSizeShouldThrowException() {
        Float[] excessiveBatch = new Float[10001]; // Exceeds the maximum allowed size of 10,000
        for (int i = 0; i < excessiveBatch.length; i++) {
            excessiveBatch[i] = 1.0f;
        }

        assertThrows(InsufficientDataException.class, () -> tradingStatsService.addBatch("AAPL", excessiveBatch));
    }


    @Test
    public void testAddBatchForNonExistentSymbolThrowsException() throws InsufficientDataException {
        Float[] batch = IntStream.rangeClosed(1, 10).mapToObj(i -> 5.0f).toArray(Float[]::new);
        tradingStatsService.addBatch("AAPL", batch);

        assertThrows(InsufficientDataException.class, () -> tradingStatsService.calculateStats("GOOG", 1));
    }

    @Test
    public void testAddingMoreThanTenUniqueSymbolsThrowsException() throws InsufficientDataException {
        Float[] batch = IntStream.rangeClosed(1, 10).mapToObj(i -> 5.0f).toArray(Float[]::new);

        for (int i = 1; i <= 10; i++) {
            tradingStatsService.addBatch("SYMBOL" + i, batch);
        }

        assertThrows(InsufficientDataException.class, () -> tradingStatsService.addBatch("EXTRA_SYMBOL", batch));
    }
}
