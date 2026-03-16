package be.ephec.padelmanager.config;

public enum Role {
    GLOBAL, SITE, LIBRE,
    ADMIN_GLOBAL, ADMIN_SITE;

    public String authority() {
        return "ROLE_" + this.name();
    }
}
