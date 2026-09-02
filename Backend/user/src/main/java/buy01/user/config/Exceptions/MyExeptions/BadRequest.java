package buy01.user.config.Exceptions.MyExeptions;

public class badRequest extends RuntimeException {
    public badRequest(String message) {
        super(message);
    }
}
