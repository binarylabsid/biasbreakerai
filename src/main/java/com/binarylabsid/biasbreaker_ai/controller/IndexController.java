package com.binarylabsid.biasbreaker_ai.controller;

import com.binarylabsid.biasbreaker_ai.dto.StockWebData;
import com.binarylabsid.biasbreaker_ai.service.ReportCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import java.util.Collection;

@Controller
public class IndexController {

    @Autowired
    private ReportCacheService cacheService;

    @GetMapping("/")
    public String dashboard(Model model) {
        Collection<StockWebData> activeStocks = cacheService.getAllCachedReports();

        model.addAttribute("stocks", activeStocks);
        return "index";
    }
}