package org.eclipse.jifa.gclog.diagnoser;

import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.event.GCEvent;
import org.eclipse.jifa.gclog.model.GCModel;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jifa.gclog.util.Constant.*;

public class GlobalDiagnoseInfo {
    private GCModel model;
    private AnalysisConfig config; // time range in this config is ignored
    private EventDiagnoseInfo[] eventDiagnoseInfos;

    public GlobalDiagnoseInfo(GCModel model, AnalysisConfig config) {
        this.model = model;
        this.config = config;
        int length = model.getAllEvents().size();
        this.eventDiagnoseInfos = new EventDiagnoseInfo[length];
        for (int i = 0; i < length; i++) {
            eventDiagnoseInfos[i] = new EventDiagnoseInfo();
        }
    }

    public GCModel getModel() {
        return model;
    }

    public AnalysisConfig getConfig() {
        return config;
    }

    public EventDiagnoseInfo[] getEventDiagnoseInfos() {
        return eventDiagnoseInfos;
    }

    public EventDiagnoseInfo getEventDiagnoseInfo(GCEvent event) {
        int id = event.getId();
        if (id == UNKNOWN_INT) {
            ErrorUtil.shouldNotReachHere();
        }
        return eventDiagnoseInfos[id];
    }

    public List<AbnormalPoint.AbnormalPointVO> getEventDiagnoseVO(GCEvent event) {
        List<AbnormalPoint.AbnormalPointVO> result = new ArrayList<>();
        EventDiagnoseInfo eventDiagnose = getEventDiagnoseInfo(event);
        if (eventDiagnose == null) {
            return result;
        }
        eventDiagnose.getAbnormals().iterate(ab -> result.add(ab.toVO()));
        return result;
    }

    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        for (GCEvent event : model.getGcEvents()) {
            eventToDebugString(event, sb);
            event.phasesDoDFS(phase -> {
                sb.append("    ");
                eventToDebugString(phase, sb);
            });
        }
        return sb.toString();
    }

    private void eventToDebugString(GCEvent event, StringBuilder sb) {
        sb.append(event.toDebugString(model)).append(" [");
        getEventDiagnoseInfo(event).getAbnormals().iterate(ab ->sb.append(ab).append(", "));
        sb.append("]\n");
    }
}
