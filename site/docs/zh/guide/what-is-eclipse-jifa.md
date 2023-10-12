# Eclipse Jifa 是什么？

## 简介

Eclipse Jifa（后面简称 Jifa）中的 Jifa 由 “**J**ava **I**ssues **F**inding **A**ssistant” 的首字母组成。 项目最初来源于内部的一个在线 Web 系统，该系
统被设计用于提高生产环境 Java 应用的堆泄漏问题排查效率，并逐步支持各类常见问题的辅助排查。

目前在 Jifa 中，主要包含以下核心特性：

- [堆快照分析](./heap-dump-analysis.md)
- [GC 日志分析](./gc-log-analysis.md)
- [线程快照分析](./thread-dump-analysis.md)

在设计与实现上，Jifa 主要包含两部分：

- **服务后台**： 一个标准的 SpringBoot 3 应用。

- **用户界面**：基于 Vue 3 和 Element Plus 等框架实现。

<div class="info custom-block" style="padding-top: 8px">
在 0.1 版本中，<b>服务后台</b>基于 Eclipse Vert.x 实现，<b>用户界面</b>则是基于 Vue 2 和 Element Ui 实现。
</div>

## 使用方式

从目前 Jifa 提供的核心特性看，主要的使用流程包括待分析文件的生成，上传到 Jifa 并进行分析。以 Java 堆快照为例，用户可以通过以下步骤开始一次分析：

- 获取堆快照文件。用户可以通过 `jmap` 或 `jcmd` 等命令生成一个新的快照文件。

::: code-group

```sh [jamp]
$ jmap -dump:format=b,file=<file> <pid>
```

```sh [jcmd]
$ jcmd <pid> GC.heap_dump filename=<file>
```
:::

- 上传堆快照文件到 Jifa。目前支持多种上传方式，如本地文件上传、通过 OSS、S3 等云存储上传。

![Upload](../image/upload.jpeg)

- 在 Jifa 上进行问题分析。上传完成后会进入分析页面。
 
![Upload](../image/heap-dump-analysis-overview.jpeg)

## 部署

用户可以将 Jifa 部署到独立的服务器上，并与内部的运维监控系统集成，让团队内的开发者更方便地使用 Jifa，这也是我们期望的使用方式。

当然，用户也可以在本地部署使用。