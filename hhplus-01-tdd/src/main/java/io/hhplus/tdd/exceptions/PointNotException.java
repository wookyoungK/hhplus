package io.hhplus.tdd.exceptions;

public class PointNotException extends RuntimeException {
    public static final String MESSAGE_FORMAT = "잘못된 사용 요청: 금액이 부족합니다 요청한 금액: %d";

    public PointNotException(long value) {
        super(String.format(MESSAGE_FORMAT, value));
    }
}
