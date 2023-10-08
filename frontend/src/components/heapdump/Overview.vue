<!--
    Copyright (c) 2023 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<script setup lang="ts">
import { useAnalysisApiRequester } from '@/composables/analysis-api-requester';
import { hdt } from '@/components/heapdump/utils';
import { prettyCount, prettySize, prettyDate } from '@/support/utils';
import { a2rgb, PIE_COLORS, REMAINDER_COLOR } from '@/components/heapdump/color-helper';
import { ArcElement, Chart, Title, Tooltip } from 'chart.js';
import { Doughnut } from 'vue-chartjs';
import { useSelectedObject } from '@/composables/heapdump/selected-object';
import { currentLocale } from '@/i18n/i18n';
import { isDark } from '@/composables/theme';
import CommonContextMenu from '@/components/common/CommonContextMenu.vue';
import { commonMenu as menu } from '@/components/heapdump/menu';

const { request } = useAnalysisApiRequester();

const information = ref([]);
let usedHeapSize = 0;
const contextmenu = ref();
const loading1 = ref(true);

request('details').then((data) => {
  usedHeapSize = data.usedHeapSize;

  let keys = [
    'usedHeapSize',
    'numberOfClasses',
    'numberOfObjects',
    'numberOfClassLoaders',
    'numberOfGCRoots',
    'creationDate',
    'identifierSize'
  ];
  let formatters = [
    prettySize,
    prettyCount,
    prettyCount,
    prettyCount,
    prettyCount,
    prettyDate,
    (i) => (i === 8 ? '64 bit' : '32 bit')
  ];
  for (let i = 0; i < keys.length; i++) {
    information.value.push({
      key: computed(() => hdt('overview.' + keys[i])),
      value: formatters[i](data[keys[i]])
    });
  }
  loading1.value = false;
});

let biggestObjects = [];

const chartData = ref();

Chart.register(ArcElement, Title, Tooltip);

function buildChartOptions() {
  return {
    responsive: true,
    maintainAspectRatio: false,
    events: ['contextmenu', 'mousemove', 'mouseout', 'click', 'touchstart', 'touchmove'],
    onClick: (event, elements) => {
      if (elements.length > 0) {
        let object = biggestObjects[elements[0].index];
        let id = object.objectId;
        if (id >= 0) {
          if (event.type === 'click') {
            const { selectedObjectId } = useSelectedObject();
            selectedObjectId.value = id;
          } else if (event.type === 'contextmenu') {
            contextmenu.value.show(event.native, { objectId: id, label: object.label });
          }
        }
      }
    },
    plugins: {
      title: {
        display: true,
        text: hdt('overview.biggestObjectsChartTitle'),
        position: 'top',
        font: { size: '14px' },
        color: isDark.value ? '#CFD3DC' : '#606266'
      },
      tooltip: {
        displayColors: false,
        callbacks: {
          label: (context) => {
            let value = context.parsed;
            if (usedHeapSize) {
              return (
                prettySize(value) + ' - ' + ((value / usedHeapSize) * 100).toPrecision(3) + '%'
              );
            }
            return prettySize(value);
          }
        }
      }
    }
  };
}

const chartOptions = ref(buildChartOptions());

watch([currentLocale, isDark], () => {
  chartOptions.value = buildChartOptions();
});

const loading2 = ref(true);
request('biggestObjects').then((data) => {
  biggestObjects = data;
  let labels = [];
  let values = [];
  for (let i = 0; i < data.length; i++) {
    labels.push(data[i].label);
    values.push(data[i].value);
  }

  let color = [];
  for (let i = 0; i < data.length - 1; i++) {
    color.push(a2rgb(PIE_COLORS[i % PIE_COLORS.length]));
  }

  if (data[data.length - 1].objectId === -1 || data[data.length - 1].label === 'Remainder') {
    color.push(a2rgb(REMAINDER_COLOR));
  } else {
    color.push(a2rgb(PIE_COLORS[(biggestObjects.length - 1) % PIE_COLORS.length]));
  }

  chartData.value = {
    labels: labels,
    datasets: [
      {
        backgroundColor: color,
        data: data
      }
    ]
  };
  loading2.value = false;
});
</script>
<template>
  <el-scrollbar>
    <CommonContextMenu :menu="menu" ref="contextmenu" />

    <div style="display: flex; flex-direction: column">
      <el-table :data="information" :show-header="false" stripe size="large" v-loading="loading1">
        <el-table-column prop="key" width="300px"></el-table-column>
        <el-table-column prop="value"></el-table-column>
      </el-table>

      <div style="margin-top: 10px; height: 495px" v-loading="loading2">
        <Doughnut
          ref="c"
          :data="chartData"
          :options="chartOptions"
          v-if="chartData"
          @contextmenu="(e) => e.preventDefault()"
        ></Doughnut>
      </div>
    </div>
  </el-scrollbar>
</template>
