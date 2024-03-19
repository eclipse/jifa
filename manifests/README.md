## Deploy jifa by helm

We can use [helm](https://helm.sh/) to quickly deploy jifa.

## Install jifa

```shell
helm install jifa jifa -n jifa --create-namespace
```

Check workloads:

```shell
# kubectl get po -n jifa 
NAME                         READY   STATUS    RESTARTS        AGE
jifa-54d6b4d888-ngr6j        1/1     Running   2 (8m32s ago)   8m45s
jifa-mysql-cfd5dbdb4-2m8bn   1/1     Running   0               8m45s
```

Check service available：Upload an analysis file, check whether the worker starts normally, 
whether the log is normal, and whether the analysis results are obtained on the jifa interface.

```shell
kubectl get po -n jifa -w
NAME                         READY   STATUS    RESTARTS      AGE
jifa-54d6b4d888-ngr6j        1/1     Running   2 (12m ago)   13m
jifa-elastic-worker-1        1/1     Running   0             16s
jifa-mysql-cfd5dbdb4-2m8bn   1/1     Running   0             13m
```

``jifa-elastic-worker-1`` logs:
```shell
kubectl logs -f -n jifa jifa-elastic-worker-1
     ,--.  ,--.  ,------.   ,---.
     |  |  |  |  |  .---'  /  O  \
,--. |  |  |  |  |  `--,  |  .-.  |
|  '-'  /  |  |  |  |`    |  | |  |
 `-----'   `--'  `--'     `--' `--'
Eclipse Jifa 
Powered by Spring Boot 3.1.4

2024-03-19T04:19:15.573Z  INFO 1 --- [           main] org.eclipse.jifa.server.Launcher         : Starting Launcher using Java 17.0.8.1 with PID 1 (/jifa started by root in /)
2024-03-19T04:19:15.579Z  INFO 1 --- [           main] org.eclipse.jifa.server.Launcher         : No active profile set, falling back to 1 default profile: "default"
2024-03-19T04:19:16.693Z  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2024-03-19T04:19:16.917Z  INFO 1 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 215 ms. Found 8 JPA repository interfaces.
2024-03-19T04:19:17.876Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8102 (http)
2024-03-19T04:19:17.886Z  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2024-03-19T04:19:17.886Z  INFO 1 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.13]
2024-03-19T04:19:18.019Z  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2024-03-19T04:19:18.020Z  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 2330 ms
2024-03-19T04:19:18.178Z  INFO 1 --- [           main] o.hibernate.jpa.internal.util.LogHelper  : HHH000204: Processing PersistenceUnitInfo [name: default]
2024-03-19T04:19:18.236Z  INFO 1 --- [           main] org.hibernate.Version                    : HHH000412: Hibernate ORM core version 6.2.9.Final
2024-03-19T04:19:18.238Z  INFO 1 --- [           main] org.hibernate.cfg.Environment            : HHH000406: Using bytecode reflection optimizer
2024-03-19T04:19:18.368Z  INFO 1 --- [           main] o.h.b.i.BytecodeProviderInitiator        : HHH000021: Bytecode provider name : bytebuddy
2024-03-19T04:19:18.511Z  INFO 1 --- [           main] o.s.o.j.p.SpringPersistenceUnitInfo      : No LoadTimeWeaver setup: ignoring JPA class transformer
2024-03-19T04:19:18.531Z  INFO 1 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2024-03-19T04:19:18.695Z  INFO 1 --- [           main] com.zaxxer.hikari.pool.HikariPool        : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@2558f65c
2024-03-19T04:19:18.698Z  INFO 1 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2024-03-19T04:19:18.752Z  WARN 1 --- [           main] org.hibernate.dialect.Dialect            : HHH000511: The 5.6.51 version for [org.hibernate.dialect.MySQLDialect] is no longer supported, hence certain features may not work properly. The minimum supported version is 5.7.0. Check the community dialects project for available legacy versions.
2024-03-19T04:19:19.044Z  INFO 1 --- [           main] o.h.b.i.BytecodeProviderInitiator        : HHH000021: Bytecode provider name : bytebuddy
2024-03-19T04:19:19.799Z  INFO 1 --- [           main] o.h.e.t.j.p.i.JtaPlatformInitiator       : HHH000490: Using JtaPlatform implementation: [org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform]
2024-03-19T04:19:19.926Z  INFO 1 --- [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
2024-03-19T04:19:23.844Z  WARN 1 --- [           main] JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
2024-03-19T04:19:24.408Z  INFO 1 --- [           main] o.s.s.web.DefaultSecurityFilterChain     : Will secure any request with [org.springframework.security.web.session.DisableEncodeUrlFilter@18e0c618, org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter@32002a21, org.springframework.security.web.context.SecurityContextHolderFilter@c1aa8f9, org.springframework.security.web.header.HeaderWriterFilter@3106efb9, org.springframework.web.filter.CorsFilter@b13c600, org.springframework.security.web.authentication.logout.LogoutFilter@30090808, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter@476fc8a1, org.springframework.security.web.savedrequest.RequestCacheAwareFilter@498e9130, org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter@7b779c7b, org.springframework.security.web.authentication.AnonymousAuthenticationFilter@7ae5231c, org.springframework.security.web.session.SessionManagementFilter@54ba7366, org.springframework.security.web.access.ExceptionTranslationFilter@4acbe932, org.springframework.security.web.access.intercept.AuthorizationFilter@34a7decd]
2024-03-19T04:19:24.737Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8102 (http) with context path ''
2024-03-19T04:19:24.752Z  INFO 1 --- [           main] org.eclipse.jifa.server.Launcher         : Started Launcher in 9.805 seconds (process running for 10.276)
2024-03-19T04:19:26.470Z  INFO 1 --- [nio-8102-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2024-03-19T04:19:26.470Z  INFO 1 --- [nio-8102-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2024-03-19T04:19:26.471Z  INFO 1 --- [nio-8102-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 1 ms
```

  Successful installation!