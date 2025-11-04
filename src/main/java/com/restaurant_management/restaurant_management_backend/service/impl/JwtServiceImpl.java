package com.restaurant_management.restaurant_management_backend.service.impl;

import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.entity.User;
import com.restaurant_management.restaurant_management_backend.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;

  @Value("${application.security.jwt.expiration}")
  private long jwtExpiration;

  @Value("${application.security.jwt.refresh-token.expiration}")
  private long refreshExpiration;


  public String extractUsername(final String token) {
    final Claims jwtToken = Jwts.parser()
      .verifyWith(getSignInKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();

    return jwtToken.getSubject();
  }

  public String generateToken(final User user) {
    return buildToken(user, jwtExpiration);
  }

  public String generateRefreshToken(final User user) {
    return buildToken(user, refreshExpiration);
  }

  private String buildToken(final User user, final long expiration) {
    return Jwts.builder()
      .id(user.getId().toString())
      .claims(Map.of("email", user.getEmail()))
      .subject(user.getEmail())
      .issuedAt(new Date(System.currentTimeMillis()))
      .expiration(new Date(System.currentTimeMillis() + expiration))
      .signWith(getSignInKey())
      .compact();
  }

  public boolean isTokenValid(final String token, final User user) {
    final String username = extractUsername(token);
    return (username.equals(user.getEmail())) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(final String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(final String token) {
    final Claims jwtToken = Jwts.parser()
      .verifyWith(getSignInKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();

    return jwtToken.getExpiration();
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  } 
  
}

