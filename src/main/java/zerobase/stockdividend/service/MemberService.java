package zerobase.stockdividend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zerobase.stockdividend.exception.impl.AlreadyExistUserException;
import zerobase.stockdividend.model.Auth;
import zerobase.stockdividend.model.MemberEntity;
import zerobase.stockdividend.persist.MemberRepository;

@Service
@Slf4j
@AllArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.memberRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "couldn't find user -> " + username));
    }

    public MemberEntity register(Auth.SignUg member) {
        boolean exists = this.memberRepository.existsByUsername(member.getUsername());
        if (exists) {
            throw new AlreadyExistUserException();
        }

        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        var result = this.memberRepository.save(member.toEntity());

        return result;
    }

    public MemberEntity authenticate(Auth.SignIn member) {
        MemberEntity user = this.memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디입니다."));

        if (!this.passwordEncoder.matches(member.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
