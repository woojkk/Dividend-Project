package zerobase.stockdividend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.jsoup.helper.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import zerobase.stockdividend.service.MemberService;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    private static final long TOKEN_EXPIRE_TIME = 1000 * 60 * 60; //1시간

    private static final String KEY_ROLES = "roles";

    private final MemberService memberService;

    @Value("${spring.jwt.secret}")
    private String secretKey;

    //토큰 생성 메서드
    public String generateToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(KEY_ROLES, roles);

        var now = new Date();
        var expiredDate =new Date(now.getTime() + TOKEN_EXPIRE_TIME);
        //1시간동안 토큰 유효

        return Jwts.builder().setClaims(claims)
                .setIssuedAt(now)   //토큰 생성시간
                .setExpiration(expiredDate) //토큰 만료시간
                .signWith(SignatureAlgorithm.HS512, this.secretKey) //사용할 암호화 알고리즘,비밀키
                .compact();
    }

    public Authentication getAuthentication(String jwt) {
        UserDetails userDetails = this.memberService.loadUserByUsername(this.getUsername(jwt));

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return this.parseClaims(token).getSubject();    //유에서 넣은 유저네임
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        var claims = this.parseClaims(token);
        return !claims.getExpiration().before(new Date());
                        //만료시간   //현재시간비교한 값(이전인지아닌지) //유효한지아닌지
    }

    public Claims parseClaims(String token) {
        try {

        return Jwts.parser().setSigningKey(this.secretKey)
                .parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
