// package buy01.user.config.Helpers;

// import java.util.ArrayList;
// import java.util.List;

// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Component;

// import buy01.user.model.Auth.userEntity;
// import buy01.user.repository.Auth.userRepository;

// @Component
// public class UserDetailsServiceImpl implements UserDetailsService {
//     private final userRepository userRepository;

//     public UserDetailsServiceImpl(userRepository userRepository) {
//         this.userRepository = userRepository;
//     }

//     @Override
//     public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//         userEntity user = userRepository.findByEmail(email);

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

