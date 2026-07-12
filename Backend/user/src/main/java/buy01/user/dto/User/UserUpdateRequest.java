package buy01.user.dto.User;

public record UserUpdateRequest(
        String firstName,
        String lastName,
        String email,
        String role,
        String profilePictureUrl
) {
}