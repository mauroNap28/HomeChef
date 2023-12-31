package Mauro.HomeChef.model;

import Mauro.HomeChef.dto.Enum.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private LocalDateTime dataIscrizione;

    private Boolean emailVerificata;

    @Enumerated(EnumType.STRING)
    private Role role;

    @JoinTable
    @ManyToMany(fetch = FetchType.LAZY)
    private List<Ricetta> ricettePreferite;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private AnagraficaUtente anagraficaUtente;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
