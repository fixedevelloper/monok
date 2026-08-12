package com.monokek.pms.internal;

/** Mirrors pms-modulith's {@code ApiError} wire format — only the field this client needs. */
record PmsErrorResponse(String message) {
}
