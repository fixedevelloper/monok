package com.monokek.printing.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "printers")
@Getter
@Setter
@NoArgsConstructor
public class Printer extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References company.Branch by id only — see module package-info. */
    private Long branchId;

    private String name;

    /** escpos, label, pdf */
    private String type = "escpos";

    /** usb, network, bt */
    private String connection = "network";

    /** receipt, kitchen, bar, pizza... */
    private String location = "receipt";

    private String ip;

    private int port = 9100;

    @Column(name = "char_per_line")
    private int charPerLine = 42; // 80mm ≈ 42-48 chars, 58mm ≈ 32 chars

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "paper_width")
    private int paperWidth = 58;

    @Column(name = "use_beep")
    private boolean useBeep = true;

    /** OS-registered printer name (CUPS queue / Windows printer name) a USB job is sent to via the Tauri app's local print plugin — irrelevant for network printers. */
    @Column(name = "os_printer_name")
    private String osPrinterName;
}
