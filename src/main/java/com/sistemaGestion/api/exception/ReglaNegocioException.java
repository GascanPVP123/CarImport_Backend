package com.sistemaGestion.api.exception;

/** Datos de entrada que incumplen una regla de negocio → HTTP 400. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
