package com.finplan.finance.web;

import org.springframework.security.core.Authentication;

import java.util.UUID;

final class CurrentUser {
    private CurrentUser() {}

    /** The JWT subject is the user id; the filter put it in as the authentication name. */
    static UUID id(Authentication auth) { return UUID.fromString(auth.getName()); }
}
