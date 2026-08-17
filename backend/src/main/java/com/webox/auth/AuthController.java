package com.webox.auth;

import com.webox.api.ApiException;
import com.webox.model.Role;
import com.webox.model.User;
import com.webox.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUsers;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager, CurrentUserService currentUsers) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.currentUsers = currentUsers;
    }

    public record Credentials(
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number.")
            String password) {}
    public record UserResponse(Long id, String email, Role role) {}

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    UserResponse register(@Valid @RequestBody Credentials credentials, HttpServletRequest request) {
        var email = credentials.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account already exists for this email.");
        }
        users.save(new User(email, passwordEncoder.encode(credentials.password()), Role.EMPLOYEE));
        return authenticate(credentials, request);
    }

    @PostMapping("/login")
    UserResponse login(@Valid @RequestBody Credentials credentials, HttpServletRequest request) {
        return authenticate(credentials, request);
    }

    private UserResponse authenticate(Credentials credentials, HttpServletRequest request) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(credentials.email().trim().toLowerCase(), credentials.password()));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    @GetMapping("/me")
    UserResponse me(Authentication authentication) {
        var user = currentUsers.require(authentication);
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }

    @PostMapping("/logout")
    void logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
