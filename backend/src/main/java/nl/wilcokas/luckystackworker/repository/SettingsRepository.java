package nl.wilcokas.luckystackworker.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import nl.wilcokas.luckystackworker.model.Settings;

@Repository
public interface SettingsRepository extends CrudRepository<Settings, Integer> {
}
