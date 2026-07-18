package buy01.user.config.Helpers;

import buy01.user.dto.User.Userdto;
import buy01.user.model.userEntity;

public class Mapper {
    public static Userdto MappToUSerDto(userEntity user) {
        Userdto dto = new Userdto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}
