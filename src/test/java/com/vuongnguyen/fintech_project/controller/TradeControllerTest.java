package com.vuongnguyen.fintech_project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vuongnguyen.fintech_project.dto.TradeRequest;
import com.vuongnguyen.fintech_project.dto.TradeResponse;
import com.vuongnguyen.fintech_project.enums.TradeSide;
import com.vuongnguyen.fintech_project.exception.GlobalExceptionHandler;
import com.vuongnguyen.fintech_project.exception.InsufficientBalanceException;
import com.vuongnguyen.fintech_project.exception.PriceNotAvailableException;
import com.vuongnguyen.fintech_project.exception.ResourceNotFoundException;
import com.vuongnguyen.fintech_project.exception.TradingException;
import com.vuongnguyen.fintech_project.service.TradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock
    private TradingService tradingService;

    @InjectMocks
    private TradeController tradeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TradeRequest tradeRequest;
    private TradeResponse tradeResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        tradeRequest = new TradeRequest();
        tradeRequest.setUserId(1L);
        tradeRequest.setSymbol("BTCUSDT");
        tradeRequest.setSide(TradeSide.BUY);
        tradeRequest.setQuantity(new BigDecimal("0.5"));
        tradeRequest.setClientOrderId("order-123");

        tradeResponse = new TradeResponse();
        tradeResponse.setTradeId(1L);
        tradeResponse.setUserId(1L);
        tradeResponse.setSymbol("BTCUSDT");
        tradeResponse.setSide(TradeSide.BUY);
        tradeResponse.setPrice(new BigDecimal("50001.00"));
        tradeResponse.setQuantity(new BigDecimal("0.5"));
        tradeResponse.setTotalAmount(new BigDecimal("25000.50"));
        tradeResponse.setCreatedAt(LocalDateTime.now());
        tradeResponse.setClientOrderId("order-123");
        tradeResponse.setUpdatedUsdtBalance(new BigDecimal("74999.50"));
        tradeResponse.setUpdatedAssetBalance(new BigDecimal("0.5"));
        tradeResponse.setAssetCurrency("BTC");
    }

    @Test
    void testExecuteTrade_SuccessfulBuyOrder() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Trade executed successfully"))
                .andExpect(jsonPath("$.data.tradeId").value(1))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.data.side").value("BUY"))
                .andExpect(jsonPath("$.data.quantity").value(0.5))
                .andExpect(jsonPath("$.data.clientOrderId").value("order-123"));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_SuccessfulSellOrder() throws Exception {
        tradeRequest.setSide(TradeSide.SELL);
        tradeResponse.setSide(TradeSide.SELL);
        tradeResponse.setPrice(new BigDecimal("50000.00"));
        tradeResponse.setTotalAmount(new BigDecimal("25000.00"));
        tradeResponse.setUpdatedUsdtBalance(new BigDecimal("125000.00"));
        tradeResponse.setUpdatedAssetBalance(new BigDecimal("0.5"));

        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.side").value("SELL"));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_Idempotency_DuplicateRequest() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId").value(1));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId").value(1));

        verify(tradingService, times(2)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_UserNotFound() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: 1"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_PriceNotAvailable() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class)))
                .thenThrow(new PriceNotAvailableException("No price data available for symbol: BTCUSDT"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_InsufficientBalance() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient USDT balance. Required: 25000.50, Available: 1000.00"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_TradingException() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class)))
                .thenThrow(new TradingException("USDT wallet not found for user: 1"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isInternalServerError());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_RaceCondition_OptimisticLockingFailure() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class)))
                .thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isInternalServerError());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_MissingUserId() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.5"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_MissingSymbol() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.5"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_MissingSide() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setQuantity(new BigDecimal("0.5"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_MissingQuantity() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_InvalidQuantity_TooSmall() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.000000001"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_InvalidQuantity_TooLarge() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("10000000"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_InvalidUserId_Negative() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(-1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.5"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_InvalidUserId_Zero() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(0L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.5"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_ClientOrderIdTooLong() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.5"));
        invalidRequest.setClientOrderId("a".repeat(51));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_ResponseContainsAllFields() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId", notNullValue()))
                .andExpect(jsonPath("$.data.userId", notNullValue()))
                .andExpect(jsonPath("$.data.symbol", notNullValue()))
                .andExpect(jsonPath("$.data.side", notNullValue()))
                .andExpect(jsonPath("$.data.price", notNullValue()))
                .andExpect(jsonPath("$.data.quantity", notNullValue()))
                .andExpect(jsonPath("$.data.totalAmount", notNullValue()))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()))
                .andExpect(jsonPath("$.data.clientOrderId", notNullValue()))
                .andExpect(jsonPath("$.data.updatedUsdtBalance", notNullValue()))
                .andExpect(jsonPath("$.data.updatedAssetBalance", notNullValue()))
                .andExpect(jsonPath("$.data.assetCurrency", notNullValue()));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_ResponseStructure() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_DifferentSymbols() throws Exception {
        TradeRequest ethRequest = new TradeRequest();
        ethRequest.setUserId(1L);
        ethRequest.setSymbol("ETHUSDT");
        ethRequest.setSide(TradeSide.BUY);
        ethRequest.setQuantity(new BigDecimal("1.0"));
        ethRequest.setClientOrderId("order-456");

        TradeResponse ethResponse = new TradeResponse();
        ethResponse.setTradeId(2L);
        ethResponse.setUserId(1L);
        ethResponse.setSymbol("ETHUSDT");
        ethResponse.setSide(TradeSide.BUY);
        ethResponse.setPrice(new BigDecimal("3001.00"));
        ethResponse.setQuantity(new BigDecimal("1.0"));
        ethResponse.setTotalAmount(new BigDecimal("3001.00"));
        ethResponse.setCreatedAt(LocalDateTime.now());
        ethResponse.setClientOrderId("order-456");
        ethResponse.setAssetCurrency("ETH");

        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(ethResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ethRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.symbol").value("ETHUSDT"))
                .andExpect(jsonPath("$.data.assetCurrency").value("ETH"));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_MultipleRequestsWithDifferentClients() throws Exception {
        TradeRequest request1 = new TradeRequest();
        request1.setUserId(1L);
        request1.setSymbol("BTCUSDT");
        request1.setSide(TradeSide.BUY);
        request1.setQuantity(new BigDecimal("0.5"));
        request1.setClientOrderId("order-1");

        TradeRequest request2 = new TradeRequest();
        request2.setUserId(1L);
        request2.setSymbol("BTCUSDT");
        request2.setSide(TradeSide.BUY);
        request2.setQuantity(new BigDecimal("0.3"));
        request2.setClientOrderId("order-2");

        TradeResponse response1 = new TradeResponse();
        response1.setTradeId(1L);
        response1.setClientOrderId("order-1");

        TradeResponse response2 = new TradeResponse();
        response2.setTradeId(2L);
        response2.setClientOrderId("order-2");

        when(tradingService.executeUserTrading(any(TradeRequest.class)))
                .thenReturn(response1)
                .thenReturn(response2);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId").value(1));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId").value(2));

        verify(tradingService, times(2)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_LargeQuantity() throws Exception {
        tradeRequest.setQuantity(new BigDecimal("100.5"));
        tradeResponse.setQuantity(new BigDecimal("100.5"));
        tradeResponse.setTotalAmount(new BigDecimal("5025050.50"));

        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(100.5));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_SmallQuantity() throws Exception {
        tradeRequest.setQuantity(new BigDecimal("0.00000001"));
        tradeResponse.setQuantity(new BigDecimal("0.00000001"));

        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk());

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_WithoutClientOrderId() throws Exception {
        tradeRequest.setClientOrderId(null);
        tradeResponse.setClientOrderId(null);

        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tradeId").value(1));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_ContentTypeValidation() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(tradingService, times(1)).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_VerifyServiceCalledWithCorrectRequest() throws Exception {
        when(tradingService.executeUserTrading(any(TradeRequest.class))).thenReturn(tradeResponse);

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tradeRequest)))
                .andExpect(status().isOk());

        verify(tradingService, times(1)).executeUserTrading(argThat(request ->
                request.getUserId().equals(1L) &&
                request.getSymbol().equals("BTCUSDT") &&
                request.getSide().equals(TradeSide.BUY) &&
                request.getQuantity().equals(new BigDecimal("0.5")) &&
                request.getClientOrderId().equals("order-123")
        ));
    }

    @Test
    void testExecuteTrade_EmptySymbol() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.5"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }

    @Test
    void testExecuteTrade_QuantityWithExcessiveDecimalPlaces() throws Exception {
        TradeRequest invalidRequest = new TradeRequest();
        invalidRequest.setUserId(1L);
        invalidRequest.setSymbol("BTCUSDT");
        invalidRequest.setSide(TradeSide.BUY);
        invalidRequest.setQuantity(new BigDecimal("0.123456789"));

        mockMvc.perform(post("/api/v1/trades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tradingService, never()).executeUserTrading(any(TradeRequest.class));
    }
}
