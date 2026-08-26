package com.monokek.accounting.application;

import com.monokek.common.ApiException;
import org.springframework.dao.EmptyResultDataAccessException;
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
    private final Map<String, Function<ReportParams, ReportTable>> builders = Map.of(
            "sales-summary", this::salesSummary,
            "payments-breakdown", this::paymentsBreakdown,
            "detailed-sales", this::detailedSales,
            "categories-sales", this::categoriesSales
    );

    public AccountingService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code branchId} null means unscoped (owner/super-admin) — see {@code identity.CurrentUser#branchId}.
     * @throws ApiException 400 if {@code report} isn't one of the four keys the frontend can request. */
    @Transactional(readOnly = true)
    public ReportTable build(String report, LocalDate startDate, LocalDate endDate, Long branchId) {
        Function<ReportParams, ReportTable> builder = builders.get(report);
        if (builder == null) {
            throw ApiException.badRequest("Rapport comptable inconnu : " + report);
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw ApiException.badRequest("Période invalide.");
        }
        return builder.apply(new ReportParams(startDate, endDate, branchId));
    }

    /** "Commandes du Shift" — every payment settled during one cash-register session (a shift:
     * open-to-close on one till), for the accountant to reconcile against that till's physical
     * cash count. {@code effectiveBranchId} null means unscoped (owner/super-admin); otherwise
     * a session belonging to another branch 404s exactly like one that doesn't exist at all,
     * rather than leaking that it does — same reasoning as everywhere else a caller could
     * otherwise probe ids outside their own branch.
     * @throws ApiException 404 if the session doesn't exist or belongs to another branch. */
    @Transactional(readOnly = true)
    public ReportTable buildForSession(Long sessionId, Long effectiveBranchId) {
        SessionInfo session = findSession(sessionId);
        if (session == null || (effectiveBranchId != null && !effectiveBranchId.equals(session.branchId()))) {
            throw ApiException.notFound("Session de caisse introuvable.");
        }

        MapSqlParameterSource sessionParams = new MapSqlParameterSource("sessionId", sessionId);
        List<List<Object>> rows = jdbc.query(
                """
                SELECT p.created_at AS dt, o.reference AS ref, t.name AS table_name, u.name AS waiter,
                       pm.name AS method, p.amount AS amount
                FROM payments p
                JOIN orders o ON p.order_id = o.id
                LEFT JOIN restaurant_tables t ON o.table_id = t.id
                LEFT JOIN users u ON o.user_id = u.id
                JOIN payment_methods pm ON p.payment_method_id = pm.id
                WHERE p.cash_session_id = :sessionId
                ORDER BY p.created_at
                """, sessionParams,
                (rs, i) -> List.<Object>of(
                        rs.getTimestamp("dt").toLocalDateTime().toString(),
                        rs.getString("ref"),
                        rs.getString("table_name") == null ? "Emporter" : rs.getString("table_name"),
                        rs.getString("waiter") == null ? "" : rs.getString("waiter"),
                        rs.getString("method"),
                        rs.getBigDecimal("amount")));

        rows = withTotalRow(rows, List.of(5), "TOTAL");
        // "-" not "—" : PdfReportWriter's WinAnsi encoding can't render an em dash (drops to "?").
        String title = "Commandes du Shift - %s (%s)".formatted(session.registerName(), session.cashierName());
        LocalDate start = session.openedAt().toLocalDate();
        LocalDate end = (session.closedAt() != null ? session.closedAt() : session.openedAt()).toLocalDate();
        return new ReportTable(title, start, end,
                List.of("Heure", "N° Facture", "Table", "Serveur", "Mode de Règlement", "Montant"), rows);
    }

    private SessionInfo findSession(Long sessionId) {
        try {
            return jdbc.queryForObject(
                    """
                    SELECT cs.opened_at, cs.closed_at, cr.branch_id, cr.name AS register_name,
                           COALESCE(u.name, 'Utilisateur inconnu') AS cashier_name
                    FROM cash_sessions cs
                    JOIN cash_registers cr ON cs.register_id = cr.id
                    LEFT JOIN users u ON cs.user_id = u.id
                    WHERE cs.id = :sessionId
                    """,
                    new MapSqlParameterSource("sessionId", sessionId),
                    (rs, i) -> new SessionInfo(
                            rs.getTimestamp("opened_at").toLocalDateTime(),
                            rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toLocalDateTime(),
                            rs.getObject("branch_id", Long.class),
                            rs.getString("register_name"),
                            rs.getString("cashier_name")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private record SessionInfo(LocalDateTime openedAt, LocalDateTime closedAt, Long branchId, String registerName, String cashierName) {
    }

    /** Backs the "Exporter Excel" button on the admin order-history page ({@code history/page.tsx}) —
     * every filter mirrors {@code OrderController#historyAdmin}/{@code OrderRepository#search} exactly
     * (search matches only the order reference, {@code status} is an exact match, dates are optional
     * bounds on {@code created_at}), so the export always matches whatever the page currently shows.
     * {@code branchId} null means unscoped, same convention as {@link #build}. Kept separate from the
     * {@code builders} map above since its parameter shape (optional dates, search, status) doesn't fit
     * the four fixed OHADA reports' {@code ReportParams} — same reasoning as {@link #buildForSession}. */
    @Transactional(readOnly = true)
    public ReportTable ordersHistory(String search, String status, LocalDate startDate, LocalDate endDate, Long branchId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("search", (search == null || search.isBlank()) ? null : search)
                .addValue("status", (status == null || status.isBlank()) ? null : status)
                .addValue("from", startDate == null ? null : startDate.atStartOfDay())
                .addValue("to", endDate == null ? null : endDate.atTime(java.time.LocalTime.MAX))
                .addValue("branchId", branchId);

        List<List<Object>> rows = jdbc.query(
                """
                SELECT o.created_at AS dt, o.reference AS ref, t.name AS table_name, u.name AS waiter,
                       o.status AS status, o.subtotal AS subtotal, o.discount AS discount, o.total AS total
                FROM orders o
                LEFT JOIN restaurant_tables t ON o.table_id = t.id
                LEFT JOIN users u ON o.user_id = u.id
                WHERE (:search IS NULL OR LOWER(o.reference) LIKE LOWER(CONCAT('%', :search, '%')))
                  AND (:status IS NULL OR o.status = :status)
                  AND (:from IS NULL OR o.created_at >= :from)
                  AND (:to IS NULL OR o.created_at <= :to)
                  AND (:branchId IS NULL OR o.branch_id = :branchId)
                ORDER BY o.created_at DESC
                """, params,
                (rs, i) -> List.<Object>of(
                        rs.getTimestamp("dt").toLocalDateTime().toString(),
                        rs.getString("ref"),
                        rs.getString("table_name") == null ? "Emporter" : rs.getString("table_name"),
                        rs.getString("waiter") == null ? "" : rs.getString("waiter"),
                        orderStatusLabel(rs.getString("status")),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("total")));

        rows = withTotalRow(rows, List.of(5, 6, 7), "TOTAL");
        LocalDate start = startDate != null ? startDate : LocalDate.EPOCH;
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return new ReportTable("Historique des Commandes", start, end,
                List.of("Date", "N° Commande", "Table", "Serveur", "Statut", "Sous-total", "Remise", "Total"), rows);
    }

    private String orderStatusLabel(String status) {
        return switch (status) {
            case "paid" -> "Payée";
            case "cancelled" -> "Annulée";
            case "completed" -> "Attente paiement";
            default -> status;
        };
    }

    /** "Journal Général des Ventes" — Z de caisse cumulé jour par jour, avec une ligne TOTAL en pied de journal. */
    private ReportTable salesSummary(ReportParams params) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT DATE(paid_at) AS jour, COUNT(*) AS nb, SUM(total) AS brut, SUM(tax) AS tva, SUM(total - tax) AS net
                FROM orders
                WHERE status = 'paid' AND paid_at >= :start AND paid_at < :end
                  AND (:branchId IS NULL OR branch_id = :branchId)
                GROUP BY DATE(paid_at)
                ORDER BY jour
                """, params.sqlParams(),
                (rs, i) -> List.<Object>of(
                        rs.getDate("jour").toLocalDate().toString(),
                        rs.getLong("nb"),
                        rs.getBigDecimal("brut"),
                        rs.getBigDecimal("tva"),
                        rs.getBigDecimal("net")));

        rows = withTotalRow(rows, List.of(1, 2, 3, 4), "TOTAL");
        return new ReportTable("Journal Général des Ventes", params.start(), params.end(),
                List.of("Date", "Nb Commandes", "CA Brut TTC", "TVA", "CA Net HT"), rows);
    }

    /** "Rapport des Règlements" — un encaissement par ligne, avec la référence de transaction pour le rapprochement bancaire. */
    private ReportTable paymentsBreakdown(ReportParams params) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT p.created_at AS dt, o.reference AS ref, pm.name AS method, p.amount AS amount, p.reference AS txn_ref
                FROM payments p
                JOIN payment_methods pm ON p.payment_method_id = pm.id
                JOIN orders o ON p.order_id = o.id
                WHERE p.created_at >= :start AND p.created_at < :end
                  AND (:branchId IS NULL OR o.branch_id = :branchId)
                ORDER BY p.created_at
                """, params.sqlParams(),
                (rs, i) -> List.<Object>of(
                        rs.getTimestamp("dt").toLocalDateTime().toString(),
                        rs.getString("ref"),
                        rs.getString("method"),
                        rs.getBigDecimal("amount"),
                        rs.getString("txn_ref") == null ? "" : rs.getString("txn_ref")));

        rows = withTotalRow(rows, List.of(3), "TOTAL");
        return new ReportTable("Rapport des Règlements", params.start(), params.end(),
                List.of("Date", "N° Facture", "Mode de Règlement", "Montant", "Référence Transaction"), rows);
    }

    /** "Journal Détaillé des Factures" — une ligne par article vendu, pour l'audit unitaire. */
    private ReportTable detailedSales(ReportParams params) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT o.paid_at AS dt, o.reference AS ref, pr.name AS product, oi.qty AS qty, oi.price AS pu, oi.total AS total
                FROM order_items oi
                JOIN order_rounds orr ON oi.order_round_id = orr.id
                JOIN orders o ON orr.order_id = o.id
                JOIN products pr ON oi.product_id = pr.id
                WHERE o.status = 'paid' AND o.paid_at >= :start AND o.paid_at < :end
                  AND (:branchId IS NULL OR o.branch_id = :branchId)
                ORDER BY o.paid_at, o.reference
                """, params.sqlParams(),
                (rs, i) -> List.<Object>of(
                        rs.getTimestamp("dt").toLocalDateTime().toString(),
                        rs.getString("ref"),
                        rs.getString("product"),
                        (long) rs.getInt("qty"),
                        rs.getBigDecimal("pu"),
                        rs.getBigDecimal("total")));

        rows = withTotalRow(rows, List.of(5), "TOTAL");
        return new ReportTable("Journal Détaillé des Factures", params.start(), params.end(),
                List.of("Date", "N° Facture", "Produit", "Qté", "PU", "Total Ligne"), rows);
    }

    /** "Ventes par Catégorie de Produits" — répartition du CA par pôle d'activité. */
    private ReportTable categoriesSales(ReportParams params) {
        List<List<Object>> rows = jdbc.query(
                """
                SELECT c.name AS categorie, SUM(oi.total) AS ca
                FROM order_items oi
                JOIN order_rounds orr ON oi.order_round_id = orr.id
                JOIN orders o ON orr.order_id = o.id
                JOIN products pr ON oi.product_id = pr.id
                JOIN categories c ON pr.category_id = c.id
                WHERE o.status = 'paid' AND o.paid_at >= :start AND o.paid_at < :end
                  AND (:branchId IS NULL OR o.branch_id = :branchId)
                GROUP BY c.id, c.name
                ORDER BY ca DESC
                """, params.sqlParams(),
                (rs, i) -> List.<Object>of(rs.getString("categorie"), rs.getBigDecimal("ca")));

        rows = withTotalRow(rows, List.of(1), "TOTAL");
        return new ReportTable("Ventes par Catégorie de Produits", params.start(), params.end(),
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

    private record ReportParams(LocalDate start, LocalDate end, Long branchId) {
        MapSqlParameterSource sqlParams() {
            return new MapSqlParameterSource()
                    .addValue("start", LocalDateTime.of(start, java.time.LocalTime.MIDNIGHT))
                    .addValue("end", LocalDateTime.of(end.plusDays(1), java.time.LocalTime.MIDNIGHT))
                    .addValue("branchId", branchId);
        }
    }
}
