/********************************************************************************
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import { useEnv } from '@/stores/env';
import { JSEncrypt } from 'jsencrypt/lib/JSEncrypt';

function encrypt(raw: string): string {
  const env = useEnv();

  if (!env.publicKey) {
    return raw;
  }

  let encryptor = new JSEncrypt();
  encryptor.setPublicKey(env.publicKey.pkcs8);
  let result = encryptor.encrypt(raw);
  return result ? result : raw;
}

export function useCipher() {
  return { encrypt };
}
