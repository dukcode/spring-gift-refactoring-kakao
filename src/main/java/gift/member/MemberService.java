package gift.member;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + id));
    }

    public Member register(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        return memberRepository.save(new Member(email, password));
    }

    public Member login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.matchesPassword(password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return member;
    }

    @Transactional
    public Member update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password);
        return member;
    }

    @Transactional
    public Member chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
        return member;
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
