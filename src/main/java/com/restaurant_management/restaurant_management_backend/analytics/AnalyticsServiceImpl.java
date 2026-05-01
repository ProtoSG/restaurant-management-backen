package com.restaurant_management.restaurant_management_backend.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.analytics.dto.response.AverageTicketMetricResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.BalanceIntradayResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.BalanceSummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.CategoryProductsResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailyBalanceItemResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailyBalanceResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailyBreakdownResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailySalesByPaymentResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailySalesMetricResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DailySummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.DashboardOverviewResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.EarningsSummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.HourlyBalanceResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.OccupiedTablesMetricResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.PaymentMethodBreakdownResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.PeriodComparisonResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.PeriodInfoResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.PeriodSummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.ProductRankingResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.ProfitsMetricResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.RecentPaidOrderResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.RecentPaidOrdersResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.TopProductsResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.WeekComparisonResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.WeekSummaryResponse;
import com.restaurant_management.restaurant_management_backend.analytics.dto.response.WeeklySalesResponse;
import com.restaurant_management.restaurant_management_backend.auth.entity.User;
import com.restaurant_management.restaurant_management_backend.orders.OrderItemRepository;
import com.restaurant_management.restaurant_management_backend.orders.OrderRepository;
import com.restaurant_management.restaurant_management_backend.orders.dto.internal.TopSellingProductInternal;
import com.restaurant_management.restaurant_management_backend.orders.dto.response.OrderStatusCountResponse;
import com.restaurant_management.restaurant_management_backend.orders.entity.Order;
import com.restaurant_management.restaurant_management_backend.shared.enums.TransactionStatus;
import com.restaurant_management.restaurant_management_backend.tables.TableRepository;
import com.restaurant_management.restaurant_management_backend.tables.entity.Table;
import com.restaurant_management.restaurant_management_backend.transactions.TransactionRepository;
import com.restaurant_management.restaurant_management_backend.transactions.dto.response.TransactionMountGroupByPaymentMethodResponse;
import com.restaurant_management.restaurant_management_backend.transactions.entity.Transaction;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

  @Value("${app.timezone:America/Lima}")
  private String appTimezone;

  private final TransactionRepository transactionRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final TableRepository tableRepository;

  @Override
  @Transactional
  public DailySummaryResponse getDailySummary(LocalDate date) {
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

    List<OrderStatusCountResponse> ordersByStatus = orderRepository
      .countOrdersByStatusAndDate(startDate, endDate);

    return new DailySummaryResponse(date, totalRevenue, totalOrders, averageTicket, ordersByStatus);
  }

  @Override
  public List<TopSellingProductInternal> getTopSellingProducts(int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return orderItemRepository.findTopSellingProducts(pageable);
  }

  @Override
  @Transactional
  public DashboardOverviewResponse getDashboardOverview(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

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

    LocalDateTime prevStartDate = date.minusDays(1).atStartOfDay();
    LocalDateTime prevEndDate = date.atStartOfDay();
    List<Transaction> prevTransactions = transactionRepository
      .findByTransactionDateBetweenAndStatus(prevStartDate, prevEndDate, TransactionStatus.COMPLETED);
    BigDecimal prevSales = prevTransactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    Double salesChange = calculatePercentageChange(prevSales, totalSales);

    Long totalTables = tableRepository.count();
    Long occupiedTables = tableRepository.countOccupiedTables();
    Double occupancyPercentage = totalTables > 0
      ? (occupiedTables.doubleValue() / totalTables.doubleValue()) * 100
      : 0.0;

    LocalDateTime sparklineStart = date.minusDays(6).atStartOfDay();
    List<Transaction> sparklineTransactions = transactionRepository
      .findCompletedTransactionsByDateRange(sparklineStart, endDate);
    List<BigDecimal> salesSparkline = buildDailyTotals(date, sparklineTransactions);
    List<BigDecimal> ticketSparkline = buildDailyAverageTickets(date, sparklineTransactions);
    List<Integer> tablesSparkline = java.util.Collections.nCopies(7, occupiedTables.intValue());

    return new DashboardOverviewResponse(
      date,
      new DailySalesMetricResponse(totalSales, salesChange, salesChange >= 0 ? "up" : "down", salesSparkline),
      new OccupiedTablesMetricResponse(occupiedTables.intValue(), totalTables.intValue(), occupancyPercentage, tablesSparkline),
      new ProfitsMetricResponse(totalSales, salesChange, "Por el momento igual a ventas (sin costo)", salesChange >= 0 ? "up" : "down", salesSparkline),
      new AverageTicketMetricResponse(averageTicket, totalOrders, salesChange, salesChange >= 0 ? "up" : "down", ticketSparkline)
    );
  }

  @Override
  @Transactional
  public BalanceIntradayResponse getBalanceIntraday(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository
      .findCompletedTransactionsByDateRange(startDate, endDate);

    List<HourlyBalanceResponse> hourlyData = new ArrayList<>();
    BigDecimal cumulativeBalance = BigDecimal.ZERO;
    int currentHour = LocalDateTime.now(ZoneId.of(appTimezone)).getHour();

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

      hourlyData.add(new HourlyBalanceResponse(
        hourLabel,
        String.format("%02d:00", hour),
        hourSales,
        cumulativeBalance,
        orderCount,
        baseConversionRate
      ));
    }

    Double rate = hourlyData.isEmpty() ? 0.0 : hourlyData.get(hourlyData.size() - 1).conversionRate();

    return new BalanceIntradayResponse(
      date,
      new BalanceSummaryResponse(rate, rate, cumulativeBalance, 4.75, "On track"),
      hourlyData
    );
  }

  @Override
  @Transactional
  public EarningsSummaryResponse getEarningsSummary(String period, LocalDate date) {
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
      default:
        currentStart = date;
        currentEnd = date;
        prevStart = date.minusDays(1);
        prevEnd = date.minusDays(1);
    }

    PeriodSummaryResponse currentPeriod = getPeriodSummary(currentStart, currentEnd);
    PeriodSummaryResponse previousPeriod = getPeriodSummary(prevStart, prevEnd);

    Double revenueChange = calculatePercentageChange(previousPeriod.totalRevenue(), currentPeriod.totalRevenue());
    Double ordersChange = calculatePercentageChange(
      BigDecimal.valueOf(previousPeriod.totalOrders()),
      BigDecimal.valueOf(currentPeriod.totalOrders())
    );
    Double ticketChange = calculatePercentageChange(previousPeriod.averageTicket(), currentPeriod.averageTicket());

    String message = String.format(
      "Revenue is %.0f%% %s than last %s",
      Math.abs(revenueChange),
      revenueChange >= 0 ? "More" : "Less",
      period.substring(0, 1).toUpperCase() + period.substring(1)
    );

    return new EarningsSummaryResponse(
      period,
      currentPeriod,
      previousPeriod,
      new PeriodComparisonResponse(revenueChange, ordersChange, ticketChange, message),
      100,
      "Profit margin será <100% cuando se agreguen costos"
    );
  }

  @Override
  @Transactional
  public TopProductsResponse getTopProductsWithPeriod(int limit, LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
    LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

    Pageable pageable = PageRequest.of(0, limit);
    List<TopSellingProductInternal> products = orderItemRepository.findTopSellingProductsByDateRange(start, end, pageable);

    List<ProductRankingResponse> rankings = new ArrayList<>();
    for (int i = 0; i < products.size(); i++) {
      TopSellingProductInternal p = products.get(i);
      BigDecimal avgPrice = p.quantitySold() > 0
        ? p.totalRevenue().divide(BigDecimal.valueOf(p.quantitySold()), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
      rankings.add(new ProductRankingResponse(
        i + 1, p.productId(), p.productName(), p.categoryName(),
        p.quantitySold(), p.totalRevenue(), avgPrice
      ));
    }

    return new TopProductsResponse(
      new PeriodInfoResponse(
        startDate != null ? startDate : LocalDate.now().minusMonths(1),
        endDate != null ? endDate : LocalDate.now()
      ),
      rankings.size(),
      rankings
    );
  }

  @Override
  @Transactional
  public DailySalesByPaymentResponse getDailySalesByPayment(LocalDate date) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<TransactionMountGroupByPaymentMethodResponse> paymentData = transactionRepository
      .getTotalAmountGroupedByPaymentMethodAndDate(startDate, endDate);

    BigDecimal totalSales = paymentData.stream()
      .map(TransactionMountGroupByPaymentMethodResponse::totalSum)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Integer totalTransactions = paymentData.stream()
      .map(TransactionMountGroupByPaymentMethodResponse::transactionCount)
      .reduce(0, Integer::sum);

    List<PaymentMethodBreakdownResponse> breakdowns = paymentData.stream()
      .map(pm -> {
        Double percentage = totalSales.compareTo(BigDecimal.ZERO) > 0
          ? pm.totalSum().divide(totalSales, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue()
          : 0.0;
        return new PaymentMethodBreakdownResponse(
          pm.paymentMethodType(), pm.totalSum(), pm.transactionCount(), percentage
        );
      })
      .collect(Collectors.toList());

    return new DailySalesByPaymentResponse(date, totalSales, totalTransactions, breakdowns);
  }

  @Override
  @Transactional
  public WeeklySalesResponse getWeeklySales(LocalDate startDate, LocalDate endDate) {
    LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(6);
    LocalDate end = endDate != null ? endDate : LocalDate.now();

    WeekSummaryResponse currentWeek = getWeekSummary(start, end);
    WeekSummaryResponse previousWeek = getWeekSummary(start.minusDays(7), end.minusDays(7));

    Double salesChange = calculatePercentageChange(previousWeek.totalSales(), currentWeek.totalSales());
    Double ordersChange = calculatePercentageChange(
      BigDecimal.valueOf(previousWeek.totalOrders()),
      BigDecimal.valueOf(currentWeek.totalOrders())
    );

    return new WeeklySalesResponse(
      start, end, currentWeek, previousWeek,
      new WeekComparisonResponse(salesChange, ordersChange)
    );
  }

  @Override
  @Transactional
  public CategoryProductsResponse getTopProductsByCategory(
    Long categoryId, int limit, LocalDate startDate, LocalDate endDate
  ) {
    LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
    LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();

    Pageable pageable = PageRequest.of(0, limit);
    List<TopSellingProductInternal> products = orderItemRepository
      .findTopSellingProductsByCategoryAndDateRange(categoryId, start, end, pageable);

    BigDecimal totalSales = products.stream()
      .map(TopSellingProductInternal::totalRevenue)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Long totalQuantity = products.stream()
      .map(TopSellingProductInternal::quantitySold)
      .reduce(0L, Long::sum);

    List<ProductRankingResponse> rankings = new ArrayList<>();
    for (int i = 0; i < products.size(); i++) {
      TopSellingProductInternal p = products.get(i);
      BigDecimal avgPrice = p.quantitySold() > 0
        ? p.totalRevenue().divide(BigDecimal.valueOf(p.quantitySold()), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
      rankings.add(new ProductRankingResponse(
        i + 1, p.productId(), p.productName(), p.categoryName(),
        p.quantitySold(), p.totalRevenue(), avgPrice
      ));
    }

    String categoryName = products.isEmpty() ? "Unknown" : products.get(0).categoryName();

    return new CategoryProductsResponse(categoryId, categoryName, totalSales, totalQuantity, rankings);
  }

  @Override
  @Transactional
  public DailyBalanceResponse getDailyBalance(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository.findCompletedTransactionsByDateRange(start, end);

    List<DailyBalanceItemResponse> items = new ArrayList<>();
    BigDecimal totalBalance = BigDecimal.ZERO;
    Locale locale = new Locale("es", "PE");

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      final LocalDate current = date;
      LocalDateTime dayStart = current.atStartOfDay();
      LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();

      BigDecimal daySales = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) && t.getTransactionDate().isBefore(dayEnd))
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (daySales.compareTo(BigDecimal.ZERO) > 0) {
        String label = current.getDayOfMonth() + " " +
          current.getMonth().getDisplayName(TextStyle.SHORT, locale);
        items.add(new DailyBalanceItemResponse(current, label, daySales));
        totalBalance = totalBalance.add(daySales);
      }
    }

    long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    List<Transaction> prevTransactions = transactionRepository
      .findCompletedTransactionsByDateRange(startDate.minusDays(days).atStartOfDay(), startDate.atStartOfDay());
    BigDecimal prevTotal = prevTransactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new DailyBalanceResponse(
      startDate, endDate, totalBalance, calculatePercentageChange(prevTotal, totalBalance), items
    );
  }

  @Override
  @Transactional
  public RecentPaidOrdersResponse getRecentPaidOrders(LocalDate date, int limit) {
    LocalDateTime startDate = date.atStartOfDay();
    LocalDateTime endDate = date.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository.findRecentCompletedTransactions(startDate, endDate);

    List<RecentPaidOrderResponse> orders = transactions.stream()
      .limit(limit)
      .map(t -> {
        Order order = t.getOrder();
        Table table = order.getTable();
        User cashier = t.getUser();

        return new RecentPaidOrderResponse(
          t.getId(),
          order.getId(),
          order.getOrderCode(),
          table != null ? table.getNumber() : null,
          order.getCustomerName(),
          order.getType() != null ? order.getType().name() : "DINE_IN",
          t.getTotal(),
          t.getPaymentMethod() != null ? t.getPaymentMethod().name() : "",
          t.getTransactionDate(),
          cashier != null ? cashier.getName() : ""
        );
      })
      .collect(Collectors.toList());

    return new RecentPaidOrdersResponse(date, (long) transactions.size(), orders);
  }

  private PeriodSummaryResponse getPeriodSummary(LocalDate startDate, LocalDate endDate) {
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

    return new PeriodSummaryResponse(startDate, endDate, totalRevenue, totalOrders, averageTicket);
  }

  private WeekSummaryResponse getWeekSummary(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay();

    List<Transaction> transactions = transactionRepository.findCompletedTransactionsByDateRange(start, end);

    BigDecimal totalSales = transactions.stream()
      .map(Transaction::getTotal)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    Long orderCount = orderRepository.countPaidOrdersByDate(start, end);
    int totalOrders = orderCount != null ? orderCount.intValue() : 0;

    BigDecimal averageDailySales = totalSales.compareTo(BigDecimal.ZERO) > 0
      ? totalSales.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP)
      : BigDecimal.ZERO;

    List<DailyBreakdownResponse> dailyBreakdown = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      final LocalDate currentDate = date;
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

      List<Transaction> dayTxs = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) && t.getTransactionDate().isBefore(dayEnd))
        .collect(Collectors.toList());

      BigDecimal daySales = dayTxs.stream()
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

      int dayCount = (int) dayTxs.stream().map(t -> t.getOrder().getId()).distinct().count();

      dailyBreakdown.add(new DailyBreakdownResponse(
        currentDate,
        date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
        daySales,
        dayCount,
        0.0
      ));
    }

    return new WeekSummaryResponse(totalSales, averageDailySales, totalOrders, dailyBreakdown);
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
      totals.add(transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) && t.getTransactionDate().isBefore(dayEnd))
        .map(Transaction::getTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    return totals;
  }

  private List<BigDecimal> buildDailyAverageTickets(LocalDate lastDay, List<Transaction> transactions) {
    List<BigDecimal> tickets = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate day = lastDay.minusDays(i);
      LocalDateTime dayStart = day.atStartOfDay();
      LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
      List<Transaction> dayTxs = transactions.stream()
        .filter(t -> !t.getTransactionDate().isBefore(dayStart) && t.getTransactionDate().isBefore(dayEnd))
        .collect(Collectors.toList());
      BigDecimal dayTotal = dayTxs.stream().map(Transaction::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
      long orderCount = dayTxs.stream().map(t -> t.getOrder().getId()).distinct().count();
      tickets.add(orderCount > 0
        ? dayTotal.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO);
    }
    return tickets;
  }

}
