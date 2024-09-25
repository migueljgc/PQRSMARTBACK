package Proyecto.PQRSMART.Config.Exception;

public class Exceptions {

    // Excepción para cuando el usuario ya existe
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    // Excepción para cuando el email ya existe
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    // Excepción para cuando el número de identificación ya existe
    public static class IdentificationNumberAlreadyExistsException extends RuntimeException {
        public IdentificationNumberAlreadyExistsException(String message) {
            super(message);
        }
    }

    // Excepción para cuando el número ya existe
    public static class NumberAlreadyExistsException extends RuntimeException {
        public NumberAlreadyExistsException(String message) {
            super(message);
        }
    }
}
