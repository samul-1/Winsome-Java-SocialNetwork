package exceptions;

public class InvalidClientArgumentsException extends Exception {
    public InvalidClientArgumentsException(String expectedType) {
        super("Expected an argument of type " + expectedType);
    }
}
