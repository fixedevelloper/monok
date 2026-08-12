/**
 * Storage module: a generic file-upload endpoint backed by MinIO. Owns no
 * database table — same structural exception as {@code reporting} (see its
 * package-info) — so there's no {@code domain} package, just
 * {@code infrastructure} (the MinIO client + bucket setup) and
 * {@code application}/{@code web} for the upload use case itself.
 *
 * <p>Deliberately has zero Java-level dependency on any other module,
 * {@code catalog} included: the client uploads a file here, gets back a
 * plain public URL, and sends that URL along in whatever request actually
 * needs it (e.g. {@code CreateProductRequest.image}) — the same shape
 * {@code ProductDto.image} already had before this module existed, just
 * populated by a real upload instead of typed in by hand. Declared
 * {@code OPEN} (like {@code common}) in case a future module wants to call
 * it directly instead of round-tripping through the client.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Storage",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.monokek.storage;
