package gift.auth;

import gift.member.Member;
import gift.member.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional(readOnly = true)
public class KakaoAuthService {
    private final KakaoLoginProperties properties;
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public KakaoAuthService(
        KakaoLoginProperties properties,
        KakaoLoginClient kakaoLoginClient,
        MemberService memberService,
        JwtProvider jwtProvider
    ) {
        this.properties = properties;
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
    }

    public String buildAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUriString();
    }

    @Transactional
    public String processCallback(String code) {
        KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(
            kakaoToken.accessToken());
        String email = kakaoUser.email();

        Member member = memberService.findByEmailOrCreate(email);
        member.updateKakaoAccessToken(kakaoToken.accessToken());

        return jwtProvider.createToken(member.getEmail());
    }
}
