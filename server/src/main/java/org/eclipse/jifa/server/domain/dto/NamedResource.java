package org.eclipse.jifa.server.domain.dto;

import org.springframework.core.io.Resource;

public record NamedResource(String name, Resource resource) {
}
