package com.monokek.printing.infrastructure;

import com.github.anastaciocintra.escpos.image.BitonalThreshold;
import com.github.anastaciocintra.escpos.image.CoffeeImageImpl;
import com.github.anastaciocintra.escpos.image.EscPosImage;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Fetches {@code StoreSettings.StoreInfo.logoUrl} — either an {@code http(s)://}
 * URL (MinIO, via {@code storage.StorageService}) or a {@code data:image/...;base64,...}
 * URI (the older settings-page hack) — and converts it to an {@link EscPosImage}
 * ready for {@code EscPos.write(ImageWrapperInterface, EscPosImage)}.
 *
 * <p>Every failure (network, malformed URL, corrupt/unsupported image) is
 * swallowed and returns {@link Optional#empty()} — a broken logo must never
 * abort printing the rest of the receipt.
 */
@Component
public class LogoImageLoader {

    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(FETCH_TIMEOUT).build();

    public Optional<EscPosImage> load(String logoUrl, int paperWidthMm) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] bytes = logoUrl.startsWith("data:") ? decodeDataUri(logoUrl) : fetch(logoUrl);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return Optional.empty();
            }
            BufferedImage scaled = scaleToWidth(image, targetWidthPx(paperWidthMm));
            return Optional.of(new EscPosImage(new CoffeeImageImpl(scaled), new BitonalThreshold()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private byte[] decodeDataUri(String dataUri) {
        int comma = dataUri.indexOf(',');
        return Base64.getDecoder().decode(comma < 0 ? dataUri : dataUri.substring(comma + 1));
    }

    private byte[] fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(FETCH_TIMEOUT).GET().build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Logo injoignable : HTTP " + response.statusCode());
        }
        return response.body();
    }

    /** Roughly matches common 203dpi thermal printer resolutions for 58mm/80mm paper. */
    private int targetWidthPx(int paperWidthMm) {
        return paperWidthMm >= 80 ? 576 : 384;
    }

    private BufferedImage scaleToWidth(BufferedImage source, int targetWidth) {
        if (source.getWidth() <= targetWidth) {
            return source;
        }
        int targetHeight = (int) ((long) source.getHeight() * targetWidth / source.getWidth());
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return scaled;
    }
}
