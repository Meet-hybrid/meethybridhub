package com.meethybridhub.identity;

/**
 * Kinds of security-relevant events recorded in the {@code audit_log} table
 * (see V2__identity.sql). Stored as the enum name in a VARCHAR(100) column —
 * appending values is safe; renaming existing values is NOT (historic rows
 * would silently change meaning).
 */
public enum AuditEventType {
    REGISTER,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    EMAIL_VERIFIED,
    PASSWORD_RESET_CONFIRMED,
    PASSWORD_CHANGED,
    ROLES_UPDATED,
    USER_STATUS_UPDATED,
    USER_DELETED,
    STORE_CREATED,
    STORE_STATUS_UPDATED,
    STORE_SETTINGS_UPDATED,
    LOGOUT
}
