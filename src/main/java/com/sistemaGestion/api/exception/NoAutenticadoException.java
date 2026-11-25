package com.sistemaGestion.api.exception;

/** La operación requiere un usuario autenticado (token JWT válido) → HTTP 401. */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
