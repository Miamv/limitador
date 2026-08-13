package ar.edu.um.ingenieria.limitador.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String description;

	@ManyToMany(mappedBy = "roles")
	private Set<User> users = new HashSet<>();

	protected Role() {
	}

	public Role(String description) {
		this.description = description;
	}
	
	//Ejemplo de un método:
	public String getUpperCaseDescription() {
		return description.toUpperCase();
	}
}