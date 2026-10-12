package com.teammatch.backend.exception;

import com.teammatch.backend.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(AuthenticationFailedException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(AuthenticationFailedException exception,
			HttpServletRequest request) {
		return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception,
			HttpServletRequest request) {
		return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException exception,
			HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenOperationException exception,
			HttpServletRequest request) {
		return response(HttpStatus.FORBIDDEN, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(DataIntegrityViolationException exception,
			HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "A record with one of these unique values already exists.", Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new TreeMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return response(HttpStatus.BAD_REQUEST, "Request validation failed.", fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "Request body is invalid or contains an unsupported value.", Map.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleArgumentTypeMismatch(
			MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST,
				"Path or query parameter '" + exception.getName() + "' has an invalid value.", Map.of());
	}

	private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message,
			Map<String, String> fieldErrors) {
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message,
				fieldErrors);
		return ResponseEntity.status(status).body(body);
	}
}