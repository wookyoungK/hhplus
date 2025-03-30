package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exceptions.PointMaxException;
import io.hhplus.tdd.exceptions.PointNotException;
import io.hhplus.tdd.exceptions.PointOverException;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

import io.hhplus.tdd.point.handler.LockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private UserPoint userPoint;
    private final ReentrantLock lock = new ReentrantLock();
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;
    @Mock
    LockManager lockManager;
    @InjectMocks
    private PointService pointService;
    @BeforeEach
    void setUp() {
        userPoint = new UserPoint(1L, 3000L, System.currentTimeMillis());
    }

    @Nested
    @DisplayName("특정 유저의 포인트 조회 기능")
    class getPointTest {
        @Test
        @DisplayName("존재하는 유저의 포인트를 조회하면 해당 포인트를 반환한다")
        void getPoint_whenUserExists() {
            given(userPointTable.selectById(1L)).willReturn(userPoint);

            UserPoint point = pointService.getPoint(1L);

            assertThat(point).isEqualTo(userPoint);
            assertThat(point.id()).isEqualTo(1L);
            assertThat(point.point()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("포인트 저장 이력이 없는 회원 조회시 포인트 0원으로 조회되는지 테스트")
        void getPoint_whenUserDoesNotExist() {
            given(userPointTable.selectById(2L)).willReturn(UserPoint.empty(2L));

            UserPoint emptyPoint = pointService.getPoint(2L);

            assertThat(emptyPoint.point()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("특정 유저의 포인트 히스토리 조회 기능")
    class getHistoryTest {
        @Test
        @DisplayName("신규 Point History 조회")
        void getEmptyHistory() {
            // given
            given(pointHistoryTable.selectAllByUserId(3L))
                    .willReturn(Collections.emptyList());

            // when
            List<PointHistory> actualPointHistories = pointService.getHistory(3L);

            // then
            assertThat(actualPointHistories).isEmpty();
        }

        @Test
        @DisplayName("특정 유저의 포인트 충전/이용 내역을 조회 성공 테스트" +
                "2충전/1사용 했을 때 정상적으로 히스토리 보여지는지 확인 테스트"
        )
        void getHistory() {
            // given
            insertHistory(3L, 300L, TransactionType.CHARGE);
            insertHistory(3L, 400L, TransactionType.CHARGE);
            insertHistory(3L, 500L, TransactionType.USE);

            List<PointHistory> history = List.of(
                    new PointHistory(1, 3L, 300L, TransactionType.CHARGE, System.currentTimeMillis()),
                    new PointHistory(2, 3L, 400L, TransactionType.CHARGE, System.currentTimeMillis()),
                    new PointHistory(3, 3L, 500L, TransactionType.USE, System.currentTimeMillis())
            );
            given(pointHistoryTable.selectAllByUserId(3L)).willReturn(history);

            // when
            List<PointHistory> result = pointService.getHistory(3L);

            //then
            assertThat(result).hasSize(3);
            assertThat(result).hasSize(3);
            assertThat(result.get(0).amount()).isEqualTo(300L);
            assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
            assertThat(result.get(1).amount()).isEqualTo(400L);
            assertThat(result.get(1).type()).isEqualTo(TransactionType.CHARGE);
            assertThat(result.get(2).amount()).isEqualTo(500L);
            assertThat(result.get(2).type()).isEqualTo(TransactionType.USE);
        }
    }

    @Nested
    @DisplayName("특정 유저의 포인트 충전")
    class charge{
        @Test
        @DisplayName("정상적인 포인트 충전")
        void charge() {
            // given
            given(lockManager.getLock(1L))
                    .willReturn(lock);
            //given
            given(userPointTable.selectById(1L))
                    .willReturn(userPoint);

            long chargingPoint = 5000L;
            long newPoint = userPoint.point() + chargingPoint;

            given(userPointTable.insertOrUpdate(1L, newPoint))
                    .willReturn(new UserPoint(1L, 8000L, System.currentTimeMillis()));

            //when
            UserPoint result = pointService.charge(1L, 5000L);

            //then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.point()).isEqualTo(8000L);
        }

        @ParameterizedTest
        @DisplayName("포인트 저장 실패 테스트 - 포인트 충전 시 0보다 작은 수 충전 했을 때 예외 발생 테스트")
        @ValueSource(longs = {-2000L}) // given
        void chargePointExceptionTest(long point) {
            // when & then
            assertThatThrownBy(() -> pointService.charge(2L, point))
                    .isInstanceOf(PointOverException.class)
                    .hasMessage("잘못된 충전 요청: 충전 금액은 0 이상이어야 합니다. 요청한 금액: " + point);
        }
        @Test
        @DisplayName("포인트 저장 실패 테스트 - 백만원 보다 넘게 가지면 안되는 포인트 정책 위반 하는 경우")
        void maxPointExceptionTest() {
            // given
            given(lockManager.getLock(2L))
                    .willReturn(lock);

            // given
            given(userPointTable.selectById(2L))
                    .willReturn(new UserPoint(2L, 500000L, System.currentTimeMillis()));
            // when & then
            assertThatThrownBy(() -> pointService.charge(2L, 600000L))
                    .isInstanceOf(PointMaxException.class)
                    .hasMessageContaining("잘못된 충전 요청: 충전 금액은 1000000넘을 수 없습니다. 충전 금액: " +1100000L);
        }
    }

    @Nested
    @DisplayName("특정 유저의 포인트를 사용하는 기능")
    class use {
        @Test
        @DisplayName("정상적인 포인트 사용")
        void use() {

            // given
            given(lockManager.getLock(1L))
                    .willReturn(lock);
            //given
            given(userPointTable.selectById(1L))
                    .willReturn(userPoint);

            long usingPoint = 2000L;
            long newPoint = userPoint.point() - usingPoint;

            given(userPointTable.insertOrUpdate(1L, newPoint))
                    .willReturn(new UserPoint(1L, 1000L, System.currentTimeMillis()));

            //when
            UserPoint result = pointService.use(1L, 2000L);

            //then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.point()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("포인트 사용 실패 테스트 - 기존 3000포인트만 있는데 5000 포인트 사용하려고 하면 예외 발생 테스트 ")
        void usePointExceptionTest() {
            // given
            given(lockManager.getLock(1L))
                    .willReturn(lock);
            // given
            given(userPointTable.selectById(1L))
                    .willReturn(userPoint);

            // when & then
            assertThatThrownBy(() -> pointService.use(1L, 5000L))
                    .isInstanceOf(PointNotException.class)
                    .hasMessageContaining("잘못된 사용 요청: 금액이 부족합니다 요청한 금액: "+5000L);
        }
    }

    // insertHistory 메서드는 테스트 코드에서만 사용되는 메서드
    private void insertHistory(long userId, long amount, TransactionType transactionType) {
        pointHistoryTable.insert(userId, amount, transactionType, System.currentTimeMillis());
    }
}