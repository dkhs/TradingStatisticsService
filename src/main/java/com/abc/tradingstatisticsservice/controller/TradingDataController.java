package com.abc.tradingstatisticsservice.controller;

import com.abc.tradingstatisticsservice.service.TradingStatsService;
import com.abc.tradingstatisticsservice.dto.TradingDataBatch;
import com.abc.tradingstatisticsservice.dto.TradingStats;
import com.abc.tradingstatisticsservice.exception.InsufficientDataException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradingDataController {

    private final TradingStatsService tradingStatsService;

    public TradingDataController(TradingStatsService tradingStatsService) {
        this.tradingStatsService = tradingStatsService;
    }

    @PostMapping("/add_batch/")
    public ResponseEntity<String> addBatch(@RequestBody TradingDataBatch request) {
        try {
            tradingStatsService.addBatch(request.getSymbol(), request.getValues());
            return new ResponseEntity<>("Batch added successfully.", HttpStatus.OK);
        } catch (InsufficientDataException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stats/")
    public ResponseEntity<?> getStats(@RequestParam String symbol, @RequestParam int k) {
        try {
            TradingStats stats = tradingStatsService.calculateStats(symbol, k);
            return new ResponseEntity<>(stats, HttpStatus.OK);
        } catch (InsufficientDataException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleExceptions(Exception ex) {
        return new ResponseEntity<>("An error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
