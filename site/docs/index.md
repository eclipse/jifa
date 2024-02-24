---
title: Eclipse Jifa
titleTemplate: :title
layout: home

hero:
  name: "Eclipse Jifa"
  tagline: "Make Troubleshooting Easy"
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: Playground
      link: https://jifa.dragonwell-jdk.io
    - theme: alt
      text: View on GitHub
      link: https://github.com/eclipse/jifa

features:
  - title: Heap Dump Analysis
    link: ./guide/heap-dump-analysis
    icon: 🔬
    details: "Based on Eclipse Memory Analyzer. Features: Leak Detection, Dominator Tree, Object Query Language (OQL/Calcite SQL), etc." 
  - title: GC Log Analysis
    link: ./guide/gc-log-analysis
    icon: 🗑️
    details: "Features: Cause Analysis, Options Tuning, Comparison, etc. Supports commonly used algorithms such as CMS and G1."
  - title: Thread Dump Analysis
    link: ./guide/thread-dump-analysis
    icon: 🔒
    details: "Features: Thread & Thread Pool Analysis, Java Monitors Analysis, Aggregated Stack Trace Views, etc."
  - title: JFR Analysis
    link: ./guide/jfr-analysis
    icon: 🧬
    details: "Features: Parse JFR files and generate hotspot views for CPU, Memory Allocation, Lock, File IO, Socket IO, Wall Clock and other dimensions. Can help locate various application performance issues."
---