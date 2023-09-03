/********************************************************************************
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jifa.tda.parser;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParserFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserFactory.class);

    private static final Parser DEFAULT = new SerDesParser(new JStackParser());

    private static final List<Parser> SPECIFIC_PARSERS = Arrays.asList(new SerDesParser(new ThreadMXBeanParser()));

    public static Parser buildParser(Path path) {

        Parser parser = SPECIFIC_PARSERS.stream().filter(p -> p.canParse(path)).findFirst().orElse(DEFAULT);
        LOGGER.debug("Selecting {} parser for path {}",parser.getClass().getSimpleName(),path);
        return parser;
    }
}
