package com.monokek.printing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.escpos.barcode.QRCode;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import com.github.anastaciocintra.escpos.image.RasterBitImageWrapper;
import com.monokek.printing.application.dto.CouponContent;
import com.monokek.printing.application.dto.KitchenTicketContent;
import com.monokek.printing.application.dto.ReceiptContent;
import com.monokek.printing.application.dto.SessionSummaryContent;
import com.monokek.printing.domain.Printer;
import com.monokek.printing.infrastructure.LogoImageLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Writes {@link KitchenTicketContent}/{@link ReceiptContent}/{@link SessionSummaryContent}/
 * {@link CouponContent} as ESC/POS onto an already-open {@link EscPos} — ported line-for-line from
 * the reference Laravel implementation ({@code mike42/escpos-php}) supplied
 * by the user, using {@code escpos-coffee}'s equivalents: {@link Style} for
 * justification/size/bold, {@link QRCode} for the receipt's QR, {@link LogoImageLoader}
 * + {@link RasterBitImageWrapper} for the store logo, {@code EscPos.pulsePin}
 * for the cash-drawer kick.
 */
@Component
public class EscPosTicketRenderer {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LogoImageLoader logoImageLoader;
    private final ObjectMapper objectMapper;

    public EscPosTicketRenderer(LogoImageLoader logoImageLoader, ObjectMapper objectMapper) {
        this.logoImageLoader = logoImageLoader;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // Kitchen / bar ticket
    // ------------------------------------------------------------------

    public void renderKitchen(EscPos escpos, KitchenTicketContent content, Printer printer) throws IOException {
        String label = printer.getLocation() == null ? "CUISINE" : printer.getLocation().toUpperCase();

        escpos.writeLF(header2x(), "BON " + label);
        escpos.writeLF("");
        escpos.writeLF(normal(), "TABLE : " + orNa(content.tableName()));
        escpos.writeLF(normal(), "Serveur : " + orNa(content.serverName()));
        escpos.writeLF(normal(), LocalDateTime.now().format(TIMESTAMP_FORMAT));
        escpos.writeLF(doubleRule(printer));

        for (KitchenTicketContent.TicketItem item : content.items()) {
            escpos.writeLF(leftBold(), ("%dx %s".formatted(item.qty(), item.productName())).toUpperCase());
            for (String modifier : item.modifierNames()) {
                escpos.writeLF(normal(), "  + " + modifier);
            }
        }

        if (content.note() != null && !content.note().isBlank()) {
            escpos.writeLF(rule(printer));
            escpos.writeLF(leftBold(), "NOTE : " + content.note());
        }

        escpos.writeLF(rule(printer));
        escpos.writeLF(centered(), "Ref : " + content.orderId());

        escpos.feed(3).cut(EscPos.CutMode.FULL);
    }

    // ------------------------------------------------------------------
    // Client receipt
    // ------------------------------------------------------------------

    public void renderReceipt(EscPos escpos, ReceiptContent content, Printer printer) throws IOException {
        Optional<EscPosImage> logo = logoImageLoader.load(content.storeLogoUrl(), printer.getPaperWidth());
        if (logo.isPresent()) {
            escpos.write(new RasterBitImageWrapper().setJustification(EscPosConst.Justification.Center), logo.get());
            escpos.feed(1);
        }

        escpos.writeLF(header2x(), orNa(content.storeName()).toUpperCase());
        if (content.storeAddress() != null) {
            escpos.writeLF(centered(), content.storeAddress());
        }
        if (content.storePhone() != null) {
            escpos.writeLF(centered(), "Tel : " + content.storePhone());
        }
        escpos.writeLF(centered(), LocalDateTime.now().format(TIMESTAMP_FORMAT));
        escpos.writeLF(rule(printer));

        escpos.writeLF(normal(), "Table    : " + orNa(content.tableName()));
        escpos.writeLF(normal(), "Facture  : " + content.reference());
        escpos.writeLF(normal(), "Serveur  : " + orNa(content.serverName()));
        escpos.writeLF(normal(), "Caissier : " + orNa(content.cashierName()));
        escpos.writeLF(rule(printer));

        for (ReceiptContent.RoundSection round : content.rounds()) {
            if (round.items().isEmpty()) {
                continue;
            }
            escpos.writeLF(centered(), "--- SERVICE #" + round.roundNumber() + " ---");
            for (ReceiptContent.TicketItem item : round.items()) {
                escpos.writeLF(normal(), formatItemLine(item.qty(), item.productName(), item.total()));
                for (String modifier : item.modifierNames()) {
                    escpos.writeLF(normal(), "  + " + modifier);
                }
            }
        }

        escpos.writeLF(doubleRule(printer));
        escpos.write(rightBold2x(), "TOTAL : " + formatFcfa(content.total()));
        escpos.writeLF("");
        escpos.writeLF(rule(printer));

        escpos.writeLF(normal(), "REGLEMENTS :");
        escpos.writeLF(normal(), "- %-21s : %s".formatted(orNa(content.paymentMethod()), formatFcfa(content.total())));

        if (content.reference() != null) {
            Map<String, Object> qrData = new LinkedHashMap<>();
            qrData.put("REF", content.reference());
            qrData.put("DATE", LocalDateTime.now().format(TIMESTAMP_FORMAT));
            qrData.put("MNT", formatFcfa(content.total()));
            qrData.put("SYS", orNa(content.storeName()));
            escpos.feed(1);
            QRCode qrCode = new QRCode()
                    .setJustification(EscPosConst.Justification.Center)
                    .setModel(QRCode.QRModel._2)
                    .setSize(6)
                    .setErrorCorrectionLevel(QRCode.QRErrorCorrectionLevel.QR_ECLEVEL_L);
            escpos.write(qrCode, objectMapper.writeValueAsString(qrData));
            escpos.feed(1);
        }

        pulseDrawer(escpos);

        escpos.writeLF(centered(), "Merci de votre visite !");
        escpos.feed(3).cut(EscPos.CutMode.FULL);
    }

    // ------------------------------------------------------------------
    // Promotional coupon
    // ------------------------------------------------------------------

    public void renderCoupon(EscPos escpos, CouponContent content, Printer printer) throws IOException {
        escpos.writeLF(header2x(), orNa(content.storeName()).toUpperCase());
        if (content.storeAddress() != null) {
            escpos.writeLF(centered(), content.storeAddress());
        }
        if (content.storePhone() != null) {
            escpos.writeLF(centered(), "Tel : " + content.storePhone());
        }
        escpos.writeLF(centered(), LocalDateTime.now().format(TIMESTAMP_FORMAT));
        escpos.writeLF(doubleRule(printer));
        escpos.writeLF(centered(), "BON DE REDUCTION");
        escpos.feed(1);

        escpos.writeLF(header2x(), content.code().toUpperCase());
        escpos.feed(1);

        escpos.writeLF(centered(), "Valeur : " + formatFcfa(content.amount()));
        if (content.expiresAt() != null) {
            escpos.writeLF(centered(), "Valable jusqu'au " + content.expiresAt().substring(0, 10));
        }
        escpos.writeLF(rule(printer));
        escpos.writeLF(centered(), "A presenter en caisse lors du reglement.");

        escpos.feed(3).cut(EscPos.CutMode.FULL);
    }

    // ------------------------------------------------------------------
    // Cash session closing report (Z-report)
    // ------------------------------------------------------------------

    public void renderSessionSummary(EscPos escpos, SessionSummaryContent content, Printer printer) throws IOException {
        escpos.writeLF(header2x(), orNa(content.storeName()).toUpperCase());
        escpos.writeLF(centered(), "CLOSURE / RECAPITULATIF DE CAISSE");
        escpos.writeLF(rule(printer));

        escpos.writeLF(normal(), "Session  : #" + content.sessionId());
        escpos.writeLF(normal(), "Caissier : " + orNa(content.cashierName()));
        escpos.writeLF(normal(), "Ouverture: " + formatOrNa(content.openedAt()));
        escpos.writeLF(normal(), "Fermeture: " + formatOrNa(content.closedAt()));
        escpos.writeLF(rule(printer));

        escpos.writeLF(centered(), "--- FLUX DE CAISSE ---");
        escpos.writeLF(normal(), amountLine("Fond de caisse init.", content.openingAmount()));
        escpos.writeLF(normal(), amountLine("+ Ventes de la session", content.totalSales()));
        escpos.writeLF(normal(), dottedRule(printer));
        escpos.writeLF(normal(), amountLine("Theorique en caisse", content.expectedAmount()));
        escpos.writeLF(normal(), amountLine("Compte en caisse", content.actualAmount()));
        escpos.writeLF(normal(), amountLine("Ecart", content.difference()));
        escpos.writeLF(rule(printer));

        if (!content.paymentBreakdown().isEmpty()) {
            escpos.writeLF(centered(), "--- MODES DE REGLEMENTS ---");
            for (SessionSummaryContent.PaymentBreakdown breakdown : content.paymentBreakdown()) {
                escpos.writeLF(normal(), "- %-21s : %s".formatted(breakdown.method(), formatFcfa(breakdown.total())));
            }
            escpos.writeLF(rule(printer));
        }

        if (!content.soldItems().isEmpty()) {
            escpos.writeLF(centered(), "--- ARTICLES VENDUS ---");
            for (SessionSummaryContent.SoldItem item : content.soldItems()) {
                escpos.writeLF(normal(), formatItemLine(item.qty(), item.productName(), item.total()));
            }
            escpos.writeLF(rule(printer));
        }

        pulseDrawer(escpos);

        escpos.writeLF(centered(), LocalDateTime.now().format(TIMESTAMP_FORMAT));
        escpos.writeLF(centered(), "Rapport genere avec succes.");
        escpos.feed(3).cut(EscPos.CutMode.FULL);
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------

    private void pulseDrawer(EscPos escpos) {
        try {
            escpos.pulsePin(EscPos.PinConnector.Pin_2, 120, 240);
        } catch (Exception e) {
            // Best-effort, same as the PHP reference's own try/catch around the drawer pulse.
        }
    }

    private String formatItemLine(int qty, String productName, BigDecimal total) {
        return "%-3dx %-22s %s".formatted(qty, truncate(productName, 22), formatFcfa(total));
    }

    private String amountLine(String label, BigDecimal amount) {
        return "%-24s %s".formatted(label, formatFcfa(amount));
    }

    private String truncate(String value, int maxLength) {
        String safe = value == null ? "Article" : value;
        return safe.length() <= maxLength ? safe : safe.substring(0, Math.max(0, maxLength - 2)) + "..";
    }

    private String orNa(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String formatOrNa(LocalDateTime value) {
        return value == null ? "N/A" : value.format(TIMESTAMP_FORMAT);
    }

    private String formatFcfa(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(amount == null ? BigDecimal.ZERO : amount) + " FCFA";
    }

    private String rule(Printer printer) {
        return "-".repeat(Math.max(1, printer.getCharPerLine()));
    }

    private String doubleRule(Printer printer) {
        return "=".repeat(Math.max(1, printer.getCharPerLine()));
    }

    private String dottedRule(Printer printer) {
        return ".".repeat(Math.max(1, printer.getCharPerLine()));
    }

    private Style header2x() {
        return new Style().setJustification(EscPosConst.Justification.Center).setBold(true).setFontSize(Style.FontSize._2, Style.FontSize._2);
    }

    private Style centered() {
        return new Style().setJustification(EscPosConst.Justification.Center);
    }

    private Style normal() {
        return new Style().setJustification(EscPosConst.Justification.Left_Default);
    }

    private Style leftBold() {
        return new Style().setJustification(EscPosConst.Justification.Left_Default).setBold(true);
    }

    private Style rightBold2x() {
        return new Style().setJustification(EscPosConst.Justification.Right).setBold(true).setFontSize(Style.FontSize._2, Style.FontSize._1);
    }
}
