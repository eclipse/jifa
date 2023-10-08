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
interface Title {
  title: string | (() => string);
}

export interface Item extends Title {
  onClick: (d: any) => void;
  followedByDivider?: boolean;
}

export interface SubMenu extends Title {
  items: (Item | SubMenu)[];
  followedByDivider?: boolean;
}

export interface Menu {
  items: (Item | SubMenu)[];
}

export function item(
  title: Title['title'],
  onClick: Item['onClick'],
  followedByDivider = false
): Item {
  return {
    title,
    onClick,
    followedByDivider
  };
}

export function subMenu(
  title: Title['title'],
  items: SubMenu['items'],
  followedByDivider = false
): SubMenu {
  return {
    title,
    items,
    followedByDivider
  };
}

export function menu(items: Menu['items']) {
  return {
    items
  };
}
