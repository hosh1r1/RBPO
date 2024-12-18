package ru.mtuci.rbpopr.controller;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.rbpopr.configuration.JwtTokenProvider;
import ru.mtuci.rbpopr.model.*;
import ru.mtuci.rbpopr.repository.UserRepository;


//TODO: 1. Добавьте в контроллеры больше CRUD операций

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest request) {
        try {
            String email = request.getEmail();

            ApplicationUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));

            String token = jwtTokenProvider.createToken(email, user.getRole().getGrantedAuthorities());

            return ResponseEntity.ok(new AuthenticationResponse(email, token));
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            String email = request.getEmail();

            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This email is already associated with an existing account.");
            }

            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            ApplicationUser user = new ApplicationUser();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(ApplicationRole.USER);
            user.setUsername(request.getUsername());

            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.OK).body("Registration successful.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during registration. Please try again.");
        }
    }
    @PostMapping("/changeEmail")
    public ResponseEntity<?> changeEmail(@RequestHeader("Authorization") String token, @RequestBody ChangeEmailRequest request, HttpServletRequest req) {
        try {
            String newEmail = request.getNewEmail();
            String password = request.getPassword();

            String email = jwtTokenProvider.getUsername(req.getHeader("Authorization").substring(7));

            ApplicationUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            user.setEmail(newEmail);
            userRepository.save(user);

            String newToken = jwtTokenProvider.createToken(newEmail, user.getRole().getGrantedAuthorities());

            return ResponseEntity.ok(new AuthenticationResponse(newEmail, newToken));
        } catch (UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password\"");
        }
    }
}
