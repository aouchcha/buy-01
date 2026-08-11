package buy01.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.security.crypto.bcrypt.BCrypt;

import buy01.user.model.Roles;
import buy01.user.model.userEntity;
import buy01.user.repository.userRepository;

@SpringBootApplication
@EnableKafka
public class UserApplication {

	private final userRepository userRepository;
	private final String adminPass;

	public UserApplication(userRepository userRepository, @Value("${adminPassword}") String adminPass) {
		this.userRepository = userRepository;
		this.adminPass = adminPass;
	}

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	private void init() {
		final userEntity user = userRepository.findByEmail("admin@gmail.com");
		if (user == null) {
			userEntity admin = new userEntity();
			admin.setEmail("admin@gmail.com");
			admin.setFirstName("admin");
			admin.setLastName("admin");
			admin.setPassword(BCrypt.hashpw(adminPass, BCrypt.gensalt(12)));
			admin.setRole(Roles.ADMIN.toString());
			admin.setProfilePictureUrl(null);
			userRepository.save(admin);
			System.out.println("Admin Created");
		}else {
			System.out.println("Admin already there");
		}
	}
}
