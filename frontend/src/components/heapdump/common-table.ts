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
import type { Menu } from '@/composables/contextmenu';

export interface Column {
  label: string | (() => string);
  width?: number | string;
  minWidth?: number;
  widthExpansionLimit?: number;
  align?: 'left' | 'center' | 'right';
  fix?: 'left' | 'right' | true;
  sortable?: boolean;

  property?: string;
  icon?: ((item: Item) => string) | string;
  prefix?: (item: Item) => string;
  content?: (item: Item) => string;
  suffix?: (item: Item) => string;

  contentOfSummary?: ((item: Item) => string) | string;
}

export interface Api {
  api: string;
  parameters?: (item?: Item) => object;
  paged?: boolean;

  respMapper?: (resp: any) => object;

  summaryApi?: string;
  parametersOfSummaryApi?: (item?: Item) => object;
}

export interface TableProperty {
  columns: Column[];

  apis: Api[];

  columnAdjuster?: (d: object) => boolean;

  hasChildren?: (d: object) => boolean;

  defaultSortProperty?: object;

  sortParameterConverter?: (sortProperty: any) => object;

  onRowClick?: (item: Item) => void;

  watch?: any[];

  stripe?: boolean;

  showOverflowTooltip?: boolean;

  dataFilter?: (item: any) => boolean;

  spanMethod?: (cell: any) => number[];

  menu?: Menu;

  hasMenu?: (d: object) => boolean;

  menuDataConverter?: (d: object) => object;
}

export interface Pagination {
  next: number;
  size: number;
  total: number | undefined;
}

export interface ItemMeta {
  rowKey: number;

  tier: number;
  parent?: Item | undefined;

  index?: number;
  hasChildren?: boolean;

  pagination?: Pagination | undefined;
  summary?: boolean;
}

export interface Item {
  __meta: ItemMeta;

  __hasChildren?: boolean;
}

export interface PageView {
  totalSize: number;
  data: [];
}
