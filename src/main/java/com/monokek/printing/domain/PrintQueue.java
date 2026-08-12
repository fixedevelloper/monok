package com.monokek.printing.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "print_queues")
@Getter
@Setter
@NoArgsConstructor
public class PrintQueue extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "printer_id", nullable = false)
    private Printer printer;

    /** order, kitchen, bill */
    @Column(name = "job_type", nullable = false)
    private String jobType;

    /** Structured payload (items, prices, table number...) stored as native JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String content;

    private int attempts = 0;

    /** pending, printing, completed, failed */
    private String status = "pending";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 1 = normal, 2 = high, 3 = urgent. Explicit default: unlike the DB column's DEFAULT 1, Hibernate sends this field NULL if unset, which the NOT NULL constraint rejects. */
    @Column(name = "priority", nullable = false)
    private Byte priority = 1; // Maps to TINYINT

    @Column(name = "printed_at")
    private LocalDateTime printedAt;
}
