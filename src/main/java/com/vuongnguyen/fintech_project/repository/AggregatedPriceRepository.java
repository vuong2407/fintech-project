package com.vuongnguyen.fintech_project.repository;

import com.vuongnguyen.fintech_project.entity.AggregatedPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedPriceRepository extends JpaRepository<AggregatedPrice, Long> {

    @Query("SELECT ap FROM AggregatedPrice ap WHERE ap.symbol = :symbol ORDER BY ap.timestamp DESC LIMIT 1")
    Optional<AggregatedPrice> findLatestBySymbol(@Param("symbol") String symbol);

    @Query("SELECT ap FROM AggregatedPrice ap WHERE ap.symbol IN :symbols ORDER BY ap.timestamp DESC LIMIT 1")
    List<AggregatedPrice> findLatestBySymbolIn(List<String> symbols);

    Optional<AggregatedPrice> findTopBySymbolOrderByTimestampDesc(String symbol);
}
