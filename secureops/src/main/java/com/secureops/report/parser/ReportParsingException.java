package com.secureops.report.parser;

/**
 * FILE: src/main/java/com/secureops/report/parser/ReportParsingException.java
 * PURPOSE: Exception thrown when report parsing fails critically.
 * WHY IT EXISTS: Distinguishes parsing errors from other runtime exceptions.
 * DEPENDENCIES: Thrown by SecurityReportParser implementations.
 */
public class ReportParsingException extends RuntimeException {

    public ReportParsingException(String message) {
        super(message);
    }

    public ReportParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}
