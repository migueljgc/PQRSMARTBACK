package Proyecto.PQRSMART.Domain.Service;

import Proyecto.PQRSMART.Persistence.Entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private static final String SECRECT_KEY = "8A2FBD63C1A7F3E59B6E0324D725FAF4D5B243BF50B99184959ECA2E42AC39";
    public String getUserName(String token){
        return getClaim(token, Claims::getSubject);
    }
    public String genereteToken(UserDetails userDetails){
        return genereteToken(new HashMap<>(),userDetails);
    }
    public String genereteToken(Map<String, Object> extraClaims, UserDetails userDetails){
        return Jwts.builder().setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() * 1000 * 60 *60 *24))
                .signWith(getSingInKey(), SignatureAlgorithm.HS256)
                .compact();
    }



    public <T> T getClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSingInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSingInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRECT_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSingInKey()).build().parseClaimsJws(token);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username= getUserName(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    public boolean validateTokenForPasswordReset(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }

    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    public String genereteTokenEmail(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() * 1000 * 60 *60 *24))
                .signWith(getSingInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String genereteTokenCambioEmail(Long userId, String newEmail){
        return Jwts.builder()
                .claim("newEmail", newEmail)
                .setSubject(userId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() * 1000 * 60 *60 *24))
                .signWith(getSingInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public Map<String, Object> extractClaimsFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSingInKey()) // Usa la misma clave de firma que se usó al crear el token
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Extraer el userId desde el subject y el newEmail desde el claim
        String userId = claims.getSubject();
        String newEmail = (String) claims.get("newEmail");

        Map<String, Object> extractedValues = new HashMap<>();
        extractedValues.put("userId", Long.parseLong(userId));
        extractedValues.put("newEmail", newEmail);

        return extractedValues;
    }

}
