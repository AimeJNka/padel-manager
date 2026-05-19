package be.ephec.padelmanager.integration;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Factory for test Authentication tokens.
 * Use in new integration tests to avoid repeating TestingAuthenticationToken construction.
 * Existing test classes are not modified to use these — they retain their inline constants.
 */
public final class TestAuth {

    private TestAuth() {}

    public static Authentication adminGlobal() {
        return new TestingAuthenticationToken("admin_global", null, "ROLE_ADMIN_GLOBAL");
    }

    public static Authentication adminSite(int siteId) {
        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("admin_site_" + siteId, null, "ROLE_ADMIN_SITE");
        auth.setDetails(siteId);
        return auth;
    }

    public static Authentication membre(String matricule) {
        return new TestingAuthenticationToken(matricule, null, "ROLE_GLOBAL");
    }

    public static Authentication membreSite(String matricule, int siteId) {
        TestingAuthenticationToken auth =
                new TestingAuthenticationToken(matricule, null, "ROLE_SITE");
        auth.setDetails(siteId);
        return auth;
    }
}
