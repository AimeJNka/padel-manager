package be.ephec.padelmanager.service;

public interface IJwtService {
    String generateToken(String subject, String role);
    String generateToken(String subject, String role, Integer idSite);
    String extractSubject(String token);
    String extractRole(String token);
    Integer extractIdSite(String token);
    boolean isTokenValide(String token, String subject);
}
