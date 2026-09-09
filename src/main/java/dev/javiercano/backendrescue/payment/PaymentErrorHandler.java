package dev.javiercano.backendrescue.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(assignableTypes = PaymentController.class)
public class PaymentErrorHandler {
    @ExceptionHandler(PaymentException.class)
    ProblemDetail paymentError(PaymentException error) {
        var problem = ProblemDetail.forStatusAndDetail(error.status(), error.getMessage());
        problem.setProperty("code", error.code());
        return problem;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class})
    ProblemDetail invalidRequest(Exception error) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "A valid payment amount and Idempotency-Key are required.");
        problem.setProperty("code", "INVALID_PAYMENT_REQUEST");
        return problem;
    }
}
