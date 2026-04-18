package com.restaurant_management.restaurant_management_backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.AverageTicketMetricDTO;
import com.restaurant_management.restaurant_management_backend.dto.BalanceIntradayDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailyBalanceDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailyBalanceItemDTO;
import com.restaurant_management.restaurant_management_backend.dto.BalanceSummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.CategoryProductsDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailyBreakdownDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySalesByPaymentDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySalesMetricDTO;
import com.restaurant_management.restaurant_management_backend.dto.DailySummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.DashboardOverviewDTO;
import com.restaurant_management.restaurant_management_backend.dto.EarningsSummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.HourlyBalanceDTO;
import com.restaurant_management.restaurant_management_backend.dto.OccupiedTablesMetricDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderStatusCountDTO;
import com.restaurant_management.restaurant_management_backend.dto.PaymentMethodBreakdownDTO;
import com.restaurant_management.restaurant_management_backend.dto.PeriodComparisonDTO;
import com.restaurant_management.restaurant_management_backend.dto.PeriodInfoDTO;
import com.restaurant_management.restaurant_management_backend.dto.PeriodSummaryDTO;
import com.restaurant_management.restaurant_management_backend.dto.ProductRankingDTO;
import com.restaurant_management.restaurant_management_backend.dto.ProfitsMetricDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableTransferDTO;
import com.restaurant_management.restaurant_management_backend.dto.TableTransfersResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TopProductsResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.TopSellingProductDTO;
import com.restaurant_management.restaurant_management_backend.dto.TransactionMountGroupByPaymentMethodDTO;
import com.restaurant_management.restaurant_management_backend.dto.TransferTableInfoDTO;
import com.restaurant_management.restaurant_management_backend.dto.TransferUserInfoDTO;
import com.restaurant_management.restaurant_management_backend.dto.TrendTransactionsWeekDTO;
import com.restaurant_management.restaurant_management_backend.dto.WeekComparisonDTO;
import com.restaurant_management.restaurant_management_backend.dto.RecentPaidOrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.RecentPaidOrdersResponseDTO;
import com.restaurant_management.restaurant_management_backend.dto.WeeklySalesDTO;
import com.restaurant_management.restaurant_management_backend.dto.WeekSummaryDTO;
import com.restaurant_management.restaurant_management_backend.entity.OrderItem;
import com.restaurant_management.restaurant_management_backend.entity.Table;
import com.restaurant_management.restaurant_management_backend.entity.TableTransferAudit;
import com.restaurant_management.restaurant_management_backend.entity.Transaction;
import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.enums.TransactionStatus;
import com.restaurant_management.restaurant_management_backend.repository.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.repository.OrderRepository;
import com.restaurant_management.restaurant_management_backend.repository.TableRepository;
import com.restaurant_management.restaurant_management_backend.repository.TableTransferAuditRepository;
import com.restaurant_management.restaurant_management_backend.repository.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.service.AnalyticsService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

  @Value("${app.timezone:America/Lima}")
  private String appTimezone;

  private final TransactionRepository transactionRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final TableRepository tableRepository;
  private final TableTransferAuditRepository tableTransferAuditRepository;

  @Override
  @Transactional
  public DailySummaryDTO getDailySummary(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findByTransactionDateBetweenAndStatus(startDate, endDate, TransactionStatus.COMPLETED);

    BigDecimal totalRevenue = transactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    int totalOrders = (int) transactions.stream()
      .map(t -> t.getOrder().getId())
      .distinct()
      .count();

    BigDecimal averageTicket = totalOrders > 0
      ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
      : BigDecimal.ZERO;

    List<OrderStatusCountDTO> ordersByStatus = orderRepository
      .countOrdersByStatusAndDate(startDate, endDate);

    return DailySummaryDTO.builder()
      .date(date)
      .totalRevenue(totalRevenue)
      .totalOrders(totalOrders)
      .averageTicket(averageTicket)
      .ordersByStatus(ordersByStatus)
      .build();
  }

  @Override
  public List<TopSellingProductDTO> getTopSellingProducts(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return orderItemRepository.findTopSellingProducts(pageable);
  }

  @Override
  @Transactional
  public DashboardOverviewDTO getDashboardOverview(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    // Get transactions for the day
    List<Transaction> transactions = transactionRepository
      .findByTransactionDateBetweenAndStatus(startDate, endDate, TransactionStatus.COMPLETED);

    BigDecimal totalSales = transactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Long orderCount = orderRepository.countPaidOrdersByDate(startDate, endDate);
    int totalOrders = orderCount != null ? orderCount.intValue() : 0;

    BigDecimal averageTicket = totalOrders > 0
      ? totalSales.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
      : BigDecimal.ZERO;

    // Get previous day stats for percentage change
    LocalDateTime prevStartDate = date.minusDays(1).atStartOfDay();
    LocalDateTime prevEndDate = date.atStartOfDay();
    
    List<Transaction> prevTransactions = transactionRepository
      .findByTransactionDateBetweenAndStatus(prevStartDate, prevEndDate, TransactionStatus.COMPLETED);
    
    BigDecimal prevSales = prevTransactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Double salesChange = calculatePercentageChange(prevSales, totalSales);

    // Tables info
    Long totalTables = tableRepository.count();
    Long occupiedTables = tableRepository.countOccupiedTables();
    
    Double occupancyPercentage = totalTables > 0 
      ? (occupiedTables.doubleValue() / totalTables.doubleValue()) * 100 
      : 0.0;

    // Real sparkline data: daily totals for the last 7 days
    LocalDateTime sparklineStart = date.minusDays(6).atStartOfDay();
    List<Transaction> sparklineTransactions = transactionRepository
      .findCompletedTransactionsByDateRange(sparklineStart, endDate);

    List<BigDecimal> salesSparkline = buildDailyTotals(date, sparklineTransactions);
    List<BigDecimal> ticketSparkline = buildDailyAverageTickets(date, sparklineTransactions);
    List<Integer> tablesSparkline = java.util.Collections.nCopies(7, occupiedTables.intValue());

    // Build DTOs
    DailySalesMetricDTO dailySales = DailySalesMetricDTO.builder()
      .amount(totalSales)
      .percentageChange(salesChange)
      .trend(salesChange >= 0 ? "up" : "down")
      .sparklineData(salesSparkline)
      .build();

    OccupiedTablesMetricDTO occupiedTablesMetric = OccupiedTablesMetricDTO.builder()
      .occupied(occupiedTables.intValue())
      .total(totalTables.intValue())
      .percentageOccupancy(occupancyPercentage)
      .sparklineData(tablesSparkline)
      .build();

    ProfitsMetricDTO profits = ProfitsMetricDTO.builder()
      .amount(totalSales)
      .percentageChange(salesChange)
      .trend(salesChange >= 0 ? "up" : "down")
      .note("Por el momento igual a ventas (sin costo)")
      .sparklineData(salesSparkline)
      .build();

    AverageTicketMetricDTO averageTicketMetric = AverageTicketMetricDTO.builder()
      .amount(averageTicket)
      .orderCount(totalOrders)
      .percentageChange(salesChange)
      .trend(salesChange >= 0 ? "up" : "down")
      .sparklineData(ticketSparkline)
      .build();

    return DashboardOverviewDTO.builder()
      .date(date)
      .dailySales(dailySales)
      .occupiedTables(occupiedTablesMetric)
      .profits(profits)
      .averageTicket(averageTicketMetric)
      .build();
  }

  @Override
  @Transactional
  public BalanceIntradayDTO getBalanceIntraday(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findCompletedTransactionsByDateRange(startDate, endDate);

    // Calculate hourly data
    List<HourlyBalanceDTO> hourlyData = new ArrayList<>();
    BigDecimal cumulativeBalance = BigDecimal.ZERO;
    int currentHour = LocalDateTime.now(ZoneId.of(appTimezone)).getHour();

    // Fetch table counts once outside the loop to avoid N+1
    Long occupiedNow = tableRepository.countOccupiedTables();
    Long totalTables = tableRepository.count();
    Double baseConversionRate = totalTables > 0
      ? (occupiedNow.doubleValue() / totalTables.doubleValue()) * 100
      : 0.0;

    for (int hour = 9; hour <= Math.min(currentHour, 22); hour++) {
      LocalDateTime hourStart = date.atTime(hour, 0);
      LocalDateTime hourEnd = date.atTime(hour, 59, 59);

      List<Transaction> hourTransactions = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(hourStart) &&
                     !t.getTransactionDate().isAfter(hourEnd))
        .collect(Collectors.toList());

      BigDecimal hourSales = hourTransactions.stream()
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      cumulativeBalance = cumulativeBalance.add(hourSales);

      int orderCount = (int) hourTransactions.stream()
        .map(t -> t.getOrder().getId())
        .distinct()
        .count();

      String hourLabel = String.format("%d%s",
        hour > 12 ? hour - 12 : hour,
        hour >= 12 ? "pm" : "am");

      hourlyData.add(HourlyBalanceDTO.builder()
        .hour(hourLabel)
        .time(String.format("%02d:00", hour))
        .sales(hourSales)
        .cumulativeBalance(cumulativeBalance)
        .orderCount(orderCount)
        .conversionRate(baseConversionRate)
        .build());
    }

    // Calculate summary
    Double rate = hourlyData.isEmpty() ? 0.0 : 
      hourlyData.get(hourlyData.size() - 1).getConversionRate();
    
    BalanceSummaryDTO summary = BalanceSummaryDTO.builder()
      .rate(rate)
      .rateChange(rate)
      .balance(cumulativeBalance)
      .balanceChange(4.75)
      .status("On track")
      .build();

    return BalanceIntradayDTO.builder()
      .date(date)
      .summary(summary)
      .hourlyData(hourlyData)
      .build();
  }

  @Override
  @Transactional
  public EarningsSummaryDTO getEarningsSummary(String period, LocalDate date) {
    PeriodSummaryDTO currentPeriod;
    PeriodSummaryDTO previousPeriod;
    LocalDate currentStart, currentEnd, prevStart, prevEnd;

    switch (period.toLowerCase()) {
      case "weekly":
        currentStart = date.minusDays(6);
        currentEnd = date;
        prevStart = currentStart.minusDays(7);
        prevEnd = currentStart.minusDays(1);
        break;
      case "monthly":
        currentStart = date.withDayOfMonth(1);
        currentEnd = date;
        prevStart = currentStart.minusMonths(1);
        prevEnd = currentStart.minusDays(1);
        break;
      default: // daily
        currentStart = date;
        currentEnd = date;
        prevStart = date.minusDays(1);
        prevEnd = date.minusDays(1);
    }

    currentPeriod = getPeriodSummary(currentStart, currentEnd);
    previousPeriod = getPeriodSummary(prevStart, prevEnd);

    Double revenueChange = calculatePercentageChange(
      previousPeriod.getTotalRevenue(), 
      currentPeriod.getTotalRevenue()
    );
    
    Double ordersChange = calculatePercentageChange(
      BigDecimal.valueOf(previousPeriod.getTotalOrders()), 
      BigDecimal.valueOf(currentPeriod.getTotalOrders())
    );
    
    Double ticketChange = calculatePercentageChange(
      previousPeriod.getAverageTicket(), 
      currentPeriod.getAverageTicket()
    );

    String message = String.format(
      "Revenue is %.0f%% %s than last %s",
      Math.abs(revenueChange),
      revenueChange >= 0 ? "More" : "Less",
      period.substring(0, 1).toUpperCase() + period.substring(1)
    );

    PeriodComparisonDTO comparison = PeriodComparisonDTO.builder()
      .revenueChange(revenueChange)
      .ordersChange(ordersChange)
      .ticketChange(ticketChange)
      .message(message)
      .build();

    return EarningsSummaryDTO.builder()
      .period(period)
      .currentPeriod(currentPeriod)
      .previousPeriod(previousPeriod)
      .comparison(comparison)
      .profitMargin(100)
      .note("Profit margin será <100% cuando se agreguen costos")
      .build();
  }

  @Override
  @Transactional
  public TopProductsResponseDTO getTopProductsWithPeriod(
    int limit, 
    LocalDate startDate, 
    LocalDate endDate
  ) {
    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
    LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

    Pageable pageable = PageRequest.of(0, limit);
    List<TopSellingProductDTO> products = orderItemRepository
      .findTopSellingProductsByDateRange(start, end, pageable);

    List<ProductRankingDTO> rankings = new ArrayList<>();
    for (int i = 0; i < products.size(); i++) {
      TopSellingProductDTO product = products.get(i);
      BigDecimal avgPrice = product.getQuantitySold() > 0 
        ? product.getTotalRevenue().divide(
            BigDecimal.valueOf(product.getQuantitySold()), 
            2, 
            RoundingMode.HALF_UP
          )
        : BigDecimal.ZERO;

      rankings.add(ProductRankingDTO.builder()
        .rank(i + 1)
        .productId(product.getProductId())
        .productName(product.getProductName())
        .categoryName(product.getCategoryName())
        .quantitySold(product.getQuantitySold())
        .totalRevenue(product.getTotalRevenue())
        .averagePrice(avgPrice)
        .build());
    }

    PeriodInfoDTO periodInfo = PeriodInfoDTO.builder()
      .startDate(startDate != null ? startDate : LocalDate.now().minusMonths(1))
      .endDate(endDate != null ? endDate : LocalDate.now())
      .build();

    return TopProductsResponseDTO.builder()
      .period(periodInfo)
      .totalProducts(rankings.size())
      .products(rankings)
      .build();
  }

  @Override
  @Transactional
  public TableTransfersResponseDTO getTableTransfers(LocalDate date, int limit) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<TableTransferAudit> transfers = tableTransferAuditRepository
      .findByTransferDateBetween(startDate, endDate);

    List<TableTransferDTO> transferDTOs = transfers.stream()
      .limit(limit)
      .map(transfer -> {
        Table fromTable = transfer.getFromTable();
        Table toTable = transfer.getToTable();
        User user = transfer.getUser();

        return TableTransferDTO.builder()
          .id(transfer.getId())
          .orderId(transfer.getOrder().getId())
          .orderCode(transfer.getOrder().getOrderCode())
          .fromTable(TransferTableInfoDTO.builder()
            .id(fromTable.getId())
            .number(fromTable.getNumber())
            .build())
          .toTable(TransferTableInfoDTO.builder()
            .id(toTable.getId())
            .number(toTable.getNumber())
            .build())
          .transferDate(transfer.getTransferDate())
          .orderAmount(transfer.getOrderTotal())
          .user(user != null ? TransferUserInfoDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .build() : null)
          .build();
      })
      .collect(Collectors.toList());

    Long totalTransfers = tableTransferAuditRepository
      .countTransfersByDate(startDate, endDate);

    return TableTransfersResponseDTO.builder()
      .date(date)
      .totalTransfers(totalTransfers)
      .transfers(transferDTOs)
      .build();
  }

  @Override
  @Transactional
  public DailySalesByPaymentDTO getDailySalesByPayment(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<TransactionMountGroupByPaymentMethodDTO> paymentData = transactionRepository
      .getTotalAmountGroupedByPaymentMethodAndDate(startDate, endDate);

    BigDecimal totalSales = paymentData.stream()
      .map(TransactionMountGroupByPaymentMethodDTO::getTotalSum)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Integer totalTransactions = paymentData.stream()
      .map(TransactionMountGroupByPaymentMethodDTO::getTransactionCount)
      .reduce(0, Integer::sum);

    List<PaymentMethodBreakdownDTO> breakdowns = paymentData.stream()
      .map(pm -> {
        Double percentage = totalSales.compareTo(BigDecimal.ZERO) > 0
          ? pm.getTotalSum().divide(totalSales, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue()
          : 0.0;

        return PaymentMethodBreakdownDTO.builder()
          .method(pm.getPaymentMethodType())
          .amount(pm.getTotalSum())
          .transactionCount(pm.getTransactionCount())
          .percentage(percentage)
          .build();
      })
      .collect(Collectors.toList());

    return DailySalesByPaymentDTO.builder()
      .date(date)
      .totalSales(totalSales)
      .totalTransactions(totalTransactions)
      .paymentMethods(breakdowns)
      .build();
  }

  @Override
  @Transactional
  public WeeklySalesDTO getWeeklySales(LocalDate startDate, LocalDate endDate) {
    LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(6);
    LocalDate end = endDate != null ? endDate : LocalDate.now();

    WeekSummaryDTO currentWeek = getWeekSummary(start, end);
    
    LocalDate prevStart = start.minusDays(7);
    LocalDate prevEnd = end.minusDays(7);
    WeekSummaryDTO previousWeek = getWeekSummary(prevStart, prevEnd);

    Double salesChange = calculatePercentageChange(
      previousWeek.getTotalSales(), 
      currentWeek.getTotalSales()
    );
    
    Double ordersChange = calculatePercentageChange(
      BigDecimal.valueOf(previousWeek.getTotalOrders()), 
      BigDecimal.valueOf(currentWeek.getTotalOrders())
    );

    WeekComparisonDTO comparison = WeekComparisonDTO.builder()
      .salesChange(salesChange)
      .ordersChange(ordersChange)
      .build();

    return WeeklySalesDTO.builder()
      .startDate(start)
      .endDate(end)
      .currentWeek(currentWeek)
      .previousWeek(previousWeek)
      .comparison(comparison)
      .build();
  }

  @Override
  @Transactional
  public CategoryProductsDTO getTopProductsByCategory(
    Long categoryId, 
    int limit, 
    LocalDate startDate, 
    LocalDate endDate
  ) {
    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
    LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

    Pageable pageable = PageRequest.of(0, limit);
    List<TopSellingProductDTO> products = orderItemRepository
      .findTopSellingProductsByCategoryAndDateRange(categoryId, start, end, pageable);

    BigDecimal totalSales = products.stream()
      .map(TopSellingProductDTO::getTotalRevenue)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Long totalQuantity = products.stream()
      .map(TopSellingProductDTO::getQuantitySold)
      .reduce(0L, Long::sum);

    List<ProductRankingDTO> rankings = new ArrayList<>();
    for (int i = 0; i < products.size(); i++) {
      TopSellingProductDTO product = products.get(i);
      BigDecimal avgPrice = product.getQuantitySold() > 0 
        ? product.getTotalRevenue().divide(
            BigDecimal.valueOf(product.getQuantitySold()), 
            2, 
            RoundingMode.HALF_UP
          )
        : BigDecimal.ZERO;

      rankings.add(ProductRankingDTO.builder()
        .rank(i + 1)
        .productId(product.getProductId())
        .productName(product.getProductName())
        .categoryName(product.getCategoryName())
        .quantitySold(product.getQuantitySold())
        .totalRevenue(product.getTotalRevenue())
        .averagePrice(avgPrice)
        .build());
    }

    String categoryName = products.isEmpty() ? "Unknown" : products.get(0).getCategoryName();

    return CategoryProductsDTO.builder()
      .categoryId(categoryId)
      .categoryName(categoryName)
      .totalSales(totalSales)
      .totalQuantity(totalQuantity)
      .products(rankings)
      .build();
  }

  @Override
  @Transactional
  public DailyBalanceDTO getDailyBalance(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findCompletedTransactionsByDateRange(start, end);

    List<DailyBalanceItemDTO> items = new ArrayList<>();
    BigDecimal totalBalance = BigDecimal.ZERO;
    Locale locale = new Locale("es", "PE");

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      final LocalDate current = date;
      LocalDateTime dayStart = current.atStartOfDay();
      LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();

      BigDecimal daySales = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) &&
                     t.getTransactionDate().isBefore(dayEnd))
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (daySales.compareTo(BigDecimal.ZERO) > 0) {
        String label = current.getDayOfMonth() + " " +
          current.getMonth().getDisplayName(TextStyle.SHORT, locale);

        items.add(DailyBalanceItemDTO.builder()
          .date(current)
          .label(label)
          .totalSales(daySales)
          .build());

        totalBalance = totalBalance.add(daySales);
      }
    }

    // Compare with previous period of equal length
    long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    LocalDateTime prevStart = startDate.minusDays(days).atStartOfDay();
    LocalDateTime prevEnd = startDate.atStartOfDay();
    List<Transaction> prevTransactions = transactionRepository
      .findCompletedTransactionsByDateRange(prevStart, prevEnd);
    BigDecimal prevTotal = prevTransactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Double changePercentage = calculatePercentageChange(prevTotal, totalBalance);

    return DailyBalanceDTO.builder()
      .startDate(startDate)
      .endDate(endDate)
      .totalBalance(totalBalance)
      .changePercentage(changePercentage)
      .items(items)
      .build();
  }

  // Helper methods
  
  private PeriodSummaryDTO getPeriodSummary(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findByTransactionDateBetweenAndStatus(start, end, TransactionStatus.COMPLETED);

    BigDecimal totalRevenue = transactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Long orderCount = orderRepository.countPaidOrdersByDate(start, end);
    int totalOrders = orderCount != null ? orderCount.intValue() : 0;

    BigDecimal averageTicket = totalOrders > 0
      ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
      : BigDecimal.ZERO;

    return PeriodSummaryDTO.builder()
      .startDate(startDate)
      .endDate(endDate)
      .totalRevenue(totalRevenue)
      .totalOrders(totalOrders)
      .averageTicket(averageTicket)
      .build();
  }

  private WeekSummaryDTO getWeekSummary(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findCompletedTransactionsByDateRange(start, end);

    BigDecimal totalSales = transactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Long orderCount = orderRepository.countPaidOrdersByDate(start, end);
    int totalOrders = orderCount != null ? orderCount.intValue() : 0;

    BigDecimal averageDailySales = totalSales.compareTo(BigDecimal.ZERO) > 0
      ? totalSales.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP)
      : BigDecimal.ZERO;

    // Build daily breakdown
    List<DailyBreakdownDTO> dailyBreakdown = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      final LocalDate currentDate = date;
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

      List<Transaction> dayTransactions = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) && 
                     t.getTransactionDate().isBefore(dayEnd))
        .collect(Collectors.toList());

      BigDecimal daySales = dayTransactions.stream()
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      int dayTransactionCount = (int) dayTransactions.stream()
        .map(t -> t.getOrder().getId())
        .distinct()
        .count();

      DayOfWeek dayOfWeek = date.getDayOfWeek();
      String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH);

      dailyBreakdown.add(DailyBreakdownDTO.builder()
        .date(currentDate)
        .dayOfWeek(dayName)
        .sales(daySales)
        .transactions(dayTransactionCount)
        .percentageChange(0.0) // Could calculate vs previous week same day
        .build());
    }

    return WeekSummaryDTO.builder()
      .totalSales(totalSales)
      .averageDailySales(averageDailySales)
      .totalOrders(totalOrders)
      .dailyBreakdown(dailyBreakdown)
      .build();
  }

  private Double calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
    if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
      return newValue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
    }
    return newValue.subtract(oldValue)
      .divide(oldValue, 4, RoundingMode.HALF_UP)
      .multiply(BigDecimal.valueOf(100))
      .doubleValue();
  }

  private List<BigDecimal> buildDailyTotals(LocalDate lastDay, List<Transaction> transactions) {
    List<BigDecimal> totals = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate day = lastDay.minusDays(i);
      LocalDateTime dayStart = day.atStartOfDay();
      LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
      BigDecimal dayTotal = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) &&
                     t.getTransactionDate().isBefore(dayEnd))
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
      totals.add(dayTotal);
    }
    return totals;
  }

  private List<BigDecimal> buildDailyAverageTickets(LocalDate lastDay, List<Transaction> transactions) {
    List<BigDecimal> tickets = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate day = lastDay.minusDays(i);
      LocalDateTime dayStart = day.atStartOfDay();
      LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
      List<Transaction> dayTransactions = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) &&
                     t.getTransactionDate().isBefore(dayEnd))
        .collect(Collectors.toList());

      BigDecimal dayTotal = dayTransactions.stream()
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      long orderCount = dayTransactions.stream()
        .map(t -> t.getOrder().getId())
        .distinct()
        .count();

      BigDecimal avgTicket = orderCount > 0
        ? dayTotal.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

      tickets.add(avgTicket);
    }
    return tickets;
  }

  @Override
  @Transactional
  public RecentPaidOrdersResponseDTO getRecentPaidOrders(LocalDate date, int limit) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findRecentCompletedTransactions(startDate, endDate);

    long totalTransactions = transactions.size();

    List<RecentPaidOrderDTO> orders = transactions.stream()
      .limit(limit)
      .map(t -> {
        com.restaurant_management.restaurant_management_backend.entity.Order order = t.getOrder();
        Table table = order.getTable();
        User cashier = t.getUser();

        String tableNumber = (table != null) ? table.getNumber() : null;
        String customerName = order.getCustomerName();
        String orderType = order.getType() != null ? order.getType().name() : "DINE_IN";
        String paymentMethod = t.getPaymentMethod() != null ? t.getPaymentMethod().name() : "";
        String cashierName = (cashier != null) ? cashier.getName() : "";

        return RecentPaidOrderDTO.builder()
          .transactionId(t.getId())
          .orderId(order.getId())
          .orderCode(order.getOrderCode())
          .tableNumber(tableNumber)
          .customerName(customerName)
          .orderType(orderType)
          .total(t.getTotal())
          .paymentMethod(paymentMethod)
          .paidAt(t.getTransactionDate())
          .cashierName(cashierName)
          .build();
      })
      .collect(Collectors.toList());

    return RecentPaidOrdersResponseDTO.builder()
      .date(date)
      .totalTransactions(totalTransactions)
      .orders(orders)
      .build();
  }

}
