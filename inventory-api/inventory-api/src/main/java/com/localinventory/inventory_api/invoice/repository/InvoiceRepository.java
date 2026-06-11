package com.localinventory.inventory_api.invoice.repository;

import com.localinventory.inventory_api.invoice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByIdAndShopId(Long id, Long shopId);

    long countByShopId(Long shopId);
    boolean existsByInvoiceNumber(String invoiceNumber);

    // Paginated - used in getAll()
    Page<Invoice> findAllByShopId(Long shopId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.shop.id = :shopId")
    Double getTotalRevenueByShop(Long shopId);

    @Query("SELECT i FROM Invoice i WHERE i.shop.id = :shopId " +
            "AND i.createdAt >= :from AND i.createdAt <= :to " +
            "ORDER BY i.createdAt DESC")
    List<Invoice> findByShopIdAndDateRange(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT ii.productName, SUM(ii.quantity), SUM(ii.totalPrice) " +
            "FROM InvoiceItem ii WHERE ii.invoice.shop.id = :shopId " +
            "GROUP BY ii.productName")
    List<Object[]> getProductSalesByShop(Long shopId);
}