package org.eclipse.jifa.server.repository;

import org.eclipse.jifa.server.domain.entity.shared.ConfigurationEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ConfigurationRepo extends CrudRepository<ConfigurationEntity, Long>  {

    Optional<ConfigurationEntity> findByUniqueName(String uniqueName);

}
