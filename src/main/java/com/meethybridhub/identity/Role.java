package com.meethybridhub.identity;

import java.util.Locale;


public enum Role {

    ADMIN,
    STORE_OWNER,
    CUSTOMER;


    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
