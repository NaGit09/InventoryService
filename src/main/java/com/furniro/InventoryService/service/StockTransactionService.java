package com.furniro.InventoryService.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.furniro.InventoryService.database.entity.StockTransaction;
import com.furniro.InventoryService.database.repository.StockTransactionRepository;
import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.API.ApiType;
import com.furniro.InventoryService.dto.req.TransactionLog;
import com.furniro.InventoryService.exception.InventoryException;
import com.furniro.InventoryService.utils.InventoryErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;

    @Transactional(Transactional.TxType.MANDATORY)
    public void recordTransaction(TransactionLog log) {

        StockTransaction transaction = StockTransaction.builder()
                .sku(log.getSku())
                .type(log.getType())
                .quantity(log.getQuantity())
                .referenceID(log.getReferenceID())
                .note(log.getNote())
                .build();

        stockTransactionRepository.save(transaction);
    }

    public ResponseEntity<AType> getAllTransactions(
            String sku,
            int page,
            int size) {
        try {
            // 1. Tạo Pageable (phân trang)
            Pageable pageable = PageRequest.of(page, size, Sort.by("transactionId").descending());

            // 2. Tìm kiếm theo SKU (nếu có), nếu không có thì lấy tất cả
            Page<StockTransaction> pageResult;

            if (sku != null && !sku.trim().isEmpty()) {
                // Tìm kiếm theo SKU
                pageResult = stockTransactionRepository.findBySku(sku, pageable);
            } else {
                // Lấy tất cả
                pageResult = stockTransactionRepository.findAll(pageable);
            }

            // 3. Trả về dữ liệu
            return ResponseEntity.ok(ApiType.builder()
            .code(200)
            .data(pageResult)
            .message("Lấy danh sách giao dịch thành công").build());

        } catch (Exception e) {
            log.error("Error getting transactions: ", e);
            throw new InventoryException(InventoryErrorCode.SYSTEM_ERROR);
        }
    }
}