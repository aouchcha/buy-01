package buy01.user.dto.User;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMe {
    @NotNull(message = "first name shouldn't be null")
    private String firstName;
    @NotNull(message = "last name shouldn't be null")
    private String lastName;
}
