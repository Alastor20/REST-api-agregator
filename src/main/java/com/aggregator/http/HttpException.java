package com.aggregator.http;

public class HttpException extends Exception {
    public HttpException(String msg) {
        super(msg);
    }

    public HttpException(String msg, Throwable cause) {
        super(msg, cause);
    }
}