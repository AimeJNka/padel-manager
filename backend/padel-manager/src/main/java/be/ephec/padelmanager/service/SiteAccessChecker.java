package be.ephec.padelmanager.service;

import be.ephec.padelmanager.config.Role;
import be.ephec.padelmanager.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SiteAccessChecker {

    public void check(Authentication authentication, Integer idSite) {
        boolean isGlobal = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ADMIN_GLOBAL.authority()));
        if (isGlobal) return;

        Integer adminSiteId = (Integer) authentication.getDetails();
        if (adminSiteId == null || !adminSiteId.equals(idSite)) {
            throw new ForbiddenException("Accès refusé");
        }
    }
}
