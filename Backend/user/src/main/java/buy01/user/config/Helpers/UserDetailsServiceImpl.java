// package buy01.user.config.Helpers;

// import java.util.ArrayList;
// import java.util.List;

// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Component;

// import buy01.user.model.Auth.UserEntity;
// import buy01.user.repository.Auth.UserRepository;

// @Component
// public class UserDetailsServiceImpl implements UserDetailsService {
//     private final UserRepository UserRepository;

//     public UserDetailsServiceImpl(UserRepository UserRepository) {
//         this.UserRepository = UserRepository;
//     }

//     @Override
//     public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//         UserEntity user = UserRepository.findByEmail(email);

//         if (user == null) {
//             throw new UsernameNotFoundException("User not found with email: " + email);
//         }
//         List<GrantedAuthority> authorities = new ArrayList<>();
//         if (user.getRole() != null) {
//             authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
//         }
//          return new org.springframework.security.core.userdetails.User(
//                 user.getEmail(),
//                 user.getPassword(),
//                 authorities
//         );
//     }
// }

