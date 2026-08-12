package com.monokek.storage.web.dto;

import java.time.Instant;

public record StorageObjectDto(String key, String url, long size, Instant lastModified) {
}
