# ADR-001: 트랜잭션 경계 전략

- **상태**: 결정됨
- **날짜**: 2026-03-05

---

## 맥락

Step 1에서 서비스를 추출했다. 현재 `OrderService.createOrder()`에
`@Transactional`이 없어 재고 차감 → 포인트 차감 → 주문 저장의 세 단계가
부분 커밋될 수 있다.

**증명된 버그**: 포인트 부족 시 재고가 먼저 DB에 커밋된 후 예외가 발생하여
재고는 차감되고 주문은 저장되지 않는 불일치 상태가 된다.

---

## 결정

### OrderService.createOrder()

재고 차감 + 포인트 차감 + 주문 저장 + 위시 삭제를 **하나의 트랜잭션**으로 묶는다.

```
@Transactional
createOrder() {
  option.subtractQuantity()  ─┐
  member.deductPoint()        ├─ 하나의 트랜잭션. 실패 시 전부 롤백
  orderRepository.save()     ─┘
  wishRepository.delete()
  sendKakaoMessageIfPossible()  ← catch 블록으로 예외 흡수, 롤백 방지
}
```

### sendKakaoMessageIfPossible()

트랜잭션 **내부**에 유지한다. 이유:

- `catch (Exception e)` 블록이 예외를 흡수하므로 카카오 실패가 롤백을 유발하지 않는다
- best-effort 알림이므로 실패해도 주문은 유지된다
- 향후 도메인 이벤트 패턴으로 분리 가능하다

**Trade-off**: 카카오 HTTP 호출 동안 DB 커넥션을 점유하지만, catch 블록이
있으므로 위험을 허용 가능한 수준으로 낮춘다.

### OptionService, WishService

`create()`, `delete()`, `addWishIdempotent()`, `removeWish()`에 `@Transactional` 추가.
단일 write 연산이지만 명시적 트랜잭션 경계가 읽기 일관성과 실패 안전성을 보장한다.

---

## 결과

- 포인트 부족 시 재고 롤백: `OrderIntegrationTest.포인트_부족_시_주문_실패하고_재고와_포인트는_유지된다()`로 검증
- 데이터 정합성 보장: 주문 + 재고 + 포인트 + 위시가 하나의 원자적 단위로 처리
