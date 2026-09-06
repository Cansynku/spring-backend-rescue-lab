package dev.javiercano.backendrescue.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderErrorHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ProblemDetail invalidRequest(Exception error) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "A valid order identifier, email and positive amount with at most two decimal places are required.");
        problem.setProperty("code", "INVALID_ORDER_REQUEST");
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail statusError(ResponseStatusException error) {
        var problem = ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
        problem.setProperty("code", error.getStatusCode().value() == 404 ? "ORDER_NOT_FOUND" : "ORDER_REQUEST_FAILED");
        return problem;
    }
}
