package com.example.uniapp_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 用户Token 24小时
    private Long userExpiration;

    @Value("${jwt.admin-expiration:43200000}") // 管理员Token 12小时
    private Long adminExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ==================== 用户Token方法 ====================

    /**
     * 生成用户Token
     */
    public String generateUserToken(Long userId, String username) {
        return generateToken(userId, username, "USER", userExpiration);
    }

    /**
     * 从用户Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("id", Long.class);
    }

    // ==================== 管理员Token方法 ====================

    /**
     * 生成管理员Token
     */
    public String generateAdminToken(Long adminId, String username) {
        return generateToken(adminId, username, "ADMIN", adminExpiration);
    }

    /**
     * 从管理员Token中获取管理员ID
     */
    public Long getAdminIdFromToken(String token) {
        return getIdFromToken(token);
    }

    // ==================== 统一Token方法 ====================

    /**
     * 统一生成Token方法
     */
    private String generateToken(Long id, String username, String type, Long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("username", username);
        claims.put("type", type);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从Token中获取ID（用户或管理员）
     */
    public Long getIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("id", Long.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * 获取用户类型 (USER 或 ADMIN)
     */
    public String getTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("type", String.class);
    }

    /**
     * 检查是否是管理员Token
     */
    public boolean isAdminToken(String token) {
        return "ADMIN".equals(getTypeFromToken(token));
    }

    /**
     * 检查是否是用户Token
     */
    public boolean isUserToken(String token) {
        return "USER".equals(getTypeFromToken(token));
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 验证用户Token是否有效
     */
    public boolean validateUserToken(String token) {
        return validateToken(token) && isUserToken(token);
    }

    /**
     * 验证管理员Token是否有效
     */
    public boolean validateAdminToken(String token) {
        return validateToken(token) && isAdminToken(token);
    }

    /**
     * 从Token中获取所有claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查Token是否即将过期（在指定时间内）
     */
    public boolean isTokenExpiringSoon(String token, long millis) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.getTime() - System.currentTimeMillis() <= millis;
    }

    /**
     * 刷新Token（延长有效期）
     */
    public String refreshToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Long id = claims.get("id", Long.class);
        String username = claims.getSubject();
        String type = claims.get("type", String.class);

        Long expiration = "ADMIN".equals(type) ? adminExpiration : userExpiration;

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}