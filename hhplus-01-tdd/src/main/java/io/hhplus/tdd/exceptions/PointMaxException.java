package io.hhplus.tdd.exceptions;

public class PointMaxException extends RuntimeException {
    public static final String MESSAGE_FORMAT = "잘못된 충전 요청: 충전 금액은 1000000넘을 수 없습니다. 충전 금액: %d";
    public PointMaxException(long value) {
        super(String.format(MESSAGE_FORMAT, value));
    }
}
