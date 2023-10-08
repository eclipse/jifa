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
import type { FileType } from '@/composables/file-types';
import { useAnalysisStore } from '@/stores/analysis';
import { Client } from '@stomp/stompjs';
import axios from 'axios';
// @ts-ignore
import { useEnv } from '@/stores/env';
import { v4 as uuidv4 } from 'uuid';

interface Requester {
  request(namespace: string, api: string, target: string, parameters?: object): Promise<any>;

  close(): void;
}

interface ResRej {
  resolve: (value: any) => void;
  reject: (reason?: any) => void;
}

function byAxios(resolve: (req: Requester) => void, reject: (reason: any) => void) {
  resolve({
    request(namespace: string, api: string, target: string, parameters?: object): Promise<any> {
      return axios
        .post('/jifa-api/analysis', {
          namespace,
          api,
          target,
          parameters
        })
        .then((resp) => resp.data);
    },

    close() {}
  });
}

function byStomp(resolve: (req: Requester) => void, reject: (reason: any) => void) {
  const resRejMap = new Map<number, ResRej>();
  const callbackMap = new Map<number, (data: any, error: any) => void>();

  const client = new Client({
    brokerURL: `ws://${location.host}/jifa-stomp`,
    reconnectDelay: 5000
  });

  let subscriptionReceipt = uuidv4();
  let requestId = 1;

  function nextRequestId() {
    return requestId++;
  }

  let token = useEnv().token;
  if (token) {
    client.connectHeaders = {
      'jifa-token': token
    };
  }

  client.onConnect = () => {
    client.subscribe(
      '/ud/analysis',
      (message) => {
        let requestId = Number(message.headers['request-id']);

        if (resRejMap.has(requestId)) {
          let resRej = resRejMap.get(requestId) as ResRej;
          resRejMap.delete(requestId);
          let success = message.headers['response-success'] === 'true';
          let result = message.body ? JSON.parse(message.body) : null;
          if (success) {
            resRej.resolve(result);
          } else {
            resRej.reject(result);
          }
        }

        if (!callbackMap.has(requestId)) {
          return;
        }
        let callback = callbackMap.get(requestId);
        if (callback) {
          callbackMap.delete(requestId);
          let success = message.headers['response-success'] === 'true';
          let result = message.body ? JSON.parse(message.body) : null;
          if (success) {
            callback(result, null);
          } else {
            callback(null, result);
          }
        }
      },
      {
        receipt: subscriptionReceipt
      }
    );

    client.watchForReceipt(subscriptionReceipt, () => {
      resolve({
        request(namespace: string, api: string, target: string, parameters?: object): Promise<any> {
          let id = nextRequestId();
          let p = new Promise((resolve, reject) => {
            resRejMap.set(id, { resolve, reject });
          });
          let body = {
            namespace,
            api,
            target,
            parameters
          };
          client.publish({
            destination: '/ad/analysis',
            headers: { 'request-id': id.toString() },
            body: JSON.stringify(body)
          });
          return p;
        },

        close() {
          client.deactivate();
        }
      });
    });
  };

  client.activate();
}

let rp: Promise<Requester> | undefined;

function request(api: string, parameters?: object) {
  if (!rp) {
    rp = new Promise<Requester>(byAxios);
  }

  return rp.then((requester) => {
    const analysis = useAnalysisStore();
    return requester.request(
      (analysis.fileType as FileType).namespace,
      api,
      analysis.target,
      parameters
    );
  });
}

function requestWithTarget(api: string, type: FileType, target: string, parameters?: object) {
  if (!rp) {
    rp = new Promise<Requester>(byAxios);
  }

  return rp.then((requester) => {
    return requester.request(type.namespace, api, target, parameters);
  });
}

export const useAnalysisApiRequester = () => {
  return {
    request,
    requestWithTarget
  };
};
