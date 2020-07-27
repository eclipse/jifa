/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.jifa.common.util;

import org.eclipse.jifa.common.Constant;

import java.io.*;

import static org.eclipse.jifa.common.util.ErrorUtil.throwEx;

public class FileUtil {

    public static String content(File f) {
        String result = null;
        try {
            result = content(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throwEx(e);
        }
        return result;
    }

    public static String content(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(Constant.LINE_SEPARATOR);
            }
        } catch (IOException e) {
            throwEx(e);
        }
        return sb.toString();
    }

    public static void write(File f, String msg, boolean append) {
        try (FileWriter fw = new FileWriter(f, append)) {
            fw.write(msg);
        } catch (IOException e) {
            throwEx(e);
        }
    }
}
