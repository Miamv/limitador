package ar.edu.um.ingenieria.limitador.exceptions;

public class BlacklistedException extends RuntimeException {

    public BlacklistedException(String message) {
        super(message);
    }
}
