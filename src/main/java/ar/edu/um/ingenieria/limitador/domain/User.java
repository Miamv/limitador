package ar.edu.um.ingenieria.limitador.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Data
@Table(name = "users")
public class User implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String email;

	// Nullable para no romper filas existentes (ddl-auto=update agrega la columna
	// sin asignar contraseñas conocidas). WRITE_ONLY: se recibe en POST/PUT pero
	// nunca se serializa en respuestas. Excluido del toString para no filtrarlo en logs.
	@ToString.Exclude
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column
	private String password;

	private Boolean active = Boolean.FALSE;
	public void setActive(Boolean active) {
		this.active = Optional.ofNullable(active).orElse(Boolean.FALSE);
	}

	@JsonIgnore
	@Column(name = "account_non_expired")
	private Boolean accountNonExpired = Boolean.TRUE;


	@JsonIgnore
	@Column(name = "account_non_locked")
	private Boolean accountNonLocked = Boolean.TRUE;


	@JsonIgnore
	@Column(name = "credentials_non_expired")
	private Boolean credentialsNonExpired = Boolean.TRUE;

@JsonIgnore
	@ManyToMany
	@JoinTable(name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();

	@OneToOne
	@JoinColumn(name = "user_data_id")
	private UserData userData;

	// Constructor vacío requerido por JPA
	public User() {
	}

	/**
	 * Proyecta este usuario a una vista inmutable ({@link UserDetail}).
	 *
	 * @throws IllegalStateException si username o email están vacíos (estado inconsistente)
	 */
	public UserDetail toDetail() {
		requireNotBlank(username, "username");
		requireNotBlank(email, "email");
		Set<String> roleNames = new HashSet<>();
		for (Role role : roles) {
			roleNames.add(role.getDescription());
		}
		return new UserDetail(id, username, email, Boolean.TRUE.equals(active), roleNames);
	}

	private static void requireNotBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(field + " must not be blank");
		}
	}

	@JsonIgnore
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		Set<GrantedAuthority> authorities = new HashSet<>();
		for (Role role : roles) {
			authorities.add(new SimpleGrantedAuthority(role.getDescription()));
		}
		return authorities;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonExpired() {
		return accountNonExpired;
	}

	@JsonIgnore
	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	@JsonIgnore
	@Override
	public boolean isCredentialsNonExpired() {
		return credentialsNonExpired;
	}

	@JsonIgnore
	@Override
	public boolean isEnabled() {
		return active;
	}

}