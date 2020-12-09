package org.eclipse.jifa.worker.route;

import io.vertx.core.Future;
import org.eclipse.jifa.common.vo.DiskUsage;
import org.eclipse.jifa.worker.support.FileSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
class SystemRoute extends BaseRoute {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemRoute.class);

    @RouteMeta(path = "/system/diskUsage")
    void diskUsage(Future<DiskUsage> future) {
        // Should we cache it?
        long totalSpaceInMb = FileSupport.getTotalDiskSpace();
        long usedSpaceInMb = FileSupport.getUsedDiskSpace();
        assert totalSpaceInMb >= usedSpaceInMb;
        LOGGER.info("Disk total {}MB, used {}MB", totalSpaceInMb, usedSpaceInMb);

        future.complete(new DiskUsage(totalSpaceInMb, usedSpaceInMb));
    }
}
