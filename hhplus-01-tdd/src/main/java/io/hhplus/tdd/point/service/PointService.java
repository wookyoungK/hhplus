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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService implements IPointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;
    private final LockManager lockManager;
    /**
     * 특정 유저의 포인트를 조회하는 기능
     */
    @Override
    public UserPoint getPoint(Long id) {
        log.info("getPoint id: {}", id);
        return userPointTable.selectById(id);
    }

    /**
     * 특정 유저의 포인트 충전/이용 내역을 조회하는 기능
     */
    @Override
    public List<PointHistory> getHistory(Long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * 특정 유저의 포인트를 충전하는 기능
     */
    @Override
    public UserPoint charge(Long id, Long amount) {
        if (amount < 0) {
            throw new PointOverException(amount);
        }
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            UserPoint currentPoint = userPointTable.selectById(id);

            long newAmount = currentPoint.point() + amount;
            if (newAmount > 1000000L) {
                throw new PointMaxException(newAmount);
            }

            UserPoint updatedPoint = userPointTable.insertOrUpdate(id, newAmount);
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }
    /**
     * 특정 유저의 포인트를 사용하는 기능
     */
    @Override
    public UserPoint use(Long id, Long amount) {
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            UserPoint currentPoint = userPointTable.selectById(id);

            if (currentPoint.point() < amount) {
                throw new PointNotException(amount);
            }

            long newAmount = currentPoint.point() - amount;
            UserPoint updatedPoint = userPointTable.insertOrUpdate(id, newAmount);
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            return updatedPoint;
        } finally {
            lock.unlock();
        }
    }
}
