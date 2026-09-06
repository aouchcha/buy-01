package buy01.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.security.crypto.bcrypt.BCrypt;

import buy01.user.model.Roles;
import buy01.user.model.UserEntity;
import buy01.user.repository.UserRepository;

@SpringBootApplication
@EnableKafka
public class UserApplication {

	private final UserRepository UserRepository;
	private final String adminPass;

	public UserApplication(UserRepository UserRepository, @Value("${adminPassword}") String adminPass) {
		this.UserRepository = UserRepository;
		this.adminPass = adminPass;
	}

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	private void init() {
		final UserEntity user = UserRepository.findByEmail("admin@gmail.com");
		if (user == null) {
			UserEntity admin = new UserEntity();
			admin.setEmail("admin@gmail.com");
			admin.setFirstName("admin");
			admin.setLastName("admin");
			admin.setPassword(BCrypt.hashpw(adminPass, BCrypt.gensalt(12)));
			admin.setRole(Roles.ADMIN.toString());
			admin.setProfilePictureUrl(null);
			UserRepository.save(admin);
			System.out.println("Admin Created");
		}else {
			System.out.println("Admin already there");
		}
	}
}
