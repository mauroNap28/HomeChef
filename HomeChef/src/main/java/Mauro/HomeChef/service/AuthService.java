package Mauro.HomeChef.service;

import Mauro.HomeChef.config.JwtService;
import Mauro.HomeChef.dto.Enum.Role;
import Mauro.HomeChef.dto.Requests.AuthenticationRequest;
import Mauro.HomeChef.dto.Requests.RegisterRequest;
import Mauro.HomeChef.dto.Responses.AuthenticationResponse;
import Mauro.HomeChef.model.AttivazioneAccount;
import Mauro.HomeChef.model.User;
import Mauro.HomeChef.repository.AttivazioneAccountRepository;
import Mauro.HomeChef.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class AuthService {

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    EmailService emailService;

    @Autowired
    AttivazioneAccountRepository attivazioneAccountRepository;

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        if (userRepository.findByUsername(authenticationRequest.getUsername()).getEmailVerificata()) {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getUsername(),
                    authenticationRequest.getPassword()
                )
            );
            var user = userRepository.findByUsername(authenticationRequest.getUsername());
            var jwtToken = jwtService.generateToken(user, authenticationRequest.getRememberMe());
            return AuthenticationResponse
                .builder()
                .token(jwtToken)
                .build();
        } else
            throw new RuntimeException("Account non abilitato, puoi abilitarlo tramite il link che ti è stato inviato per email.");
    }

    public void register(RegisterRequest registerRequest) {
        User user = userRepository.findByUsername(registerRequest.getUsername());
        if (Objects.isNull(user)) {
            user = User.builder()
                .username(registerRequest.getUsername())
                .password(bCryptPasswordEncoder.encode(registerRequest.getPassword()))
                .dataIscrizione(LocalDateTime.now())
                .emailVerificata(false)
                .role(Objects.nonNull(registerRequest.getRole()) ? registerRequest.getRole() : Role.USER)
                .build();
            userRepository.save(user);
            String linkAttivazione = generaLink(user);
            emailService.sendEmail(
                registerRequest.getUsername(),
                "Conferma registrazione",
                "Benvenuto su HomeChef! Per favore conferma la tua iscrizione cliccando sul seguente link: \n" + linkAttivazione);
        } else
            throw new RuntimeException("Utente già iscritto.");
    }

    public boolean validitaToken(String token, String username) {
        UserDetails user = userRepository.findByUsername(username);
        return jwtService.isTokenValid(token, user);
    }

    public User cambioPassword(String username, String nuovaPassword) {
        User user = userRepository.findByUsername(username);
        if (Objects.nonNull(user) && !bCryptPasswordEncoder.matches(nuovaPassword, user.getPassword())) {
            user.setPassword(bCryptPasswordEncoder.encode(nuovaPassword));
            return userRepository.save(user);
        }
        throw new RuntimeException("Errore nel cambio della password");
    }

    public String generaLink(User user) {
        String codice = RandomStringUtils.randomAlphanumeric(8);
        attivazioneAccountRepository.save(AttivazioneAccount.builder()
            .codiceDiSicurezza(codice)
            .dataIscrizione(user.getDataIscrizione())
            .user(user)
            .build());
        return "http://localhost:8084/auth/attivazione?codiceDiSicurezza=" + codice;
    }

    public String attivaAccount(String codiceDiSicurezza) {
        AttivazioneAccount attivazioneAccount = attivazioneAccountRepository.findByCodiceDiSicurezza(codiceDiSicurezza);
        if (Objects.nonNull(attivazioneAccount)) {
            User user = userRepository.findById(attivazioneAccount.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Account non trovato in piattaforma."));
            user.setEmailVerificata(true);
            userRepository.save(user);
            attivazioneAccountRepository.delete(attivazioneAccount);
            return "Account attivato!";
        } else
            throw new RuntimeException("Codice di attivazione scaduto");
    }

}
