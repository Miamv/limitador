package ar.edu.um.ingenieria.limitador.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String description;

	@JsonIgnore
	@ManyToMany(mappedBy = "roles")
	private Set<User> users = new HashSet<>();


	public Role(String description) {
		this.description = description;
	}
	
	//Ejemplo de un método:
	public String getUpperCaseDescription() {
		return description.toUpperCase();
	}
}