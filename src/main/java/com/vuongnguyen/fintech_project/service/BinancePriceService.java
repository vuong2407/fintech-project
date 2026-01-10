package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.BinanceTickerResponse;
import com.vuongnguyen.fintech_project.dto.PriceData;
import com.vuongnguyen.fintech_project.enums.DCESource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.vuongnguyen.fintech_project.utility.Constant.SUPPORTED_SYMBOLS;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinancePriceService {

    private final ExternalService externalService;

    /**
     * Fetches current price data for supported symbols from Binance.
     *
     * @return List of PriceData containing symbol, bid price, ask price, and source information
     */
    public List<PriceData> fetchPrices() {
        log.debug("Fetching prices from Binance");
        List<BinanceTickerResponse> binanceTickerResponses = externalService.fetchBinancePrices(SUPPORTED_SYMBOLS);

        return binanceTickerResponses.stream()
                .map(b -> new PriceData(b.getSymbol(), b.getBidPrice(), b.getAskPrice(), DCESource.BINANCE))
                .toList();
    }
}
