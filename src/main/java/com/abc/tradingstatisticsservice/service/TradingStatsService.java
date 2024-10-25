package com.abc.tradingstatisticsservice.service;

import com.abc.tradingstatisticsservice.dto.TradingStats;
import com.abc.tradingstatisticsservice.exception.InsufficientDataException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

@Service
public class TradingStatsService {
    private static final int[] K_SIZES = {10, 100, 1000, 10_000, 100_000, 1000_000, 10_000_000, 100_000_000};
    private static final int[] K_SIZES_REAL = {10, 90, 900, 9000, 90_000, 900_000, 90_00000, 90_000_000};
    private static final float MAX_BATCH_SIZE= 10_000;
    private static final int MAX_SYMBOLS = 10;
    public static final int BIG_D_SCALE = 5;

    private static final Logger logger = LoggerFactory.getLogger(TradingStatsService.class);

    // Maps to store the rolling sums, squared sums, and counts for each symbol and each k
    private final Map<String, BigDecimal[]> sumForKPrices = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal[]> squaredSumForKPrices = new ConcurrentHashMap<>();
    private final Map<String, Integer[]> countForKPrices = new ConcurrentHashMap<>();
    private final Map<String, Deque<Float>[]> pricesDequeues = new ConcurrentHashMap<>();

    // TreeMap to store prices just for max and min (separated for holding only subsets)
    private final Map<String, NavigableMap<Float, Integer>[]> priceMaxMinTreeMaps = new ConcurrentHashMap<>();
    
    public void addBatch(String symbol, Float[] values) throws InsufficientDataException {
        if (!pricesDequeues.containsKey(symbol) && pricesDequeues.size() >= MAX_SYMBOLS) {
            logger.warn("Batch rejected: Unique symbols size exceeded. Symbol: {}", symbol);
            throw new InsufficientDataException("Unique symbols size exceeded. Batch rejected");
        }

        if (values.length > MAX_BATCH_SIZE) {
            logger.warn("Batch rejected: Batch size {} exceeds MAX_BATCH_SIZE. Symbol: {}", values.length, symbol);
            throw new InsufficientDataException("Singe Batch Size cannot exceed " + MAX_BATCH_SIZE);
        }

        initializeCollections(symbol);

        Deque<Float>[] priceDequeues = pricesDequeues.get(symbol);
        BigDecimal[] sumForKPrices = this.sumForKPrices.get(symbol);
        BigDecimal[] squaredSumForKPrices = this.squaredSumForKPrices.get(symbol);
        Integer[] countForKPrices = this.countForKPrices.get(symbol);
        NavigableMap<Float, Integer>[] priceMaxMinTreeMap = priceMaxMinTreeMaps.get(symbol);

        for (Float value : values) {
            accumulateInNextDeque(0, value, priceDequeues, sumForKPrices, squaredSumForKPrices, priceMaxMinTreeMap, countForKPrices);
        }

        logger.info("Batch added successfully for symbol: {} with {} values", symbol, values.length);
    }
    
    private void accumulateInNextDeque(int dequeKIndex, Float currentValue, Deque<Float>[] dequeues,
                                       BigDecimal[] sumForKPrices, BigDecimal[] squaredSumForKPrices,
                                       NavigableMap<Float, Integer>[] pricesMinMaxMap, Integer[] countForKPrices) {

        Deque<Float> deque = dequeues[dequeKIndex];
        BigDecimal bgValue = BigDecimal.valueOf(currentValue);

        deque.addLast(currentValue.floatValue());
        countForKPrices[dequeKIndex]++;

        sumForKPrices[dequeKIndex] = sumForKPrices[dequeKIndex].add(bgValue);
        squaredSumForKPrices[dequeKIndex] = squaredSumForKPrices[dequeKIndex].add(bgValue.pow(2));
        pricesMinMaxMap[dequeKIndex].merge(currentValue.floatValue(), 1, Integer::sum);

        // Handle overflow if deque size exceeds the range size for this k
        if (deque.size() > K_SIZES_REAL[dequeKIndex]) {
            Float oldestValue = deque.removeFirst();
            BigDecimal bgOldestValue = BigDecimal.valueOf(oldestValue);

            countForKPrices[dequeKIndex]--;
            sumForKPrices[dequeKIndex] = sumForKPrices[dequeKIndex].subtract(bgOldestValue);
            squaredSumForKPrices[dequeKIndex] = squaredSumForKPrices[dequeKIndex].subtract(bgOldestValue.pow(2));

            pricesMinMaxMap[dequeKIndex].compute(oldestValue.floatValue(), (key, count) -> (count == 1) ? null : count - 1);

            // Accumulate for higher dequeue (roll and recalculate)
            if (dequeKIndex + 1 < K_SIZES_REAL.length) {
                accumulateInNextDeque(dequeKIndex + 1, oldestValue, dequeues, sumForKPrices, squaredSumForKPrices, pricesMinMaxMap, countForKPrices);
            }
        }
    }

    public TradingStats calculateStats(String symbol, int kLevel) throws InsufficientDataException{
        if (!pricesDequeues.containsKey(symbol)) {
            logger.warn("Stats calculation failed: Symbol '{}' not found", symbol);
            throw new InsufficientDataException("Symbol not found");
        }

        int kIndex = kLevel - 1;
        if (kIndex < 0 || kIndex >= K_SIZES.length) {
            logger.warn("Stats calculation failed: Invalid kLevel '{}'", kLevel);
            throw new InsufficientDataException("Invalid kLevel: " + kLevel);
        }
        
        Deque<Float>[] dequeues = pricesDequeues.get(symbol);
        int itemCount = Arrays.stream(dequeues).mapToInt(Deque::size).sum();

        if (itemCount == 0 || itemCount < K_SIZES[kIndex]) {
            logger.warn("Stats calculation failed: Insufficient data for k={} with symbol '{}'", kLevel, symbol);
            throw new InsufficientDataException("Insufficient data for k=" + kLevel + "; at least "+K_SIZES[kLevel]+" data needed");
        }

        BigDecimal[] sumOfKPrices = sumForKPrices.get(symbol);
        BigDecimal[] squaredSumOfKPrices = squaredSumForKPrices.get(symbol);
        NavigableMap<Float, Integer>[] priceKMaxMinMap = priceMaxMinTreeMaps.get(symbol);


        Double min = mergeMin(priceKMaxMinMap);

        Double max = mergedMax(priceKMaxMinMap);

        BigDecimal totalKSum = getSumOfAllAvailable(sumOfKPrices, kIndex);
        BigDecimal totalKSquaredSum = getSumOfAllAvailable(squaredSumOfKPrices, kIndex);
        BigDecimal kSizeBD = BigDecimal.valueOf(K_SIZES[kIndex]);

        BigDecimal avg = totalKSum.divide(kSizeBD, BIG_D_SCALE, BigDecimal.ROUND_HALF_UP);
        BigDecimal variance = totalKSquaredSum.subtract(totalKSum.multiply(totalKSum).divide(kSizeBD, BIG_D_SCALE, BigDecimal.ROUND_HALF_UP))
                .divide(kSizeBD, BIG_D_SCALE, BigDecimal.ROUND_HALF_UP);

        Double last = fetchMostRecentPrice(dequeues);

        TradingStats stats = new TradingStats();
        stats.setMin(min);
        stats.setMax(max);
        stats.setAvg(avg.doubleValue());
        stats.setVariance(variance.doubleValue());
        stats.setLast(last);

        logger.info("Stats calculated for symbol '{}' at kLevel {}: Min={}, Max={}, Avg={}, Variance={}, Last={}",
                symbol, kLevel, min, max, avg, variance, last);

        return stats;
    }

    private static Double fetchMostRecentPrice(Deque<Float>[] dequeues) {
        return format(dequeues[0].peekLast());
    }

    private static double mergeMin(NavigableMap<Float, Integer>[] treeMaps) {
        return Arrays.stream(treeMaps)
                .filter(map -> !map.isEmpty())
                .mapToDouble(p -> format(p.firstKey()))
                .min()
                .orElse(Double.MAX_VALUE);
    }

    private static double mergedMax(NavigableMap<Float, Integer>[] treeMaps) {
        return Arrays.stream(treeMaps)
                .filter(map -> !map.isEmpty())
                .mapToDouble(p -> format(p.lastKey()))
                .max()
                .orElse(Double.MIN_VALUE);
    }

    private static Double format(Float f){
        return Double.parseDouble(String.valueOf(f));
    }

    private void initializeCollections(String symbol) {
        // Initialize data structures if symbol is seen for the first time
        pricesDequeues.computeIfAbsent(symbol, k -> initializeDataStructures(symbol));
        sumForKPrices.computeIfAbsent(symbol, k -> initializeSumArrays());
        squaredSumForKPrices.computeIfAbsent(symbol, k -> initializeSumArrays());
        countForKPrices.computeIfAbsent(symbol, k -> initializeCountArrays());
        priceMaxMinTreeMaps.computeIfAbsent(symbol, k -> initializeTreeMapArrays());
    }

    private BigDecimal[] initializeSumArrays() {
        return Arrays.stream(K_SIZES).mapToObj(i -> BigDecimal.ZERO).toArray(BigDecimal[]::new);
    }

    private Integer[] initializeCountArrays() {
        return Arrays.stream(K_SIZES).mapToObj(i -> 0).toArray(Integer[]::new);
    }

    private TreeMap<Float, Integer>[] initializeTreeMapArrays() {
        return Arrays.stream(K_SIZES).mapToObj(i -> new TreeMap<Float, Integer>()).toArray(TreeMap[]::new);
    }

    private Deque<Float>[] initializeDataStructures(String symbol) {
        return Arrays.stream(K_SIZES).mapToObj(i -> new LinkedList<Float>()).toArray(Deque[]::new);
    }

    private BigDecimal getSumOfAllAvailable(BigDecimal[] arrayToSum, int kIndex) {
        return Arrays.stream(arrayToSum, 0, kIndex + 1)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
