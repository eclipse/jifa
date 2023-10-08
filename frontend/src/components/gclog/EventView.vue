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
import { useGCLogData } from '@/stores/gc-log-data';
import { prettyTime } from '@/support/utils';
import { formatTimePeriod, getCauseHint, getPhaseHint } from '@/components/gclog/utils';
import { currentLocale, gct, t } from '@/i18n/i18n';

const props = defineProps({
  gcEvent: {
    type: Object,
    default: true
  }
});

const GCLogData = useGCLogData();
const metadata = GCLogData.metadata;

const parts = ref([]);

function append(text) {
  parts.value.push({ text });
}

let gcEvent = props.gcEvent as any;

function dealDatestamp() {
  if (gcEvent.startTime >= 0 && metadata.timestamp >= 0) {
    append(prettyTime(gcEvent.startTime + metadata.timestamp, 'Y-M-D h:m:s') + ' ');
  }
}

function dealUptime() {
  if (gcEvent.startTime > 0) {
    append((gcEvent.startTime / 1000).toFixed(3) + ' ');
  }
}

function dealGCID() {
  if (gcEvent.gcid > 0) {
    append(gcEvent.gcid + ': ');
  }
}

function dealCause() {
  const cause = gcEvent.cause;
  if (cause) {
    append('(');
    parts.value.push({
      text: cause,
      hint: [getCauseHint(cause)],
      ...getPartProblem('badCauseFullGC', cause)
    });
    append(') ');
  }
}

function dealEventType() {
  const type = gcEvent.eventType;
  if (type) {
    parts.value.push({
      text: type,
      hint: [getPhaseHint(type)],
      ...getPartProblem('badEventType', gcEvent.eventType)
    });
    append(' ');
  }
}

function dealSpecialSituation() {
  const dealFunctions = [dealInitialMark, dealPrepareMixed, dealToSpaceExhausted];
  const ps = dealFunctions.map((f) => f()).filter((p) => p);
  if (ps.length > 0) {
    append('(');
    for (let i = 0; i < ps.length; i++) {
      if (i > 0) {
        append(', ');
      }
      parts.value.push(ps[i]);
    }
    append(') ');
  }
}

function dealInitialMark() {
  if (gcEvent.INITIAL_MARK) {
    return {
      text: 'Initial Mark',
      hint: [getPhaseHint('Initial Mark Situation')]
    };
  }
}

function dealPrepareMixed() {
  if (gcEvent.PREPARE_MIXED) {
    return {
      text: 'Prepare Mixed',
      hint: [getPhaseHint('Prepare Mixed Situation')]
    };
  }
}

function dealToSpaceExhausted() {
  if (gcEvent.TO_SPACE_EXHAUSTED) {
    return {
      text: 'To-space Exhausted',
      hint: [getCauseHint('To-space Exhausted')],
      ...getPartProblem('toSpaceExhausted')
    };
  }
}

function dealDuration() {
  if (gcEvent.duration >= 0) {
    append(gct('duration') + ':');
    parts.value.push({
      text: formatTimePeriod(gcEvent.duration),
      ...getPartProblem('badDuration', gcEvent.eventType)
    });
    append(' ');
  }
}

function dealMemory() {
  let firstItemFound = false;
  const generations = ['young', 'old', 'humongous', 'heap', 'archive', 'metaspace'];
  if (gcEvent.memory) {
    generations.forEach((generation) => {
      const item = gcEvent.memory[generation];
      if (item === undefined || (item.preUsed < 0 && item.postUsed < 0 && item.total < 0)) {
        return;
      }
      if (!firstItemFound) {
        append(`[${gct('detail.memory')} `);
      } else {
        append(' ');
      }
      append(gct('generation.' + generation) + ':');
      dealMemoryPreUsed(item.preUsed);
      dealMemoryPreCapacity(item.preCapacity, generation);
      if (item.preUsed >= 0 || item.preCapacity >= 0) {
        append('->');
      }
      dealMemoryPostUsed(item.postUsed, generation);
      dealMemoryPostCapacity(item.postCapacity, generation);
      append(' ');

      firstItemFound = true;
    });
    if (firstItemFound) {
      append('] ');
    }
  }
}

function dealMemoryPreUsed(value) {
  if (value >= 0) {
    parts.value.push({
      text: formatSizeInDetail(value)
    });
  }
}

function dealMemoryPreCapacity(value, generation) {
  if (value >= 0 && (generation !== 'metaspace' || metadata.metaspaceCapacityReliable)) {
    append('(');
    parts.value.push({
      text: formatSizeInDetail(value)
    });
    append(')');
  }
}

function dealMemoryPostUsed(value, generation) {
  if (value >= 0) {
    let problem = '';
    if (generation === 'old') {
      problem = 'highOldUsed';
    } else if (generation === 'humongous') {
      problem = 'highHumongousUsed';
    } else if (generation === 'heap') {
      problem = 'highHeapUsed';
    } else if (generation === 'metaspace') {
      problem = 'highMetaspaceUsed';
    }
    parts.value.push({
      text: formatSizeInDetail(value),
      ...getPartProblem(problem)
    });
  }
}

function dealMemoryPostCapacity(value, generation) {
  if (value >= 0 && (generation !== 'metaspace' || metadata.metaspaceCapacityReliable)) {
    append('(');
    let problem = '';
    if (generation === 'old') {
      problem = 'smallOldGen';
    } else if (generation === 'young') {
      problem = 'smallYoungGen';
    }
    parts.value.push({
      text: formatSizeInDetail(value),
      ...getPartProblem(problem)
    });
    append(')');
  }
}

function dealCPUTime() {
  if (gcEvent.cputime) {
    append(`[${gct('detail.cputime')} `);
    dealUser();
    dealSys();
    dealReal();
    append('] ');
  }
}

function dealUser() {
  append(' User=');
  parts.value.push({
    text: formatTimePeriod(gcEvent.cputime.user),
    ...getPartProblem('badUsr')
  });
}

function dealSys() {
  append(' Sys=');
  parts.value.push({
    text: formatTimePeriod(gcEvent.cputime.sys),
    ...getPartProblem('badSys')
  });
}

function dealReal() {
  append(' Real=');
  const part = {
    text: formatTimePeriod(gcEvent.cputime.real)
  };
  parts.value.push(part);
}

function dealPromotion() {
  if (gcEvent.promotion >= 0) {
    append(gct('timeGraph.promotion') + ':');
    parts.value.push({
      text: formatSizeInDetail(gcEvent.promotion),
      ...getPartProblem('badPromotion', gcEvent.eventType)
    });
    append(' ');
  }
}

function dealInterval() {
  if (gcEvent.interval >= 0) {
    append(gct('detail.interval') + ':');
    parts.value.push({
      text: formatTimePeriod(gcEvent.interval),
      ...getPartProblem('badInterval', gcEvent.eventType)
    });
    append(' ');
  }
}

function dealReclamation() {
  if (gcEvent.reclamation >= 0) {
    append(gct('timeGraph.reclamation') + ':');
    parts.value.push({
      text: formatSizeInDetail(gcEvent.reclamation)
    });
    append(' ');
  }
}

function formatSizeInDetail(size) {
  if (size <= 1024) {
    return 0;
  }
  if (size <= 1024 * 1024) {
    return Math.round(size / 1024) + 'K';
  }
  return Math.round(size / 1024 / 1024) + 'M';
}

function isNormalText(part) {
  return !part.bad && !part.hint;
}

function transformParts() {
  const combined = [];
  parts.value.forEach((part) => {
    if (isNormalText(part) && combined.length > 0 && isNormalText(combined[combined.length - 1])) {
      combined[combined.length - 1].text = combined[combined.length - 1].text + part.text;
    } else {
      combined.push(part);
    }
  });

  parts.value = combined.map((part) => {
    const r: any = {
      text: part.text,
      class: part.bad ? 'danger-metric' : ''
    };
    const hint = [part.hint, part.problemDescription]
      .flat(999)
      .filter((s) => s)
      .map((s) => (s.startsWith('jifa.') ? t(s) : s));
    if (hint.length > 0) {
      r.hints = hint;
    }
    return r;
  });
}

function getPartProblem(problemType, descriptionParam?) {
  if (problemType === '') {
    return;
  }
  const problem = gcEvent.diagnose.find((p) => p.type === problemType);
  if (problem === undefined) {
    return;
  }
  return {
    bad: true,
    problemDescription: gct('diagnose.abnormal.' + problemType, { param: descriptionParam })
  };
}

function deal() {
  parts.value = [];
  dealDatestamp();
  dealUptime();
  dealGCID();
  dealEventType();
  dealCause();
  dealSpecialSituation();
  dealDuration();
  dealMemory();
  dealCPUTime();
  dealPromotion();
  dealReclamation();
  dealInterval();

  transformParts();
}

deal();

watch([currentLocale, () => props.gcEvent], () => {
  gcEvent = props.gcEvent;
  deal();
});
</script>
<template>
  <span>
    <template v-for="part in parts">
      <el-tooltip v-if="part.hints" placement="top-start">
        <template #content>
          <div v-for="line in part.hints" :key="line">
            {{ line }}
          </div>
        </template>
        <template #default>
          <span :class="part.class">{{ part.text }}</span>
        </template>
      </el-tooltip>
      <span v-else :class="part.class">{{ part.text }}</span>
    </template>
  </span>
</template>
<style scoped>
.danger-metric {
  color: var(--el-color-danger);
}
</style>
