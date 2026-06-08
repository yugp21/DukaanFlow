package com.localinventory.inventory_api.customer.repository;

import com.localinventory.inventory_api.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByShopId(Long shopId);

    Optional<Customer> findByIdAndShopId(Long id, Long shopId);

    long countByShopId(Long shopId);

    @Query("SELECT c FROM Customer c WHERE c.shop.id = :shopId AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.phone LIKE CONCAT('%', :search, '%'))")
    List<Customer> searchByShop(Long shopId, String search);
}