package com.headstartech.vas.service;

/**
 * @author Per Johansson
 */
public class UnexpectedMessage extends RuntimeException {

    public UnexpectedMessage(String message) {
        super(message);
    }
}
