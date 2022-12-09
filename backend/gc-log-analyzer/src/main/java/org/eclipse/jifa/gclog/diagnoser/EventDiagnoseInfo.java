package org.eclipse.jifa.gclog.diagnoser;

import lombok.Data;

@Data
public class EventDiagnoseInfo {
    private EventAbnormalSet abnormals = new EventAbnormalSet();

    public EventDiagnoseInfo() {
    }
}
