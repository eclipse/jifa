package org.eclipse.jifa.gclog;

import org.eclipse.jifa.gclog.util.DoubleData;
import org.junit.Assert;
import org.junit.Test;

import static org.eclipse.jifa.gclog.util.Constant.EPS;

public class TestGCLogUtil {
    @Test
    public void testDoubleData() {
        DoubleData doubleData = new DoubleData(true);
        doubleData.add(1);
        doubleData.add(2);
        doubleData.add(3);
        doubleData.add(4);
        Assert.assertEquals(doubleData.getPercentile(0.99), 0.03 * 3 + 0.97 * 4, EPS);
        Assert.assertEquals(doubleData.getPercentile(0.75), 0.75 * 3 + 0.25 * 4, EPS);
        doubleData.add(0);
        Assert.assertEquals(doubleData.getMedian(), 2, EPS);
        Assert.assertEquals(doubleData.average(), 2, EPS);
        Assert.assertEquals(doubleData.getMax(), 4, EPS);
        Assert.assertEquals(doubleData.getMin(), 0, EPS);
        Assert.assertEquals(doubleData.getN(), 5, EPS);
    }
}
