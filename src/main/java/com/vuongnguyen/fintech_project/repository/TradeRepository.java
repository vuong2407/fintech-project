package com.vuongnguyen.fintech_project.repository;

import com.vuongnguyen.fintech_project.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Trade> findByUserIdAndSymbolOrderByCreatedAtDesc(Long userId, String symbol);

    @Query("SELECT t FROM Trade t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Trade> findTradeHistoryByUserId(@Param("userId") Long userId);

    Optional<Trade> findByClientOrderId(String clientOrderId);
}
