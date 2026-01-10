package com.vuongnguyen.fintech_project.service;

import com.vuongnguyen.fintech_project.dto.TradeDetails;
import com.vuongnguyen.fintech_project.dto.TradeHistoryItem;
import com.vuongnguyen.fintech_project.dto.TradeHistoryResponse;
import com.vuongnguyen.fintech_project.dto.TradeRequest;
import com.vuongnguyen.fintech_project.dto.TradeResponse;
import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import com.vuongnguyen.fintech_project.entity.Trade;
import com.vuongnguyen.fintech_project.entity.User;
import com.vuongnguyen.fintech_project.entity.WalletBalance;
import com.vuongnguyen.fintech_project.enums.TradeSide;
import com.vuongnguyen.fintech_project.exception.InsufficientBalanceException;
import com.vuongnguyen.fintech_project.exception.PriceNotAvailableException;
import com.vuongnguyen.fintech_project.exception.ResourceNotFoundException;
import com.vuongnguyen.fintech_project.exception.TradingException;
import com.vuongnguyen.fintech_project.repository.AggregatedPriceRepository;
import com.vuongnguyen.fintech_project.repository.TradeRepository;
import com.vuongnguyen.fintech_project.repository.UserRepository;
import com.vuongnguyen.fintech_project.repository.WalletBalanceRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.vuongnguyen.fintech_project.utility.Constant.BASE_CURRENCY;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingService {

    private final UserRepository userRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final AggregatedPriceRepository aggregatedPriceRepository;
    private final TradeRepository tradeRepository;

    /**
     * Executes a user trading request with optimistic locking and retry mechanism.
     *
     * @param request The {@link TradeRequest} containing user ID, symbol, side, quantity, and optional client order ID
     * @return the {@link TradeResponse} containing the executed trade details and updated wallet balances
     * @throws ResourceNotFoundException if the user or price data is not found
     * @throws PriceNotAvailableException if no current price data exists for the trading symbol
     * @throws InsufficientBalanceException if the user has insufficient funds for the trade
     * @throws TradingException if wallet balances are not found or other trading errors occur
     * @throws OptimisticLockingFailureException if concurrent modification conflicts occur
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retry(name = "executingUserTrading")
    public TradeResponse executeUserTrading(TradeRequest request) {
        log.info("Executing trade: userId={}, symbol={}, side={}, quantity={}, clientOrderId={}",
                request.getUserId(), request.getSymbol(), request.getSide(),
                request.getQuantity(), request.getClientOrderId());

        try {
            if (request.getClientOrderId() != null) {
                Optional<Trade> existingTrade = tradeRepository.findByClientOrderId(request.getClientOrderId());
                if (existingTrade.isPresent()) {
                    log.warn("Duplicate order detected for clientOrderId: {}", request.getClientOrderId());
                    return new TradeResponse().toTradeResponse(existingTrade.get());
                }
            }

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

            AggregatedPrice latestPrice = aggregatedPriceRepository.findLatestBySymbol(request.getSymbol())
                    .orElseThrow(() -> new PriceNotAvailableException("No price data available for symbol: " + request.getSymbol()));

            TradeDetails tradeDetails = new TradeDetails().toTradeDetails(request.getSide(), request.getQuantity(), latestPrice);

            return executeTrade(user, request, tradeDetails);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for trade, will retry: userId={}, symbol={}",
                    request.getUserId(), request.getSymbol());
            throw e;
        } catch (Exception e) {
            log.error("Error executing trade: userId={}, symbol={}, error={}",
                    request.getUserId(), request.getSymbol(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Executes a trade by validating and updating wallet balances, then persisting the trade.
     *
     * @param user the user executing the trade
     * @param request the trade request containing symbol, side, quantity, and client order ID
     * @param tradeDetails the calculated trade details including price and total amount
     * @return the {@link TradeResponse} containing the executed trade and updated wallet balances
     * @throws TradingException if wallet balances are not found for the user
     * @throws InsufficientBalanceException if the user has insufficient funds for the trade
     */
    private TradeResponse executeTrade(User user, TradeRequest request, TradeDetails tradeDetails) {
        String assetCurrency = request.getSymbol().replace(BASE_CURRENCY, "");

        WalletBalance usdtBalance = walletBalanceRepository
                .findByUserIdAndCurrencyWithLock(user.getId(), BASE_CURRENCY)
                .orElseThrow(() -> new TradingException(BASE_CURRENCY + "wallet not found for user: " + user.getId()));

        WalletBalance assetBalance = walletBalanceRepository
                .findByUserIdAndCurrencyWithLock(user.getId(), assetCurrency)
                .orElseThrow(() -> new TradingException(assetCurrency + " wallet not found for user: " + user.getId()));

        if (TradeSide.BUY.equals(request.getSide())) {
            validateAndExecuteBuyOrder(usdtBalance, assetBalance, tradeDetails, request.getQuantity());
        } else {
            validateAndExecuteSellOrder(usdtBalance, assetBalance, tradeDetails, request.getQuantity());
        }

        walletBalanceRepository.saveAll(List.of(usdtBalance, assetBalance));

        Trade trade = new Trade().toEntity(user, request, tradeDetails);
        Trade savedTrade = tradeRepository.save(trade);

        log.info("Trade executed successfully: tradeId={}, userId={}, symbol={}, side={}, price={}, quantity={}, total={}",
                savedTrade.getId(), user.getId(), request.getSymbol(), request.getSide(),
                tradeDetails.getPrice(), request.getQuantity(), tradeDetails.getTotalAmount());

        return new TradeResponse().toTradeResponse(savedTrade, usdtBalance, assetBalance, assetCurrency);
    }

    /**
     * Validates and executes a buy order by checking USDT balance and updating wallet balances.
     *
     * @param usdtBalance the user's USDT wallet balance
     * @param assetBalance the user's asset wallet balance
     * @param tradeDetails the calculated trade details including total amount
     * @param quantity the quantity of asset to buy
     * @throws InsufficientBalanceException if USDT balance is insufficient for the trade
     */
    private void validateAndExecuteBuyOrder(WalletBalance usdtBalance, WalletBalance assetBalance,
                                            TradeDetails tradeDetails, BigDecimal quantity) {
        if (usdtBalance.getBalance().compareTo(tradeDetails.getTotalAmount()) < 0) {
            throw new InsufficientBalanceException(String.format("Insufficient USDT balance. Required: %s, Available: %s",
                    tradeDetails.getTotalAmount(), usdtBalance.getBalance()));
        }

        usdtBalance.setBalance(usdtBalance.getBalance().subtract(tradeDetails.getTotalAmount()));
        assetBalance.setBalance(assetBalance.getBalance().add(quantity));
    }

    /**
     * Validates and executes a sell order by checking asset balance and updating wallet balances.
     *
     * @param usdtBalance the user's USDT wallet balance
     * @param assetBalance the user's asset wallet balance
     * @param tradeDetails the calculated trade details including total amount
     * @param quantity the quantity of asset to sell
     * @throws InsufficientBalanceException if asset balance is insufficient for the trade
     */
    private void validateAndExecuteSellOrder(WalletBalance usdtBalance, WalletBalance assetBalance,
                                             TradeDetails tradeDetails, BigDecimal quantity) {
        if (assetBalance.getBalance().compareTo(quantity) < 0) {
            throw new InsufficientBalanceException(String.format("Insufficient %s balance. Required: %s, Available: %s",
                    assetBalance.getCurrency(), quantity, assetBalance.getBalance()));
        }

        assetBalance.setBalance(assetBalance.getBalance().subtract(quantity));
        usdtBalance.setBalance(usdtBalance.getBalance().add(tradeDetails.getTotalAmount()));
    }

    public TradeHistoryResponse getUserTradeHistory(Long userId, int page, int size, String symbol) {
        log.debug("Fetching trade history for user: {}, page: {}, size: {}, symbol: {}",
                userId, page, size, symbol);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Trade> trades = symbol != null && !symbol.isEmpty() ? tradeRepository.findByUserIdAndSymbol(userId, symbol, pageable)
                : tradeRepository.findByUserId(userId, pageable);

        List<TradeHistoryItem> tradeHistoryItems = trades.stream()
                .map(trade -> new TradeHistoryItem().toTradeHistoryItem(trade))
                .toList();

        return new TradeHistoryResponse(tradeHistoryItems, trades.getNumber(), trades.getTotalPages(),
                trades.getTotalElements(), trades.getSize());
    }
}
