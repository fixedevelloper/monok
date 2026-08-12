-- =============================================================================
-- V5__add_event_publication.sql
-- Spring Modulith's own event-publication registry (durable outbox for
-- @ApplicationModuleListener) — every domain event published inside a
-- @Transactional method writes here first. Without this table, EVERY event
-- publication in the app fails with "Table EVENT_PUBLICATION doesn't exist",
-- taking down the enclosing request (login, payment, order, cash session...).
--
-- app.modulith.events.jdbc-schema-initialization.enabled=true in
-- application.yml never actually created this table (Spring Modulith backs
-- off its own auto-init once it detects Flyway is managing the schema) — this
-- migration is the fix, using the exact DDL Spring Modulith itself ships in
-- spring-modulith-events-jdbc's schema-mysql.sql.
-- =============================================================================
CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION
(
    ID               VARCHAR(36)   NOT NULL,
    LISTENER_ID      VARCHAR(512)  NOT NULL,
    EVENT_TYPE       VARCHAR(512)  NOT NULL,
    SERIALIZED_EVENT VARCHAR(4000) NOT NULL,
    PUBLICATION_DATE TIMESTAMP(6)  NOT NULL,
    COMPLETION_DATE  TIMESTAMP(6)  DEFAULT NULL NULL,
    PRIMARY KEY (ID),
    INDEX EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX (COMPLETION_DATE)
);
