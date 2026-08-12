package com.monokek.settings.application;

import com.monokek.settings.StoreSettings;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class StoreSettingsImpl implements StoreSettings {

    private final SettingsService settingsService;

    StoreSettingsImpl(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public StoreInfo current() {
        Map<String, Object> settings = settingsService.index();
        return new StoreInfo(
                asString(settings.get("store_name")),
                asString(settings.get("store_address")),
                asString(settings.get("store_phone")),
                asString(settings.get("store_logo")));
    }

    private String asString(Object value) {
        return value instanceof String s ? s : null;
    }
}
