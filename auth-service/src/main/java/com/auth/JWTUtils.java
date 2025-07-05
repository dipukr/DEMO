package com.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JWTUtils {

	private final Key key = Keys.hmacShaKeyFor("mysecretkeymysecretkeymysecretkey".getBytes()); // 256-bit

	public String generateToken(String username) {
		return Jwts.builder()
				.setSubject(username)
				.setIssuer("auth-service")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public String validateTokenAndGetUsername(String token) {
		try {
			return Jwts.parserBuilder()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token)
					.getBody()
					.getSubject();
		} catch (JwtException e) {
			return null;
		}
	}
}
