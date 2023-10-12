---
title: Eclipse Jifa
titleTemplate: :title
layout: home

hero:
  name: "Eclipse Jifa"
  text:
  tagline: "一个致力于帮助 Java 研发人员排查应用中常见问题的开源项目"
  actions:
    - theme: brand
      text: 快速上手
      link: /zh/guide/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/eclipse/jifa

features:
  - title: 堆快照分析
    icon: 🔬
    details: 基于 Eclipse Memory Analyzer。支持内存泄漏检测、支配树、对象查询语言（OQL/Calcite SQL）等常用功能视图。典型场景：OOM、Full GC。
  - title: GC 日志分析
    icon: 📊
    details: 支持 GC 问题诊断、原因分析、JVM 选项调优、性能对比等功能视图，支持多种 GC 算法，如 G1、CMS、ZGC 等。典型场景：长时间暂停、RT 不稳定。
  - title: 线程快照分析
    icon: 🔒
    details: 支持线程和线程池分析、锁分析、调用栈聚合等功能视图。典型场景：线程泄漏、死锁。
---

