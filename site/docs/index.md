---
layout: home

hero:
  name: "Eclipse Jifa"
  tagline: "An open source project for diagnosing common Java issues"
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: View on GitHub
      link: https://github.com/eclipse/jifa

features:
  - title: Heap Dump Analysis
    icon: 🔬
    details: Providing commonly used views for memory leak detection, dominator tree, Object Query Language (OQL/Calcite SQL), and more. Typical scenarios include OOM and Full GC. Based on Eclipse Memory Analyzer.
  - title: GC Log Analysis
    icon: 📊
    details: Providing GC cause analysis, JVM options tuning, performance comparisons, and more, with support for various GC algorithms such as G1, CMS, ZGC, etc. Typical scenarios include long pause and unstable response time.
  - title: Thread Dump Analysis
    icon: 🔒
    details: Providing thread/thread pool analysis, lock analysis, aggregated call stack views, and more. Typical scenarios include thread leak and deadlock.
---