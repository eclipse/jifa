## Customizing JIFA

Some options are provided to configure JIFA without modification to the source code.

### Frontend configuration

Frontend can be configured by modifying the `config.js` file provided in the application webroot, in the same
location as the `index.html` file.

A [sample config.js file](frontend/public/config.js) is provided with JIFA which you can edit.

### Backend customization

#### Configuration options

A configuration file can be specified as an environment variable.

```
export WORKER_OPTS=-Djifa.worker.config=/path/to/worker-config.json
./bin/worker
...
```

A sample configuration file is here:
```
{
  "server.host": "0.0.0.0",
  "server.port": 7101,
  "server.uploadDir": "/mnt/data/uploads",
  "api.prefix": "/jifa-api",
  "hooks.className": "com.yourco.JifaHooksImplementation"
}
```

If you choose to provide a configuration file, the `api.prefix` is the only required value. Otherwise,
defaults will apply. If you do not provide a configuration, defaults will apply. For more specific
customization, see next section.

#### Overriding HTTP server, route, and file mapping

The backend can be configured; JIFA has a number of hook points where it will call some code that you provide.

With hooks you can:
- Customize the HTTP server options
- Configure HTTP server routes to add authentication, error handling, health check URLs, etc.
- Customize the layout of heap files on the local file system.

To do so, you need to set configuration to refer to a new class which provides your custom implementations.
You can [provide the implementations by implementing this class](backend/common/src/main/java/org/eclipse/jifa/common/JifaHooks.java)
and then updating configuration file.

In the configuration file, provide a hooks class name to use it and it will be loaded at service startup. See
the `hooks.className` key. The JAR containing your class needs to be present on the classpath. You can use
`export WORKER_OPTS="-Djifa.worker.config=/path/to/config.json -cp /path/to/hook.jar"`.

You will need to extract the `common.jar` from the build process to get access to the JifaHooks interface.
