package net.fornwall.eclipsecoder.ccsupport;

import net.fornwall.eclipsecoder.stats.CodeGenerator;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public static final String DEFAULT_CODE_TEMPLATE = "#include <algorithm>\n"
            + "#include <iostream>\n" + "#include <map>\n" + "#include <numeric>\n"
            + "#include <set>\n" + "#include <sstream>\n" + "#include <string>\n"
            + "#include <vector>\n" + "using namespace std;\n\n"
            + "#define FOR(i,s,e) for (int i = int(s); i != int(e); i++)\n"
            + "#define FORIT(i,c) for (typeof((c).begin()) i = (c).begin(); i != (c).end(); i++)\n"
            + "#define ISEQ(c) (c).begin(), (c).end()\n\n" + "class "
            + CodeGenerator.TAG_CLASSNAME + " {\n" + "\n" + "\tpublic: "
            + CodeGenerator.TAG_RETURNTYPE + " " + CodeGenerator.TAG_METHODNAME + "("
            + CodeGenerator.TAG_METHODPARAMS + ") {\n" + "\t\treturn "
            + CodeGenerator.TAG_DUMMYRETURN + ";\n" + "\t}\n" + "\n" + "};\n";

    /**
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    @Override
	public void initializeDefaultPreferences() {
        IPreferenceStore store = CCSupportPlugin.getInstance().getPreferenceStore();
        store.setDefault(CCSupportPlugin.CODE_TEMPLATE_PREFERENCE, DEFAULT_CODE_TEMPLATE);
    }

}
