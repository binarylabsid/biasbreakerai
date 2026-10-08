package com.binarylabsid.biasbreaker_ai.service;

import com.binarylabsid.biasbreaker_ai.dto.StockWebData;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReportCacheService {
    private final ConcurrentHashMap<String, StockWebData> cache = new ConcurrentHashMap<>();

    public void saveReport(String symbol, StockWebData data) {
        cache.put(symbol.toUpperCase(), data);
    }

    public StockWebData getReport(String symbol) {
        return cache.get(symbol.toUpperCase());
    }

    public Collection<StockWebData> getAllCachedReports() {
        return cache.values();
    }
}
