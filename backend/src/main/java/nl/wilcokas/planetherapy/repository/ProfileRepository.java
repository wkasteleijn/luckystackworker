package nl.wilcokas.planetherapy.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import nl.wilcokas.planetherapy.model.Profile;

@Repository
public interface ProfileRepository extends CrudRepository<Profile, Integer> {
	public Optional<Profile> findByName(String name);
}
