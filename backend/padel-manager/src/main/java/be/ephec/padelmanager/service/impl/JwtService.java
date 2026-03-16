package be.ephec.padelmanager.service.impl;

import be.ephec.padelmanager.service.IJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService implements IJwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public String generateToken(String subject, String role) {
        return generateToken(subject, role, null);
    }

    @Override
    public String generateToken(String subject, String role, Integer idSite) {
        var builder = Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey());
        if (idSite != null) {
            builder.claim("idSite", idSite);
        }
        return builder.compact();
    }

    @Override
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    @Override
    public Integer extractIdSite(String token) {
        return extractAllClaims(token).get("idSite", Integer.class);
    }

    @Override
    public boolean isTokenValide(String token, String subject) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(subject) && claims.getExpiration().after(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
