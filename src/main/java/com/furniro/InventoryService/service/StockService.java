package com.furniro.InventoryService.service;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.furniro.InventoryService.database.entity.Stock;
import com.furniro.InventoryService.database.entity.Warehouse;
import com.furniro.InventoryService.database.repository.StockRepository;
import com.furniro.InventoryService.database.repository.WarehouseRepository;
import com.furniro.InventoryService.dto.API.AType;
import com.furniro.InventoryService.dto.API.ApiType;
import com.furniro.InventoryService.dto.req.StockReq;
import com.furniro.InventoryService.dto.req.TransactionLog;
import com.furniro.InventoryService.exception.CustomException;
import com.furniro.InventoryService.service.kafka.KafkaProducer;
import com.furniro.InventoryService.utils.InventoryErrorCode;
import com.furniro.InventoryService.utils.TransactionType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    private final WarehouseRepository warehouseRepository;

    private final StockTransactionService stockTransactionService;

    private final KafkaProducer kafkaProducer;

    // ==== HANDLER ====
    @Transactional
    public ResponseEntity<AType> createStock(StockReq req) {

        // 1. find warehouse
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new CustomException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));

        // 2. create stock
        Stock stock = Stock.builder()
                .variantID(req.getVariantId())
                .sku(req.getSku())
                .warehouse(warehouse)
                .totalQuantity(req.getTotalQuantity())
                .availableQuantity(req.getTotalQuantity())
                .lowStockThreshold(req.getLowStockThreshold() != null ? req.getLowStockThreshold() : 5)
                .build();

        stockRepository.save(stock);

        // 3. record transaction
        stockTransactionService.recordTransaction(TransactionLog.builder()
                .sku(stock.getSku())
                .type(TransactionType.IN)
                .quantity(stock.getTotalQuantity())
                .note("Initial stock creation")
                .build());

        // 4. return response
        return ResponseEntity.ok(ApiType.success(stock));
    }

    @Transactional
    public ResponseEntity<AType> updateStock(StockReq req) {
        // 1. find stock
        Stock stock = stockRepository.findById(req.getStockID())
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        TransactionType type;
        // 2. update stock
        if (TransactionType.IN.name().equalsIgnoreCase(req.getType().name())) {

            stock.setTotalQuantity(stock.getTotalQuantity() + req.getQuantity());
            stock.setAvailableQuantity(stock.getAvailableQuantity() + req.getQuantity());
            type = TransactionType.IN;

        } else if (TransactionType.OUT.name().equalsIgnoreCase(req.getType().name())) {

            if (stock.getAvailableQuantity() < req.getQuantity()) {
                throw new CustomException(InventoryErrorCode.WAREHOUSE_NOT_ENOUGH_STOCK);
            }

            stock.setTotalQuantity(stock.getTotalQuantity() - req.getQuantity());
            stock.setAvailableQuantity(stock.getAvailableQuantity() - req.getQuantity());
            type = TransactionType.OUT;

        } else {
            throw new CustomException(InventoryErrorCode.INVALID_INPUT);
        }

        stockRepository.save(stock);

        // 3. record transaction
        stockTransactionService.recordTransaction(TransactionLog.builder()
                .sku(stock.getSku())
                .type(type)
                .quantity(req.getQuantity())
                .note("Manual stock update")
                .build());

        // 4. return response
        return ResponseEntity.ok(ApiType.success(stock));
    }

    @Transactional
    public ResponseEntity<AType> deleteStock(Integer stockId) {
        // 1. find stock
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. check if stock is available
        if (stock.getAvailableQuantity() > 0) {
            throw new CustomException(InventoryErrorCode.WAREHOUSE_NOT_ENOUGH_STOCK);
        }

        // 3. delete stock
        stockRepository.deleteById(stockId);
        return ResponseEntity.ok(ApiType.success("Delete stock succeessfully!"));
    }

    public ResponseEntity<AType> getStockBySku(String sku) {

        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        return ResponseEntity.ok(ApiType.success(stock));
    }

    public ResponseEntity<AType> getAllStocks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiType.success(stockRepository.findAll(pageable)));
    }

    public ResponseEntity<AType> getLowStock(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiType.success(stockRepository.listStockLowThreshold(pageable)));
    }

    // ==== KAFKA EVENT ====
    // event when adjust stock
    @Transactional
    public void adjustStock(String sku, Integer quantity, String referenceId, String note) {

        Stock stock = stockRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        TransactionType type;
        if (quantity > 0) {
            stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
            stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);
            type = TransactionType.ADJUST;
        } else {
            if (stock.getAvailableQuantity() < Math.abs(quantity)) {
                throw new CustomException(InventoryErrorCode.WAREHOUSE_NOT_ENOUGH_STOCK);
            }
            stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
            stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);
            type = TransactionType.ADJUST;
        }

        stockRepository.save(stock);

        // record transaction
        stockTransactionService.recordTransaction(TransactionLog.builder()
                .sku(sku)
                .type(type)
                .quantity(Math.abs(quantity))
                .referenceID(referenceId)
                .note(note)
                .build());
    }

    // event when stock is restocked
    @Transactional
    public void restockBySku(String sku, Integer quantity, String referenceId, String note) {
        // 1. find stock with pessimistic lock
        Stock stock = stockRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. update stock
        stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
        stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);

        stockRepository.save(stock);

        // 3. create RESTOCK transaction
        stockTransactionService.recordTransaction(TransactionLog.builder()
                .sku(sku)
                .type(TransactionType.RESTOCK)
                .quantity(quantity)
                .referenceID(referenceId)
                .note(note)
                .build());

        // 4. check low stock threshold and publish event if needed
        if (stock.getAvailableQuantity() <= stock.getLowStockThreshold()) {
            Map<String, Object> lowStockEvent = Map.of(
                    "sku", sku,
                    "currentQuantity", stock.getAvailableQuantity(),
                    "lowStockThreshold", stock.getLowStockThreshold(),
                    "warehouseID", stock.getWarehouse().getWarehouseID(),
                    "timestamp", System.currentTimeMillis());

            kafkaProducer.send("inventory.low-stock", lowStockEvent);
            log.info("Published inventory.low-stock for SKU: {}", sku);
        }
    }

    // kafka event order.completed
    @Transactional
    public Boolean deductStock(String sku, Integer quantity, String orderId) {
        Stock stock = stockRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        if (stock.getReservedQuantity() >= quantity) {
            stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
            stock.setTotalQuantity(stock.getTotalQuantity() - quantity);
            stockRepository.save(stock);

            // Record Sale transaction
            stockTransactionService.recordTransaction(TransactionLog.builder()
                    .sku(sku)
                    .type(TransactionType.SALE)
                    .quantity(quantity)
                    .referenceID(orderId)
                    .note("Order completed")
                    .build());
            return true;
        } else {
            log.error("Not enough reserved quantity to deduct for SKU: {}. Reserved: {}, Needed: {}",
                    sku, stock.getReservedQuantity(), quantity);
            return false;
        }
    }

    // kafka event order.cancelled
    @Transactional
    public Boolean releaseStock(String sku, Integer quantity, String orderId) {

        Stock stock = stockRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        if (stock.getReservedQuantity() < quantity) {
            log.error("Not enough reserved quantity to release for SKU: {}. Reserved: {}, Requested: {}, Order ID: {}",
                    sku, stock.getReservedQuantity(), quantity, orderId);
            throw new CustomException(InventoryErrorCode.INVALID_RELEASE_QUANTITY);
        }

        stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);
        stock.setReservedQuantity(stock.getReservedQuantity() - quantity);

        stockRepository.save(stock);

        log.info("Successfully released stock for SKU: {}, Quantity: {}, Order ID: {}", sku, quantity, orderId);
        return true;
    }

    // kafka event order.pending
    @Transactional
    public Stock reserveStock(String sku, Integer quantity, String orderId) {
        // 1. find stock with pessimistic lock
        Stock stock = stockRepository.findBySkuForUpdate(sku)
                .orElseThrow(() -> new CustomException(InventoryErrorCode.STOCK_NOT_FOUND));

        // 2. check availability
        if (stock.getAvailableQuantity() < quantity) {
            throw new CustomException(InventoryErrorCode.WAREHOUSE_NOT_ENOUGH_STOCK);
        }

        // 3. update stock
        stock.setAvailableQuantity(stock.getAvailableQuantity() - quantity);
        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);

        stockRepository.save(stock);
        log.info("Reserved stock for SKU: {}, Quantity: {}, Order ID: {}", sku, quantity, orderId);

        return stock;
    }

    public org.springframework.http.ResponseEntity<byte[]> exportStockToCsv() {
        try {
            java.util.List<Stock> stocks = stockRepository.findAll();
            StringBuilder csv = new StringBuilder();
            csv.append("StockID,VariantID,SKU,TotalQuantity,ReservedQuantity,AvailableQuantity,LowStockThreshold,WarehouseID,WarehouseName\n");
            
            for (Stock s : stocks) {
                String warehouseName = s.getWarehouse() != null ? s.getWarehouse().getName() : "N/A";
                Integer warehouseId = s.getWarehouse() != null ? s.getWarehouse().getWarehouseID() : 0;
                csv.append(s.getStockID()).append(",")
                   .append(s.getVariantID()).append(",")
                   .append(s.getSku()).append(",")
                   .append(s.getTotalQuantity()).append(",")
                   .append(s.getReservedQuantity()).append(",")
                   .append(s.getAvailableQuantity()).append(",")
                   .append(s.getLowStockThreshold()).append(",")
                   .append(warehouseId).append(",")
                   .append("\"").append(warehouseName.replace("\"", "\"\"")).append("\"\n");
            }
            
            byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv"));
            headers.setContentDisposition(org.springframework.http.ContentDisposition.parse("attachment; filename=inventory_export.csv"));
            headers.setContentLength(csvBytes.length);
            
            return new org.springframework.http.ResponseEntity<>(csvBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export stock CSV", e);
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public org.springframework.http.ResponseEntity<AType> importStockFromCsv(org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(InventoryErrorCode.INVALID_INPUT);
        }
        
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            
            String line = reader.readLine(); // header
            
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                
                String[] cols = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (cols.length < 4) {
                    failCount++;
                    errors.add("Line " + lineNum + ": Invalid columns count");
                    continue;
                }
                
                try {
                    String sku = cols[0].trim();
                    Integer variantId = cols[1].trim().isEmpty() ? null : Integer.valueOf(cols[1].trim());
                    Integer warehouseId = Integer.valueOf(cols[2].trim());
                    Integer quantity = Integer.valueOf(cols[3].trim());
                    String typeStr = cols.length > 4 ? cols[4].trim() : "IN";
                    String note = cols.length > 5 ? cols[5].trim().replace("\"", "") : "Import CSV adjustment";
                    
                    Warehouse warehouse = warehouseRepository.findById(warehouseId)
                            .orElseThrow(() -> new CustomException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));
                    
                    java.util.Optional<Stock> optStock = stockRepository.findBySku(sku);
                    if (optStock.isPresent()) {
                        Stock stock = optStock.get();
                        TransactionType type;
                        if ("OUT".equalsIgnoreCase(typeStr)) {
                            if (stock.getAvailableQuantity() < quantity) {
                                throw new CustomException(InventoryErrorCode.WAREHOUSE_NOT_ENOUGH_STOCK);
                            }
                            stock.setTotalQuantity(stock.getTotalQuantity() - quantity);
                            stock.setAvailableQuantity(stock.getAvailableQuantity() - quantity);
                            type = TransactionType.OUT;
                        } else {
                            stock.setTotalQuantity(stock.getTotalQuantity() + quantity);
                            stock.setAvailableQuantity(stock.getAvailableQuantity() + quantity);
                            type = TransactionType.IN;
                        }
                        stockRepository.save(stock);
                        
                        stockTransactionService.recordTransaction(TransactionLog.builder()
                                .sku(stock.getSku())
                                .type(type)
                                .quantity(quantity)
                                .note(note)
                                .build());
                    } else {
                        if (variantId == null) {
                            throw new IllegalArgumentException("VariantID is required for new SKU: " + sku);
                        }
                        Stock stock = Stock.builder()
                                .variantID(variantId)
                                .sku(sku)
                                .warehouse(warehouse)
                                .totalQuantity(quantity)
                                .availableQuantity(quantity)
                                .lowStockThreshold(5)
                                .build();
                        
                        stockRepository.save(stock);
                        
                        stockTransactionService.recordTransaction(TransactionLog.builder()
                                .sku(stock.getSku())
                                .type(TransactionType.IN)
                                .quantity(quantity)
                                .note("Initial import creation: " + note)
                                .build());
                    }
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    errors.add("Line " + lineNum + ": " + ex.getMessage());
                }
            }
            
            java.util.Map<String, Object> result = java.util.Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "errors", errors
            );
            return org.springframework.http.ResponseEntity.ok(ApiType.success(result));
        } catch (Exception e) {
            log.error("Failed to parse CSV upload", e);
            throw new CustomException(InventoryErrorCode.INVALID_INPUT);
        }
    }

}
