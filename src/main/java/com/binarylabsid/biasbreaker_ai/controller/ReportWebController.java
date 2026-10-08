package com.binarylabsid.biasbreaker_ai.controller;

import com.binarylabsid.biasbreaker_ai.dto.StockWebData;
import com.binarylabsid.biasbreaker_ai.service.ReportCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReportWebController {

    @Autowired
    private ReportCacheService cacheService;

    @GetMapping("/report/{symbol}")
    public String showFullReport(@PathVariable("symbol") String symbol, Model model) {
        StockWebData data = cacheService.getReport(symbol);

        if (data == null) {
            model.addAttribute("errorMsg", "Data saham " + symbol.toUpperCase() + " sedang dianalisis atau tidak ditemukan.");
            return "error";
        }

        model.addAttribute("data", data);
        return "report";
    }
}
