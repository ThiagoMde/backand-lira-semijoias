package com.lirasemijoias.projeto.exception;

import com.lirasemijoias.projeto.dto.common.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFoundBuildsResourceNotFoundError() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("Produto nao encontrado"));

        assertError(response, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Produto nao encontrado", Map.of());
    }

    @Test
    void handleBusinessBuildsBusinessError() {
        ResponseEntity<ApiError> response = handler.handleBusiness(
                new BusinessException("Operacao invalida"));

        assertError(response, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR",
                "Operacao invalida", Map.of());
    }

    @Test
    void handleInsufficientStockBuildsBusinessError() {
        ResponseEntity<ApiError> response = handler.handleBusiness(
                new InsufficientStockException("Estoque insuficiente"));

        assertError(response, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR",
                "Estoque insuficiente", Map.of());
    }

    @Test
    void handleValidationMapsEveryInvalidField() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Nome obrigatorio"));
        bindingResult.addError(new FieldError("request", "email", "Email invalido"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(exception);

        assertError(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Dados inv\u00e1lidos.", Map.of(
                        "name", "Nome obrigatorio",
                        "email", "Email invalido"));
    }

    @Test
    void handleValidationKeepsLastMessageWhenFieldHasMoreThanOneError() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Primeira mensagem"));
        bindingResult.addError(new FieldError("request", "name", "Ultima mensagem"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(exception);

        assertNotNull(response.getBody());
        assertEquals(Map.of("name", "Ultima mensagem"), response.getBody().fields());
    }

    @Test
    void handleBadCredentialsBuildsUnauthorizedError() {
        ResponseEntity<ApiError> response = handler.handleBadCredentials(
                new BadCredentialsException("ignored"));

        assertError(response, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Email ou senha inv\u00e1lidos.", Map.of());
    }

    @Test
    void handleDeniedBuildsForbiddenError() {
        ResponseEntity<ApiError> response = handler.handleDenied(
                new AccessDeniedException("ignored"));

        assertError(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Acesso negado.", Map.of());
    }

    @Test
    void handleGenericBuildsInternalServerErrorWithoutLeakingExceptionMessage() {
        ResponseEntity<ApiError> response = handler.handleGeneric(
                new Exception("sensitive database details"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Erro interno do servidor.", Map.of());
    }

    private void assertError(ResponseEntity<ApiError> response, HttpStatus status,
                             String error, String message, Map<String, String> fields) {
        assertEquals(status, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.timestamp());
        assertTrue(body.timestamp().getYear() >= 2025);
        assertEquals(status.value(), body.status());
        assertEquals(error, body.error());
        assertEquals(message, body.message());
        assertEquals(fields, body.fields());
    }
}
