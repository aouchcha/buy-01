package buy01.user.dto.Auth;

// import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class    registerRequest extends authRequest {
	@NotBlank(message = "First name is required")
    @Size(min = 3, max = 50, message = "First name must be between 3 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 3, max = 50, message = "Last name must be between 3 and 50 characters")
    private String lastName;

    @NotBlank(message = "Role is required")
    private String role;

    // private MultipartFile profilePicture;

    public registerRequest(String email, String password, String firstName, String lastName, String role/*, MultipartFile profilePicture*/) {
        super(email, password);
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        // this.profilePicture = profilePicture;
    }
}
