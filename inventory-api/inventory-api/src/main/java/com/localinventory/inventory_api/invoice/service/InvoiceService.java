package com.localinventory.inventory_api.invoice.service;

import com.localinventory.inventory_api.customer.entity.Customer;
import com.localinventory.inventory_api.customer.repository.CustomerRepository;
import com.localinventory.inventory_api.exception.InsufficientStockException;
import com.localinventory.inventory_api.exception.ResourceNotFoundException;
import com.localinventory.inventory_api.invoice.dto.*;
import com.localinventory.inventory_api.invoice.entity.Invoice;
import com.localinventory.inventory_api.invoice.entity.InvoiceItem;
import com.localinventory.inventory_api.invoice.repository.InvoiceRepository;
import com.localinventory.inventory_api.product.entity.Product;
import com.localinventory.inventory_api.product.repository.ProductRepository;
import com.localinventory.inventory_api.security.ShopContext;
import com.localinventory.inventory_api.shop.entity.Shop;
import com.localinventory.inventory_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ShopContext shopContext;

    public InvoiceResponse createInvoice(InvoiceRequest request) {
        User currentUser = shopContext.getCurrentUser();
        Shop shop = currentUser.getShop();
        Long shopId = shop.getId();

        Customer customer = customerRepository.findByIdAndShopId(request.getCustomerId(), shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Validate stock
        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdAndShopId(itemReq.getProductId(), shopId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));
            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for: " + product.getName() +
                                ". Available: " + product.getStockQuantity());
            }
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber(shopId))
                .customer(customer)
                .createdBy(currentUser)
                .shop(shop)
                .discount(request.getDiscount())
                .taxPercent(request.getTaxPercent())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("PAID")
                .notes(request.getNotes())
                .build();

        List<InvoiceItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (InvoiceItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdAndShopId(itemReq.getProductId(), shopId).get();
            BigDecimal itemTotal = product.getSellingPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemTotal);

            items.add(InvoiceItem.builder()
                    .invoice(invoice)
                    .product(product)
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getSellingPrice())
                    .totalPrice(itemTotal)
                    .build());

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepository.save(product);
        }

        BigDecimal discountAmount = subtotal.multiply(
                request.getDiscount().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal afterDiscount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = afterDiscount.multiply(
                request.getTaxPercent().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal total = afterDiscount.add(taxAmount);

        invoice.setSubtotal(subtotal);
        invoice.setTotalAmount(total);
        invoice.setItems(items);

        customer.setTotalPurchases(customer.getTotalPurchases().add(total));
        customerRepository.save(customer);

        return mapToResponse(invoiceRepository.save(invoice));
    }

    public List<InvoiceResponse> getAll() {
        Long shopId = shopContext.getCurrentShopId();
        return invoiceRepository.findAllByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InvoiceResponse getById(Long id) {
        Long shopId = shopContext.getCurrentShopId();
        return mapToResponse(invoiceRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id)));
    }

    private String generateInvoiceNumber(Long shopId) {
        long count = invoiceRepository.countByShopId(shopId) + 1;
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return "INV-" + LocalDate.now().getYear() + "-" + shopId + "-" + String.format("%04d", count);
    }
    public String deleteInvoice(Long id) {
        Long shopId = shopContext.getCurrentShopId();
        Invoice invoice = invoiceRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        invoiceRepository.delete(invoice);
        return "Invoice deleted successfully";
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getItems().stream()
                .map(item -> new InvoiceItemResponse(
                        item.getId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice()
                )).collect(Collectors.toList());

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getName(),
                invoice.getCreatedBy().getName(),
                itemResponses,
                invoice.getSubtotal(),
                invoice.getDiscount(),
                invoice.getTaxPercent(),
                invoice.getTotalAmount(),
                invoice.getPaymentMethod(),
                invoice.getPaymentStatus(),
                invoice.getNotes(),
                invoice.getCreatedAt()
        );
    }
}