package net.fornwall.eclipsecoder.ccsupport;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class CCSupportPlugin extends AbstractUIPlugin {
    public static final String CODE_TEMPLATE_PREFERENCE = "codeTemplatePreference";

    private static CCSupportPlugin instance;

    public static CCSupportPlugin getInstance() {
        return instance;
    }

    public CCSupportPlugin() {
        super();
        instance = this;
    }

    public String getCodeTemplate() {
        return getPreferenceStore().getString(CODE_TEMPLATE_PREFERENCE);
    }

}
