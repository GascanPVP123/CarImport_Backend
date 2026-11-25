package com.sistemaGestion.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Objects;

/**
 * Traduce las excepciones de dominio del proyecto al formato { "error": "..." }.
 * Solo cubre estas excepciones propias; el resto de errores mantiene su comportamiento.
 * Los @ExceptionHandler locales de un controlador (p. ej. PagoReciboController) tienen prioridad.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<Map<String, String>> noAutenticado(NoAutenticadoException e) {
        return error(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, String>> reglaNegocio(ReglaNegocioException e) {
        return error(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> conflicto(ConflictoException e) {
        return error(HttpStatus.CONFLICT, e);
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status)
                .body(Map.of("error", Objects.toString(e.getMessage(), status.getReasonPhrase())));
    }
}
