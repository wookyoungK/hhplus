package io.hhplus.tdd.exceptions;

public class PointOverException extends RuntimeException {
    public static final String MESSAGE_FORMAT = "잘못된 충전 요청: 충전 금액은 0 이상이어야 합니다. 요청한 금액: %d";

    public PointOverException(long value) {
        super(String.format(MESSAGE_FORMAT, value));
    }
}
