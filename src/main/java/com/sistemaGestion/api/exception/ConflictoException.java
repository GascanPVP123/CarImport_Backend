package com.sistemaGestion.api.exception;

/** Operación que choca con el estado actual (p. ej. voucher ya registrado) → HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
