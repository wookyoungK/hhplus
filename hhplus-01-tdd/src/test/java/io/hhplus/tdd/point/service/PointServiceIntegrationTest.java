package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;
    @Test
    @DisplayName("포인트 충전 / 사용 동시성 테스트 - 포인트 충전/사용이 순차적으로 처리되는지 테스트")
    public void pointChargeUseConcurrentTest() throws Exception {
        // given
        long userId = 2L;
        long initPoint = 1000L;
        long pointAdd = 500L;
        long pointUse = 1200L;

        // 포인트 초기 상태 설정
        pointService.charge(userId,initPoint);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CompletableFuture<UserPoint> thread1 = CompletableFuture.supplyAsync(() -> pointService.charge(userId, pointAdd), executor);
        CompletableFuture<UserPoint> thread2 = CompletableFuture.supplyAsync(() -> pointService.use(userId, pointUse),executor);

        // when
        CompletableFuture.allOf(thread1, thread2).join(); // blocking

        // then
        UserPoint result = pointService.getPoint(userId);
        assertThat(result.point()).isEqualTo(initPoint + pointAdd - pointUse);

        List<PointHistory> userPointHistory = pointService.getHistory(userId);
        assertThat(userPointHistory).hasSize(3);

        assertThat(userPointHistory.get(0).amount()).isEqualTo(initPoint);
        assertThat(userPointHistory.get(0).type()).isEqualTo(TransactionType.CHARGE);

        assertThat(userPointHistory.get(1).amount()).isEqualTo(pointAdd);
        assertThat(userPointHistory.get(1).type()).isEqualTo(TransactionType.CHARGE);

        assertThat(userPointHistory.get(2).amount()).isEqualTo(pointUse);
        assertThat(userPointHistory.get(2).type()).isEqualTo(TransactionType.USE);

        // 실행 중인 스레드 풀 종료
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("잔액 충전 동시성 테스트 - 동시에 10번 충전 요청이 왔을 때 정상적으로 10번 충전 되는지 확인.")
    public void pointMultiChargeConcurrentTest() throws InterruptedException {
        // given
        long userId = 1L;
        long intPoint = 1000L;
        long pointAdd = 100L;

        // 포인트 초기 상태 설정
        pointService.charge(userId, intPoint);

       ExecutorService executor = Executors.newFixedThreadPool(1000);
        AtomicInteger successCnt = new AtomicInteger();
        AtomicInteger failCnt = new AtomicInteger();
        long startTime = System.nanoTime();
        CountDownLatch latch = new CountDownLatch(10);
        try {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        pointService.charge(userId, pointAdd);
                        successCnt.incrementAndGet();
                    } catch (Exception e) {
                        failCnt.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
        } finally {
            executor.shutdown();
        }
        latch.await();

        long endTime = System.nanoTime();
        Duration duration = Duration.ofNanos(endTime - startTime);

        // then
        UserPoint result = pointService.getPoint(userId);
        assertThat(result.point()).isEqualTo(intPoint + pointAdd * 10);

        System.out.println("테스트 실행 시간: " + duration.getSeconds() + "초 " + duration.toMillisPart() + "ms");
        System.out.println("[성공] 요청 횟수: " + successCnt.get());
        System.out.println("[실패] 요청 횟수: " + failCnt.get());
        System.out.println(" 잔액: " + result.point());
    }

    @Test
    @DisplayName("테스트")
    public void test() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "test";
        });
        System.out.println(future.get());
    }
}