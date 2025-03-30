package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;

import java.util.List;

public interface IPointService {

        UserPoint getPoint(Long userId);

        UserPoint charge(Long userId, Long amount);

        UserPoint use(Long userId, Long amount);

        List<PointHistory> getHistory(Long userId);

    }
