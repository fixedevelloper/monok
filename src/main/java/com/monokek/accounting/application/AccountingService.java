package com.monokek.accounting.application;

import com.monokek.common.ApiException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds the four OHADA-style export tables {@code AccountingPage.tsx} has
 * always had buttons for. Same read-only-native-SQL pattern as {@code
 * reporting.application.ReportingService} — see this module's package-info
 * for why that's the right call here instead of composing narrow
 * cross-module calls.
 */
@Service
public class AccountingService {

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<String, Function<DateRange, ReportTable>> builders = Map.of(
            "sales-summary", this::salesSummary,
            "payments-breakdown", this::paymentsBreakdown,
            "detailed-sales", this::detailedSales,
            "categories-sales", this::categoriesSales
    );

    public AccountingService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @throws ApiException 400 if {@code report} isn't one of the four keys the frontend can request. */
    @Transactional(readOnly = true)
    public ReportTable build(String report, LocalDate startDate, LocalDate endDate) {
        Function<DateRange, ReportTable> builder = builders.get(report);
        if (builder == null) {
            throw ApiException.badRequest("Rapport comptable inconnu : " + report);
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw ApiException.badRequest("Période invalide.");
        }
        return builder.apply(new DateRange(startDate, endDate));
    }

    /** "Journal Général des Ventes" — Z de caisse cumulé jour par jour, avec une ligne TOTAL en pied de journal. */
    private ReportTable salesSummary(DateRange range) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT DATE(paid_at) AS jour, COUNT(*) AS nb, SUM(total) AS brut, SUM(tax) AS tva, SUM(total - tax) AS net
                FROM orders
                WHERE status = 'paid' AND paid_at >= :start AND paid_at < :end
                GROUP BY DATE(paid_at)
                ORDER BY jour
                """, range.params(),
                (rs, i) -> List.<Object>of(
                        rs.getDate("jour").toLocalDate().toString(),
                        rs.getLong("nb"),
                        rs.getBigDecimal("brut"),
                        rs.getBigDecimal("tva"),
                        rs.getBigDecimal("net")));

        rows = withTotalRow(rows, List.of(1, 2, 3, 4), "TOTAL");
        return new ReportTable("Journal Général des Ventes", range.start(), range.end(),
                List.of("Date", "Nb Commandes", "CA Brut TTC", "TVA", "CA Net HT"), rows);
    }

    /** "Rapport des Règlements" — un encaissement par ligne, avec la référence de transaction pour le rapprochement bancaire. */
    private ReportTable paymentsBreakdown(DateRange range) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT p.created_at AS dt, o.reference AS ref, pm.name AS method, p.amount AS amount, p.reference AS txn_ref
                FROM payments p
                JOIN payment_methods pm ON p.payment_method_id = pm.id
                JOIN orders o ON p.order_id = o.id
                WHERE p.created_at >= :start AND p.created_at < :end
                ORDER BY p.created_at
                """, range.params(),
                (rs, i) -> List.<Object>of(
                        rs.getTimestamp("dt").toLocalDateTime().toString(),
                        rs.getString("ref"),
                        rs.getString("method"),
                        rs.getBigDecimal("amount"),
                        rs.getString("txn_ref") == null ? "" : rs.getString("txn_ref")));

        rows = withTotalRow(rows, List.of(3), "TOTAL");
        return new ReportTable("Rapport des Règlements", range.start(), range.end(),
                List.of("Date", "N° Facture", "Mode de Règlement", "Montant", "Référence Transaction"), rows);
    }

    /** "Journal Détaillé des Factures" — une ligne par article vendu, pour l'audit unitaire. */
    private ReportTable detailedSales(DateRange range) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT o.paid_at AS dt, o.reference AS ref, pr.name AS product, oi.qty AS qty, oi.price AS pu, oi.total AS total
                FROM order_items oi
                JOIN order_rounds orr ON oi.order_round_id = orr.id
                JOIN orders o ON orr.order_id = o.id
                JOIN products pr ON oi.product_id = pr.id
                WHERE o.status = 'paid' AND o.paid_at >= :start AND o.paid_at < :end
                ORDER BY o.paid_at, o.reference
                """, range.params(),
                (rs, i) -> List.<Object>of(
                        rs.getTimestamp("dt").toLocalDateTime().toString(),
                        rs.getString("ref"),
                        rs.getString("product"),
                        (long) rs.getInt("qty"),
                        rs.getBigDecimal("pu"),
                        rs.getBigDecimal("total")));

        rows = withTotalRow(rows, List.of(5), "TOTAL");
        return new ReportTable("Journal Détaillé des Factures", range.start(), range.end(),
                List.of("Date", "N° Facture", "Produit", "Qté", "PU", "Total Ligne"), rows);
    }

    /** "Ventes par Catégorie de Produits" — répartition du CA par pôle d'activité. */
    private ReportTable categoriesSales(DateRange range) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT c.name AS categorie, SUM(oi.total) AS ca
                FROM order_items oi
                JOIN order_rounds orr ON oi.order_round_id = orr.id
                JOIN orders o ON orr.order_id = o.id
                JOIN products pr ON oi.product_id = pr.id
                JOIN categories c ON pr.category_id = c.id
                WHERE o.status = 'paid' AND o.paid_at >= :start AND o.paid_at < :end
                GROUP BY c.id, c.name
                ORDER BY ca DESC
                """, range.params(),
                (rs, i) -> List.<Object>of(rs.getString("categorie"), rs.getBigDecimal("ca")));

        rows = withTotalRow(rows, List.of(1), "TOTAL");
        return new ReportTable("Ventes par Catégorie de Produits", range.start(), range.end(),
                List.of("Catégorie", "Chiffre d'Affaires"), rows);
    }

    /** Appends a TOTAL row summing the given (0-indexed) numeric columns — every report ends with one. */
    private List<List<Object>> withTotalRow(List<List<Object>> rows, List<Integer> numericColumns, String label) {
        if (rows.isEmpty()) {
            return rows;
        }
        int columnCount = rows.get(0).size();
        List<Object> total = new ArrayList<>(columnCount);
        for (int col = 0; col < columnCount; col++) {
            if (col == 0) {
                total.add(label);
            } else if (numericColumns.contains(col)) {
                total.add(sumColumn(rows, col));
            } else {
                total.add("");
            }
        }
        List<List<Object>> withTotal = new ArrayList<>(rows);
        withTotal.add(total);
        return withTotal;
    }

    private Object sumColumn(List<List<Object>> rows, int col) {
        Object sample = rows.get(0).get(col);
        if (sample instanceof Long) {
            return rows.stream().mapToLong(r -> (Long) r.get(col)).sum();
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (List<Object> row : rows) {
            sum = sum.add((BigDecimal) row.get(col));
        }
        return sum;
    }

    private record DateRange(LocalDate start, LocalDate end) {
        MapSqlParameterSource params() {
            return new MapSqlParameterSource()
                    .addValue("start", LocalDateTime.of(start, java.time.LocalTime.MIDNIGHT))
                    .addValue("end", LocalDateTime.of(end.plusDays(1), java.time.LocalTime.MIDNIGHT));
        }
    }
}
