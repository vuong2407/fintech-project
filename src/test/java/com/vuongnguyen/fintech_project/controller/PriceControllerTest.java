package com.vuongnguyen.fintech_project.controller;

import com.vuongnguyen.fintech_project.dto.AggregatedPriceResponse;
import com.vuongnguyen.fintech_project.dto.ApiResponse;
import com.vuongnguyen.fintech_project.service.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceControllerTest {

    @Mock
    private PriceService priceService;

    @InjectMocks
    private PriceController priceController;

    private AggregatedPriceResponse mockPriceResponse;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.now();
        mockPriceResponse = new AggregatedPriceResponse();
        mockPriceResponse.setSymbol("BTCUSDT");
        mockPriceResponse.setBestBid(new BigDecimal("50000.00"));
        mockPriceResponse.setBestAsk(new BigDecimal("50001.00"));
        mockPriceResponse.setTimestamp(testTimestamp);
    }

    @Test
    void testGetLatestPrice_Success() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("BTCUSDT", response.getBody().getData().getSymbol());
        verify(priceService, times(1)).getLatestPrice("BTCUSDT");
    }

    @Test
    void testGetLatestPrice_VerifySymbolNormalization() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        priceController.getLatestPrice("btcusdt");

        verify(priceService, times(1)).getLatestPrice("BTCUSDT");
    }

    @Test
    void testGetLatestPrice_VerifySymbolTrimming() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        priceController.getLatestPrice("  btcusdt  ");

        verify(priceService, times(1)).getLatestPrice("BTCUSDT");
    }

    @Test
    void testGetLatestPrice_NotFound() {
        when(priceService.getLatestPrice("UNKNOWN"))
                .thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("unknown");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(priceService, times(1)).getLatestPrice("UNKNOWN");
    }

    @Test
    void testGetLatestPrice_VerifyResponseStructure() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getMessage());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void testGetLatestPrice_VerifyResponseData() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        AggregatedPriceResponse data = response.getBody().getData();
        assertEquals("BTCUSDT", data.getSymbol());
        assertEquals(new BigDecimal("50000.00"), data.getBestBid());
        assertEquals(new BigDecimal("50001.00"), data.getBestAsk());
        assertEquals(testTimestamp, data.getTimestamp());
    }

    @Test
    void testGetLatestPrice_ETHSymbol() {
        AggregatedPriceResponse ethResponse = new AggregatedPriceResponse();
        ethResponse.setSymbol("ETHUSDT");
        ethResponse.setBestBid(new BigDecimal("3000.00"));
        ethResponse.setBestAsk(new BigDecimal("3001.00"));
        ethResponse.setTimestamp(testTimestamp);

        when(priceService.getLatestPrice("ETHUSDT"))
                .thenReturn(Optional.of(ethResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("ethusdt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ETHUSDT", response.getBody().getData().getSymbol());
        assertEquals(new BigDecimal("3000.00"), response.getBody().getData().getBestBid());
    }

    @Test
    void testGetLatestPrice_VerifySuccessMessage() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertTrue(response.getBody().getMessage().contains("successfully"));
    }

    @Test
    void testGetLatestPrice_LargePriceValues() {
        AggregatedPriceResponse largePrice = new AggregatedPriceResponse();
        largePrice.setSymbol("BTCUSDT");
        largePrice.setBestBid(new BigDecimal("99999.99"));
        largePrice.setBestAsk(new BigDecimal("100000.01"));
        largePrice.setTimestamp(testTimestamp);

        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(largePrice));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("99999.99"), response.getBody().getData().getBestBid());
        assertEquals(new BigDecimal("100000.01"), response.getBody().getData().getBestAsk());
    }

    @Test
    void testGetLatestPrice_SmallPriceValues() {
        AggregatedPriceResponse smallPrice = new AggregatedPriceResponse();
        smallPrice.setSymbol("BTCUSDT");
        smallPrice.setBestBid(new BigDecimal("0.01"));
        smallPrice.setBestAsk(new BigDecimal("0.02"));
        smallPrice.setTimestamp(testTimestamp);

        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(smallPrice));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("0.01"), response.getBody().getData().getBestBid());
        assertEquals(new BigDecimal("0.02"), response.getBody().getData().getBestAsk());
    }

    @Test
    void testGetLatestPrice_MultipleSymbols() {
        AggregatedPriceResponse btcResponse = new AggregatedPriceResponse();
        btcResponse.setSymbol("BTCUSDT");
        btcResponse.setBestBid(new BigDecimal("50000.00"));
        btcResponse.setBestAsk(new BigDecimal("50001.00"));
        btcResponse.setTimestamp(testTimestamp);

        AggregatedPriceResponse ethResponse = new AggregatedPriceResponse();
        ethResponse.setSymbol("ETHUSDT");
        ethResponse.setBestBid(new BigDecimal("3000.00"));
        ethResponse.setBestAsk(new BigDecimal("3001.00"));
        ethResponse.setTimestamp(testTimestamp);

        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(btcResponse));
        when(priceService.getLatestPrice("ETHUSDT"))
                .thenReturn(Optional.of(ethResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> btcResult = 
                priceController.getLatestPrice("btcusdt");
        ResponseEntity<ApiResponse<AggregatedPriceResponse>> ethResult = 
                priceController.getLatestPrice("ethusdt");

        assertEquals(HttpStatus.OK, btcResult.getStatusCode());
        assertEquals(HttpStatus.OK, ethResult.getStatusCode());
        assertEquals("BTCUSDT", btcResult.getBody().getData().getSymbol());
        assertEquals("ETHUSDT", ethResult.getBody().getData().getSymbol());
    }

    @Test
    void testGetLatestPrice_VerifyTimestamp() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertNotNull(response.getBody().getData().getTimestamp());
        assertEquals(testTimestamp, response.getBody().getData().getTimestamp());
    }

    @Test
    void testGetLatestPrice_ResponseNotNull() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        assertNotNull(response);
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void testGetLatestPrice_VerifyBidAskNotNull() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        AggregatedPriceResponse data = response.getBody().getData();
        assertNotNull(data.getBestBid());
        assertNotNull(data.getBestAsk());
    }

    @Test
    void testGetLatestPrice_VerifyBidLessThanAsk() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("btcusdt");

        AggregatedPriceResponse data = response.getBody().getData();
        assertTrue(data.getBestBid().compareTo(data.getBestAsk()) < 0);
    }

    @Test
    void testGetLatestPrice_SymbolWithWhitespace() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("  BTCUSDT  ");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(priceService, times(1)).getLatestPrice("BTCUSDT");
    }

    @Test
    void testGetLatestPrice_MixedCaseSymbol() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        ResponseEntity<ApiResponse<AggregatedPriceResponse>> response = 
                priceController.getLatestPrice("BtCuSdT");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(priceService, times(1)).getLatestPrice("BTCUSDT");
    }

    @Test
    void testGetLatestPrice_ServiceCalledOnce() {
        when(priceService.getLatestPrice("BTCUSDT"))
                .thenReturn(Optional.of(mockPriceResponse));

        priceController.getLatestPrice("btcusdt");

        verify(priceService, times(1)).getLatestPrice("BTCUSDT");
    }
}
