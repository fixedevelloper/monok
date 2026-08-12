package com.monokek.reporting.application;

import com.monokek.reporting.web.dto.AnalyticsResponse;
import com.monokek.reporting.web.dto.CategorySales;
import com.monokek.reporting.web.dto.ClosingReportResponse;
import com.monokek.reporting.web.dto.DashboardStatsResponse;
import com.monokek.reporting.web.dto.PaymentMethodTotal;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\ReportController}. Unlike
 * every other module, this one owns no table and no domain entities — it
 * runs plain SQL against tables owned by {@code ordering}, {@code cashier},
 * {@code catalog} and {@code identity}. See {@code package-info.java} for
 * why that's a deliberate, one-time exception rather than the established
 * "modules own their data, cross-module reads go through a published
 * interface" rule.
 */
@Service
public class ReportingService {

    private final NamedParameterJdbcTemplate jdbc;

    public ReportingService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboardStats() {
        LocalDate today = LocalDate.now();
        MapSqlParameterSource params = dayRange(today);

        BigDecimal totalRevenue = jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(total), 0) FROM orders
                WHERE status = 'completed' AND created_at >= :start AND created_at < :end
                """, params, BigDecimal.class);

        List<PaymentMethodTotal> revenueByMethod = jdbc.query(
                """
                SELECT pm.name AS method, COUNT(*) AS cnt, SUM(p.amount) AS total
                FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.created_at >= :start AND p.created_at < :end
                GROUP BY pm.id, pm.name
                ORDER BY total DESC
                """, params,
                (rs, rowNum) -> new PaymentMethodTotal(rs.getString("method"), rs.getLong("cnt"), rs.getBigDecimal("total")));

        Long ordersCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE created_at >= :start AND created_at < :end",
                params, Long.class);

        List<DashboardStatsResponse.TopProduct> topProducts = jdbc.query(
                """
                SELECT p.name AS product, SUM(oi.qty) AS qty
                FROM order_items oi JOIN products p ON oi.product_id = p.id
                WHERE oi.created_at >= :start AND oi.created_at < :end
                GROUP BY p.id, p.name
                ORDER BY qty DESC
                LIMIT 5
                """, params,
                (rs, rowNum) -> new DashboardStatsResponse.TopProduct(rs.getString("product"), rs.getLong("qty")));

        return new DashboardStatsResponse(today.toString(), totalRevenue, revenueByMethod, ordersCount, topProducts);
    }

    @Transactional(readOnly = true)
    public List<CategorySales> salesByCategory(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : YearMonth.now().atDay(1);
        LocalDate end = endDate != null ? endDate : YearMonth.now().atEndOfMonth();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start.atStartOfDay())
                .addValue("end", end.plusDays(1).atStartOfDay());

        return jdbc.query(
                """
                SELECT c.name AS category, SUM(oi.total) AS total_sales
                FROM order_items oi
                JOIN products p ON oi.product_id = p.id
                JOIN categories c ON p.category_id = c.id
                WHERE oi.created_at >= :start AND oi.created_at < :end
                GROUP BY c.id, c.name
                ORDER BY total_sales DESC
                """, params,
                (rs, rowNum) -> new CategorySales(rs.getString("category"), rs.getBigDecimal("total_sales")));
    }

    @Transactional(readOnly = true)
    public ClosingReportResponse closingReport(Long currentUserId, String currentUserName) {
        LocalDate today = LocalDate.now();
        MapSqlParameterSource params = dayRange(today).addValue("userId", currentUserId);

        List<PaymentMethodTotal> breakdown = jdbc.query(
                """
                SELECT pm.name AS method, COUNT(*) AS cnt, SUM(p.amount) AS total
                FROM payments p
                JOIN payment_methods pm ON p.payment_method_id = pm.id
                JOIN orders o ON p.order_id = o.id
                WHERE o.user_id = :userId AND o.status = 'completed'
                  AND o.created_at >= :start AND o.created_at < :end
                GROUP BY pm.id, pm.name
                ORDER BY total DESC
                """, params,
                (rs, rowNum) -> new PaymentMethodTotal(rs.getString("method"), rs.getLong("cnt"), rs.getBigDecimal("total")));

        BigDecimal total = breakdown.stream().map(PaymentMethodTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ClosingReportResponse(currentUserName, today.toString(), breakdown, total);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : YearMonth.now().atDay(1);
        LocalDate end = endDate != null ? endDate : YearMonth.now().atEndOfMonth();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("start", start.atStartOfDay())
                .addValue("end", end.plusDays(1).atStartOfDay());

        BigDecimal totalSales = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE created_at >= :start AND created_at < :end",
                params, BigDecimal.class);

        Long ordersCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE created_at >= :start AND created_at < :end",
                params, Long.class);

        BigDecimal averageCart = ordersCount != null && ordersCount > 0
                ? totalSales.divide(BigDecimal.valueOf(ordersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<AnalyticsResponse.HourlyFlow> hourlyFlow = jdbc.query(
                """
                SELECT HOUR(created_at) AS hr, COUNT(*) AS cnt FROM orders
                WHERE created_at >= :start AND created_at < :end
                GROUP BY hr ORDER BY hr
                """, params,
                (rs, rowNum) -> new AnalyticsResponse.HourlyFlow(rs.getInt("hr"), rs.getLong("cnt")));

        List<AnalyticsResponse.WaiterPerformance> waiterPerformance = jdbc.query(
                """
                SELECT u.name AS waiter, SUM(o.total) AS sales, COUNT(o.id) AS cnt
                FROM orders o JOIN users u ON o.user_id = u.id
                WHERE o.created_at >= :start AND o.created_at < :end
                GROUP BY u.id, u.name
                ORDER BY sales DESC
                LIMIT 5
                """, params,
                (rs, rowNum) -> new AnalyticsResponse.WaiterPerformance(rs.getString("waiter"), rs.getBigDecimal("sales"), rs.getLong("cnt")));

        List<PaymentMethodTotal> paymentsByMethod = jdbc.query(
                """
                SELECT pm.name AS method, COUNT(*) AS cnt, SUM(p.amount) AS total
                FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.created_at >= :start AND p.created_at < :end
                GROUP BY pm.id, pm.name
                ORDER BY total DESC
                """, params,
                (rs, rowNum) -> new PaymentMethodTotal(rs.getString("method"), rs.getLong("cnt"), rs.getBigDecimal("total")));

        List<AnalyticsResponse.SalesOverTime> salesOverTime = jdbc.query(
                """
                SELECT DATE(created_at) AS dt, SUM(total) AS total FROM orders
                WHERE status = 'paid' AND created_at >= :start AND created_at < :end
                GROUP BY dt ORDER BY dt
                """, params,
                (rs, rowNum) -> new AnalyticsResponse.SalesOverTime(rs.getDate("dt").toString(), rs.getBigDecimal("total")));

        List<AnalyticsResponse.TopProduct> topProducts = jdbc.query(
                """
                SELECT p.name AS product, SUM(oi.qty) AS qty, SUM(oi.total) AS revenue
                FROM order_items oi
                JOIN products p ON oi.product_id = p.id
                JOIN order_rounds orr ON oi.order_round_id = orr.id
                JOIN orders o ON orr.order_id = o.id
                WHERE o.status = 'paid' AND o.created_at >= :start AND o.created_at < :end
                GROUP BY p.id, p.name
                ORDER BY qty DESC
                LIMIT 5
                """, params,
                (rs, rowNum) -> new AnalyticsResponse.TopProduct(rs.getString("product"), rs.getLong("qty"), rs.getBigDecimal("revenue")));

        // Laravel hardcodes food_cost => 32 with a comment saying it should be
        // made dynamic later "if necessary" — that placeholder was never
        // implemented, so it's carried over as-is rather than invented.
        int foodCostPercent = 32;

        return new AnalyticsResponse(start.toString(), end.toString(), totalSales, ordersCount, averageCart,
                foodCostPercent, hourlyFlow, waiterPerformance, paymentsByMethod, salesOverTime, topProducts);
    }

    private MapSqlParameterSource dayRange(LocalDate day) {
        LocalDateTime start = LocalDateTime.of(day, LocalTime.MIDNIGHT);
        return new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", start.plusDays(1));
    }
}
