package br.com.austa.experiencia.exception;

/**
 * Exception for integration errors with external systems.
 */
public class IntegrationException extends RuntimeException {
    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
