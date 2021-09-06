package org.eclipse.jifa.master.service;

import io.vertx.ext.jdbc.JDBCClient;
import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.master.support.Pivot;

import java.util.HashMap;

public class ServiceCenter {
    protected static JDBCClient dbClient;
    protected static Pivot pivot;
    private static HashMap<String, Object> serviceCenter = new HashMap<>();

    protected static <T> void register(Class<? extends ServiceCenter> klass, T service) {
        if (service.getClass() != klass) {
            throw new JifaException("Insane behavior");
        }
        if (service.getClass().isAssignableFrom(ServiceCenter.class)) {
            throw new JifaException("Parameter \"service\" must subclass of " + ServiceCenter.class.getSimpleName());
        }
        serviceCenter.put(klass.getName(), service);
    }

    public static <T extends ServiceCenter> T lookup(Class<? extends ServiceCenter> klass) {
        return (T) serviceCenter.get(klass.getName());
    }

    public static void initialize(Pivot pivot, JDBCClient dbClient) {
        // Initialize
        ServiceCenter.dbClient = dbClient;
        ServiceCenter.pivot = pivot;

        // Register various services...
        register(JobService.class, new JobService());
        register(AdminService.class, new AdminService());
        register(ConfigService.class, new ConfigService());
        register(FileService.class, new FileService());
        register(WorkerService.class, new WorkerService());
    }
}
