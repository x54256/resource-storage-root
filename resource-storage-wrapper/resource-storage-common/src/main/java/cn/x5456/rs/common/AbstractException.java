package cn.x5456.rs.common;

import org.springframework.http.HttpStatus;

public abstract class AbstractException extends RuntimeException {
    private HttpStatus status;
    private int code;

    public AbstractException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public AbstractException(int code, String message) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }

    public AbstractException(String message) {
        this(-1, message);
    }

    public HttpStatus getStatus() {
        return this.status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
