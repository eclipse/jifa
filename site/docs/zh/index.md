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
    details: 基于 Eclipse Memory Analyzer。支持内存泄漏检测、支配树、对象查询语言（OQL/Calcite SQL）等常用功能视图。帮助排查 OOM、Full GC 等问题。
  - title: GC 日志分析
    link: ./guide/gc-log-analysis
    icon: 📊
    details: 支持 GC 问题诊断、原因分析、JVM 选项调优、性能对比等功能视图，支持多种 GC 算法，如 G1、CMS、ZGC 等。帮助排查长时间暂停、RT 不稳定等问题。
  - title: 线程快照分析
    link: ./guide/thread-dump-analysis
    icon: 🔒
    details: 支持线程和线程池分析、锁分析、调用栈聚合等功能视图。帮助排查线程泄漏、死锁等问题。
---

