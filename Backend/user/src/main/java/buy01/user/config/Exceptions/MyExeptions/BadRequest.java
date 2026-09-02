package buy01.user.config.Exceptions.MyExeptions;

public class BadRequest extends RuntimeException {
    public BadRequest(String message) {
        super(message);
    }
}
