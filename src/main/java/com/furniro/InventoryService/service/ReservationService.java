package com.furniro.InventoryService.service;

import com.furniro.InventoryService.database.entity.Stock;
import com.furniro.InventoryService.database.entity.StockReservation;
import com.furniro.InventoryService.database.repository.ReservationRepository;
import com.furniro.InventoryService.dto.req.StockItem;
import com.furniro.InventoryService.dto.req.TransactionLog;
import com.furniro.InventoryService.utils.ReservationStatus;
import com.furniro.InventoryService.utils.TransactionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final StockService stockService;
    private final ReservationRepository reservationRepository;
    private final StockTransactionService stockTransactionService;

    @Transactional
    public void handleOrderCreated(Integer orderId, List<StockItem> items) {
        log.info("Processing reservation for order: {}", orderId);

        for (StockItem item : items) {
            // 1. Cập nhật số lượng khả dụng trong bảng Stock
            Stock stock = stockService.reserveStock(item.getSku(), item.getQuantity());

            // 2. Tạo bản ghi giữ chỗ tạm thời
            StockReservation reservation = StockReservation.builder()
                    .orderID(orderId)
                    .sku(stock.getSku())
                    .quantity(item.getQuantity())
                    .expiryTime(LocalDateTime.now().plusMinutes(30))
                    .status(ReservationStatus.PENDING).build();

            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public void handlePaymentSuccess(Integer orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderIDAndStatus(orderId,
                ReservationStatus.PENDING);

        for (StockReservation res : reservations) {
            // 1. Trừ Total và Reserved trong bảng Stock
            stockService.deductStock(res.getSku(), res.getQuantity());

            // 2. Ghi nhật ký xuất kho vĩnh viễn
            TransactionLog transaction = TransactionLog.builder()
                    .sku(res.getSku())
                    .quantity(res.getQuantity())
                    .type(TransactionType.OUT)
                    .referenceID(orderId.toString())
                    .note("Xác nhận đơn hàng: " + orderId)
                    .build();

            stockTransactionService.recordTransaction(transaction);

            // 3. Cập nhật trạng thái lệnh giữ chỗ
            res.setStatus(ReservationStatus.COMPLETED);
            reservationRepository.save(res);
        }
    }

    @Transactional
    public void handleOrderCancelled(Integer orderId) {
        List<StockReservation> reservations = reservationRepository.findByOrderIDAndStatus(orderId,
                ReservationStatus.PENDING);

        for (StockReservation res : reservations) {
            // Đưa Reserved trở lại Available
            stockService.releaseStock(res.getSku(), res.getQuantity());

            res.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(res);
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservation> expired = reservationRepository
                .findAllByStatusAndExpiryTimeBefore(ReservationStatus.PENDING, now);

        if (!expired.isEmpty()) {
            log.info("Cleaning up {} expired reservations", expired.size());
            for (StockReservation res : expired) {
                stockService.releaseStock(res.getSku(), res.getQuantity());
                res.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(res);
            }
        }
    }
}