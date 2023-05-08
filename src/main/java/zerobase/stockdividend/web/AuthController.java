package zerobase.stockdividend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zerobase.stockdividend.model.Auth;
import zerobase.stockdividend.model.MemberEntity;
import zerobase.stockdividend.security.TokenProvider;
import zerobase.stockdividend.service.MemberService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    private final TokenProvider tokenProvider;

        // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Auth.SignUg request) {
        MemberEntity result = this.memberService.register(request);
        log.info("user sign -> " + request.getUsername());

        return ResponseEntity.ok(result);
    }

        // 로그인용 API
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Auth.SignIn request) {
        // 패스워드 검증
        var member = this.memberService.authenticate(request);
        var token = this.tokenProvider.generateToken(member.getUsername(), member.getRoles());
        log.info("user login -> " + request.getUsername());

        return ResponseEntity.ok(token);
    }
}
