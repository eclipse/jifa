import {defineConfig} from 'vitepress'

export default defineConfig({
  base: '/jifa/',

  title: "Eclipse Jifa",
  titleTemplate: ":title · Eclipse Jifa",
  description: "An open source project for diagnosing common Java issues.",

  lastUpdated: true,

  head: [
    ['link', {rel: "icon", href: "/jifa/eclipse_incubation_vertical_svg.svg"}],
  ],

  themeConfig: {
    search: {
      provider: 'local'
    },

    socialLinks: [
      {icon: 'github', link: 'https://github.com/eclipse/jifa'}
    ]
  },

  locales: {
    root: {
      label: 'English',
      lang: 'en',

      themeConfig: {
        nav: [
          {text: 'Home', link: '/'},
          {text: 'Guide', link: '/guide/what-is-eclipse-jifa'}
        ],

        sidebar: [
          {
            text: 'Introduction',
            items: [
              {text: 'What is Eclipse Jifa?', link: '/guide/what-is-eclipse-jifa'},
              {text: 'Getting Started', link: '/guide/getting-started'},
            ]
          },
          {
            text: 'Core Features',
            items: [
              {text: 'Heap Dump Analysis', link: '/guide/heap-dump-analysis'},
              {text: 'GC Log Analysis', link: '/guide/gc-log-analysis'},
              {text: 'Thread Dump Analysis', link: '/guide/thread-dump-analysis'},
            ]
          },
          {
            items: [
              {text: 'Development', link: '/guide/development.md'},
              {text: 'Deployment', link: '/guide/deployment.md'},
              {text: 'Configuration', link: '/guide/configuration.md'},
            ]
          },
          {
            items: [
              {text: 'Contribution', link: '/guide/contribution.md'},
            ]
          }
        ],

        editLink: {
          text: 'Edit this page on GitHub',
          pattern: 'https://github.com/eclipse/jifa/edit/main/site/docs/:path'
        },

        lastUpdated: {
          text: 'Updated at',
          formatOptions: {
            dateStyle: 'short',
            timeStyle: 'short',
            forceLocale: true
          }
        }
      },
    },

    zh: {
      label: '简体中文',
      lang: 'zh',

      themeConfig: {
        nav: [
          {text: '主页', link: '/zh/'},
          {text: '指南', link: '/zh/guide/what-is-eclipse-jifa'}
        ],

        sidebar: [
          {
            text: '介绍',
            items: [
              {text: 'Eclipse Jifa 是什么？', link: '/zh/guide/what-is-eclipse-jifa'},
              {text: '快速开始', link: '/zh/guide/getting-started'},
            ]
          },
          {
            text: '核心特性',
            items: [
              {text: '堆内存分析', link: '/zh/guide/heap-dump-analysis'},
              {text: 'GC 日志分析', link: '/zh/guide/gc-log-analysis'},
              {text: '线程快照分析', link: '/zh/guide/thread-dump-analysis'},
            ]
          },
          {
            items: [
              {text: '开发', link: '/zh/guide/development.md'},
              {text: '部署', link: '/zh/guide/deployment.md'},
              {text: '配置', link: '/zh/guide/configuration.md'},
            ]
          },
          {
            items: [
              {text: '贡献', link: '/zh/guide/contribution.md'},
            ]
          }
        ],

        editLink: {
          text: '在 GitHub 上编辑此页',
          pattern: 'https://github.com/eclipse/jifa/edit/main/site/docs/:path'
        },

        lastUpdated: {
          text: '更新于',
          formatOptions: {
            dateStyle: 'short',
            timeStyle: 'short',
            forceLocale: true
          }
        }
      },
    }
  },
})
