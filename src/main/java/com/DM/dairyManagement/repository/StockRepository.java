package com.DM.dairyManagement.repository;

import com.DM.dairyManagement.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Transactional
public interface StockRepository extends JpaRepository<Stock, Long> {

    void deleteByProduct_Id(Long productId);

    // ✅ Modified to handle multiple results
    @Query("SELECT s FROM Stock s JOIN s.product p WHERE LOWER(TRIM(p.name)) = LOWER(TRIM(:name))")
    List<Stock> findAllStocksByProductNameIgnoreCase(@Param("name") String name);
    
    // ✅ Keep the original method but use findFirst to get only one result
    default Optional<Stock> findStockByProductNameIgnoreCase(@Param("name") String name) {
        List<Stock> stocks = findAllStocksByProductNameIgnoreCase(name);
        return stocks.isEmpty() ? Optional.empty() : Optional.of(stocks.get(0));
    }
}
