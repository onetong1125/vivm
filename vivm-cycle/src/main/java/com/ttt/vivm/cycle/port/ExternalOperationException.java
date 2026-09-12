package com.ttt.vivm.cycle.port;

public class ExternalOperationException extends RuntimeException {

    public enum Outcome {
        FAILED,
        UNKNOWN
    }

    private final String operation;
    private final Outcome outcome;

    public ExternalOperationException(String operation, Outcome outcome, String message) {
        super(message);
        this.operation = operation;
        this.outcome = outcome;
    }

    public ExternalOperationException(String operation, Outcome outcome, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.outcome = outcome;
    }

    public String operation() {
        return operation;
    }

    public Outcome outcome() {
        return outcome;
    }
}
