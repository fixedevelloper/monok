package com.monokek.settings.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monokek.settings.domain.Setting;
import com.monokek.settings.domain.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Admin\SettingsController}'s
 * {@code index}/{@code update} — the only two methods any route actually
 * reaches. {@code printers}/{@code storePrint}/{@code edtPrint} are real
 * methods on the Laravel controller but no route calls any of them (printer
 * CRUD lives in {@code PrinterController}, already ported in {@code printing}),
 * so there's nothing to port there.
 *
 * <p>{@code Setting::value} is cast {@code json} in Eloquent even though the
 * column is plain {@code TEXT} — every value, however simple, is stored
 * JSON-encoded. Replicated here with Jackson at this service boundary
 * instead of in the entity, since {@code settings.domain.Setting} has no
 * reason to know about JSON serialization.
 */
@Service
public class SettingsService {

    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    public SettingsService(SettingRepository settingRepository, ObjectMapper objectMapper) {
        this.settingRepository = settingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> index() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Setting setting : settingRepository.findAll()) {
            result.put(setting.getKey(), decode(setting.getValue()));
        }
        return result;
    }

    @Transactional
    public Map<String, Object> update(Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            Setting setting = settingRepository.findByKey(entry.getKey()).orElseGet(() -> {
                Setting created = new Setting();
                created.setKey(entry.getKey());
                return created;
            });
            setting.setValue(encode(entry.getValue()));
            settingRepository.save(setting);
        }
        return index();
    }

    private Object decode(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(rawValue, Object.class);
        } catch (JsonProcessingException e) {
            // Tolerate rows written before this JSON convention existed — surface the raw text rather than failing the whole listing.
            return rawValue;
        }
    }

    private String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Impossible de sérialiser le réglage", e);
        }
    }
}
