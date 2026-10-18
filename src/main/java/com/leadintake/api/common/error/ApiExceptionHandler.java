package com.leadintake.api.common.error;

import com.leadintake.api.lead.service.LeadNotFoundException;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LeadNotFoundException.class)
    ProblemDetail handleNotFound() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Lead not found.");
        problem.setTitle("Not Found");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidRequest() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.");
        problem.setTitle("Bad Request");
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableRequestBody() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body is malformed or contains invalid values.");
        problem.setTitle("Bad Request");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleInvalidIdentifier() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter.");
        problem.setTitle("Bad Request");
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleInvalidParameters() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request parameters are invalid.");
        problem.setTitle("Bad Request");
        return problem;
    }

    // PostgreSQL reports the explicit constraint name. H2 may expose the generated
    // backing index name with a numeric suffix and additional index details.
    private static final Pattern EMAIL_UNIQUE_CONSTRAINT = Pattern.compile(
            "(?i)(?:^|\\.)uk_leads_email(?:_index_[0-9]+)?(?=\\s|$)");

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrityViolation(DataIntegrityViolationException exception) {
        if (isEmailUniquenessViolation(exception)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "A lead with this email already exists.");
            problem.setTitle("Conflict");
            return problem;
        }

        return unexpectedFailure();
    }

    private boolean isEmailUniquenessViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                if (constraintName != null
                        && EMAIL_UNIQUE_CONSTRAINT.matcher(constraintName.replace("\"", "")).find()) {
                    return true;
                }
            }

            Throwable next = cause.getCause();
            if (next == cause) {
                break;
            }
            cause = next;
        }

        return false;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedFailure(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    status,
                    "Request could not be processed.");
            problem.setTitle(status instanceof HttpStatus httpStatus
                    ? httpStatus.getReasonPhrase()
                    : "Request Error");
            return problem;
        }

        return unexpectedFailure();
    }

    private ProblemDetail unexpectedFailure() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
