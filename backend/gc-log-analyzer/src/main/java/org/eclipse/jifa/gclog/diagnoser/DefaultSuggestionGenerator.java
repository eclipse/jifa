package org.eclipse.jifa.gclog.diagnoser;

import org.eclipse.jifa.common.JifaException;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.gclog.model.GCModel;
import org.eclipse.jifa.gclog.util.I18nStringView;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class DefaultSuggestionGenerator {
    private GCModel model;
    private AbnormalPoint ab;
    private BitSet givenCause = new BitSet();
    private List<I18nStringView> result = new ArrayList<>();

    public DefaultSuggestionGenerator(GCModel model, AbnormalPoint ab) {
        this.model = model;
        this.ab = ab;
    }

    private static Map<AbnormalType, Method> rules = new HashMap<>();

    static {
        initializeRules();
    }

    private static void initializeRules() {
        Method[] methods = DefaultSuggestionGenerator.class.getDeclaredMethods();
        for (Method method : methods) {
            GeneratorRule annotation = method.getAnnotation(GeneratorRule.class);
            if (annotation != null) {
                method.setAccessible(true);
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod) || Modifier.isFinal(mod)) {
                    throw new JifaException("Illegal method modifier: " + method);
                }
                rules.put(AbnormalType.getType(annotation.type()), method);
            }
        }
    }

    public List<I18nStringView> generate() {
        if (ab.getType() == null) {
            return result;
        }
        Method rule = rules.getOrDefault(ab.getType(), null);
        if (rule != null) {
            try {
                rule.invoke(this);
            } catch (Exception e) {
                ErrorUtil.shouldNotReachHere();
            }
        }
        return result;
    }

    private void addSuggestion(SuggestionType type, Object... params) {
        // don't add duplicate suggestions
        if (givenCause.get(type.ordinal())) {
            return;
        }
        givenCause.set(type.ordinal());
        result.add(new I18nStringView(type.toString(), params));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface GeneratorRule {
        String type();
    }
}
