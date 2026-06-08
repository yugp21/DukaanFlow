package com.localinventory.inventory_api.invoice.repository;

import com.localinventory.inventory_api.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByShopIdOrderByCreatedAtDesc(Long shopId);
    Optional<Invoice> findByIdAndShopId(Long id, Long shopId);
    long countByShopId(Long shopId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.shop.id = :shopId")
    Double getTotalRevenueByShop(Long shopId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.shop.id = :shopId AND MONTH(i.createdAt) = MONTH(CURRENT_DATE) AND YEAR(i.createdAt) = YEAR(CURRENT_DATE)")
    Double getMonthlyRevenueByShop(Long shopId);

    @Query("SELECT ii.productName, SUM(ii.quantity), SUM(ii.totalPrice) " +
            "FROM InvoiceItem ii WHERE ii.invoice.shop.id = :shopId " +
            "GROUP BY ii.productName")
    List<Object[]> getProductSalesByShop(Long shopId);

}