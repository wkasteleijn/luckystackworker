package nl.wilcokas.planetherapy.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import nl.wilcokas.planetherapy.model.Settings;

@Repository
public interface SettingsRepository extends CrudRepository<Settings, Integer> {
}
