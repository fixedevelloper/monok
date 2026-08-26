package com.monokek.reporting.application;

import com.monokek.reporting.web.dto.StockMovementPageDto;
import com.monokek.reporting.web.dto.StockMovementRowDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Same architectural exception as {@code ReportingService} (see this module's package-info):
 * read-only native SQL against tables owned by {@code inventory} ({@code stock_movements}) and
 * {@code catalog} ({@code product_stock_movements}), UNIONed into one feed for the admin stock
 * history page. Neither module exposes a published interface for "every movement across every
 * subject with these filters" — composing that from {@code inventory}/{@code catalog}'s own
 * narrow interfaces would mean pulling both full result sets into Java and merge-sorting by hand,
 * same reasoning {@code reporting}/{@code accounting} already document for their own cross-table
 * aggregations.
 */
@Service
public class StockMovementReportingService {

    private static final String UNION_SQL = """
            SELECT sm.created_at AS occurred_at, 'ingredient' AS subject_type, i.name AS subject_name,
                   sm.type AS base_type,
                   CASE WHEN sm.type = 'out' THEN -sm.qty ELSE sm.qty END AS signed_qty,
                   sm.reason AS reason, COALESCE(u.name, 'Système') AS author_name
            FROM stock_movements sm
            JOIN ingredients i ON sm.ingredient_id = i.id
            LEFT JOIN users u ON sm.author_id = u.id

            UNION ALL

            SELECT psm.created_at AS occurred_at, 'product' AS subject_type, p.name AS subject_name,
                   psm.type AS base_type,
                   psm.qty AS signed_qty,
                   psm.reason AS reason, COALESCE(u.name, 'Système') AS author_name
            FROM product_stock_movements psm
            JOIN products p ON psm.product_id = p.id
            LEFT JOIN users u ON psm.author_id = u.id
            """;

    private static final String WHERE_SQL = """
            WHERE (:search IS NULL OR LOWER(subject_name) LIKE LOWER(CONCAT('%', :search, '%'))
                                    OR LOWER(COALESCE(reason, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:type IS NULL
                   OR (:type = 'purchase' AND reason LIKE 'Achat%')
                   OR (:type <> 'purchase' AND base_type = :type))
              AND (:from IS NULL OR occurred_at >= :from)
              AND (:to IS NULL OR occurred_at <= :to)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public StockMovementReportingService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {@code type} is "in"/"out"/"adjust"/"purchase" — "purchase" narrows to entries whose reason
     * marks them as a supplier purchase (still fundamentally an "in" movement, see class doc), any
     * other value filters on the raw movement type. Null/blank means no type filter at all. */
    @Transactional(readOnly = true)
    public StockMovementPageDto search(String search, String type, LocalDate startDate, LocalDate endDate, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("search", blankToNull(search))
                .addValue("type", blankToNull(type))
                .addValue("from", startDate == null ? null : startDate.atStartOfDay())
                .addValue("to", endDate == null ? null : endDate.atTime(LocalTime.MAX));

        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" + UNION_SQL + ") movements " + WHERE_SQL, params, Long.class);

        params.addValue("limit", size).addValue("offset", page * size);
        var rows = jdbc.query(
                "SELECT * FROM (" + UNION_SQL + ") movements " + WHERE_SQL + " ORDER BY occurred_at DESC LIMIT :limit OFFSET :offset",
                params,
                (rs, rowNum) -> new StockMovementRowDto(
                        rs.getTimestamp("occurred_at").toLocalDateTime().toString(),
                        rs.getString("subject_type"),
                        rs.getString("subject_name"),
                        rs.getString("base_type"),
                        rs.getBigDecimal("signed_qty"),
                        rs.getString("reason"),
                        rs.getString("author_name"),
                        rs.getString("reason") != null && rs.getString("reason").startsWith("Achat")));

        return new StockMovementPageDto(rows, total);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
