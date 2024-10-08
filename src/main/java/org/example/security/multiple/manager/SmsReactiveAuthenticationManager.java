package org.example.security.multiple.manager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.exception.SmsTokenException;
import org.example.security.model.UserDetail;
import org.example.security.multiple.authentication.SmsAuthenticationToken;
import org.example.service.SmsService;
import org.example.service.UserService;
import org.springframework.security.authentication.AbstractUserDetailsReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;


@Slf4j
@AllArgsConstructor
public class SmsReactiveAuthenticationManager extends AbstractUserDetailsReactiveAuthenticationManager {

    private final SmsService smsService;

    private final UserService userService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof SmsAuthenticationToken)){
            return Mono.empty();
        }
        String phone = authentication.getName();
        String smsCode = (String) authentication.getCredentials();
        return smsService.checkAndReturn(phone, smsCode)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new SmsTokenException("短信验证码错误"))))
                .map(userDetails -> (UserDetail)userDetails)
                .flatMap(userDetail -> Mono.just(createSmsAuthenticationToken(userDetail)));
    }

    @Override
    protected Mono<UserDetails> retrieveUser(String username) {
        return userService.findByUsername(username);
    }

    private SmsAuthenticationToken createSmsAuthenticationToken(UserDetail userDetail){
        return new SmsAuthenticationToken(userDetail, userDetail.getPassword(), userDetail.getEnabled(), userDetail.getAuthorities());
    }
}
