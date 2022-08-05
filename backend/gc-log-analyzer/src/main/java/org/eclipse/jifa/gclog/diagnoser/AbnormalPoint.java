package org.eclipse.jifa.gclog.diagnoser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.model.TimedEvent;
import org.eclipse.jifa.gclog.util.I18nStringView;

import java.util.Comparator;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbnormalPoint {
    private AbnormalType type;
    private TimedEvent site;
    private AbnormalSeverity severity;

    public static final Comparator<AbnormalPoint> compareByImportance = (ab1, ab2) -> {
        if (ab1.severity != ab2.severity) {
            return ab2.severity.ordinal() - ab1.severity.ordinal();
        } else if (ab1.type != ab2.type) {
            return ab1.type.getOrdinal() - ab2.type.getOrdinal();
        }
        return 0;
    };

    public AbnormalPoint cloneExceptSite() {
        return new AbnormalPoint(type, new TimedEvent(site.getStartTime(), site.getDuration()), severity);
    }

    // When we can not find the exact reason of the problem, just tell the user
    // the general way to deal with it.
    public List<I18nStringView> generateDefaultSuggestions(GCModel model) {
        return new DefaultSuggestionGenerator(model, this).generate();
    }
}
