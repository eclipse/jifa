---
title: Eclipse Jifa
titleTemplate: :title
layout: home

hero:
  name: "Eclipse Jifa"
  tagline: "让问题排查变得简单"
  actions:
    - theme: brand
      text: 快速上手
      link: /zh/guide/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/eclipse/jifa

features:
  - title: 堆快照分析
    link: ./guide/heap-dump-analysis
    icon: 🔬
    details: 基于 Eclipse Memory Analyzer。功能：泄漏检测、支配关系、对象查询语言（OQL/Calcite SQL）等。可辅助开发者排查 OOM、Full GC 等问题。
  - title: GC 日志分析
    link: ./guide/gc-log-analysis
    icon: 🗑️
    details: 功能：问题诊断、原因分析、选项调优、性能对比等，支持常用的 GC 算法，如 CMS、G1。可辅助开发者排查长时间暂停、RT 不稳定等问题。
  - title: 线程快照分析
    link: ./guide/thread-dump-analysis
    icon: 🔒
    details: 功能：线程与线程池分析、Java monitors 分析、调用栈聚合等。可辅助开发者排查 CPU 高、线程泄漏、死锁等问题。
---

