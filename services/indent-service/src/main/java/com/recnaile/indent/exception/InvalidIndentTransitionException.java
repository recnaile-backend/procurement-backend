package com.recnaile.indent.exception;

import com.recnaile.indent.domain.enums.IndentStatus;

public class InvalidIndentTransitionException extends RuntimeException {

    public InvalidIndentTransitionException(IndentStatus from, IndentStatus to) {
        super("Invalid indent status transition: " + from + " -> " + to);
    }
}
