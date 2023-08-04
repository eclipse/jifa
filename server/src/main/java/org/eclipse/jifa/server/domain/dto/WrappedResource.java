package org.eclipse.jifa.server.domain.dto;

import org.springframework.core.io.Resource;

public record WrappedResource(String name, Resource resource) {
}
