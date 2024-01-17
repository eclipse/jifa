/********************************************************************************
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.server.repository;

import org.eclipse.jifa.server.domain.entity.shared.ConfigurationEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ConfigurationRepo extends CrudRepository<ConfigurationEntity, Long>  {

    Optional<ConfigurationEntity> findByUniqueName(String uniqueName);

}
