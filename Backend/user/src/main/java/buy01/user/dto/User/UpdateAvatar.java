package buy01.user.dto.User;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAvatar {
    @NotNull(message = "avatare shouldn't be null")
    private MultipartFile image;
}
