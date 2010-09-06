package net.fornwall.eclipsecoder.ccsupport;

import java.util.ArrayList;
import java.util.List;

import net.fornwall.eclipsecoder.util.AbstractLauncher;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.ICDebugConfiguration;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.debug.ui.ICDebuggerPage;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Launcher to launch the first binary found in a c project by creating a new
 * launch configuration.
 * 
 * See CDT:s CApplicationLauncher class from which most of this is copied.
 */
public class CBinaryLauncher extends AbstractLauncher {

	private IBinary bin;

	private IBinary findFirstBinary(ICProject cProject) throws CModelException {
		for (IBinary binary : cProject.getBinaryContainer().getBinaries()) {
			if (binary.isExecutable()) {
				return binary;
			}
		}
		return null;
	}

	/** Note that this must be called after build since it scans for binaries. */
	public CBinaryLauncher(ICProject cProject) {
		try {
			bin = findFirstBinary(cProject);
			if (bin == null) {
				cProject.getProject().refreshLocal(IResource.DEPTH_ONE, Utilities.NULL_MONITOR);
				bin = findFirstBinary(cProject);
				if (bin != null) {
					Utilities.showMessageDialog("Binary found", "Found binary after refreshLocal()");
				} else {
					Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
					Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, Utilities.NULL_MONITOR);
					bin = findFirstBinary(cProject);
					if (bin != null) {
						Utilities.showMessageDialog("Binary found", "Found binary after join()");
					}
				}
			}
			if (bin == null) {
				throw new RuntimeException("Cannot find binary");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ILaunchConfigurationType getCLaunchConfigType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
				ICDTLaunchConfigurationConstants.ID_LAUNCH_C_APP);
	}

	private ICDebugConfiguration getDebugConfig() {
		ICDebugConfiguration debugConfig = null;
		IProject project = bin.getResource().getProject();
		ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(project);
		ICConfigurationDescription configDesc = projDesc.getActiveConfiguration();
		String configId = configDesc.getId();
		ICDebugConfiguration[] debugConfigs = CDebugCorePlugin.getDefault().getActiveDebugConfigurations();
		int matchLength = 0;
		for (int i = 0; i < debugConfigs.length; ++i) {
			ICDebugConfiguration dc = debugConfigs[i];
			String[] patterns = dc.getSupportedBuildConfigPatterns();
			if (patterns != null) {
				for (int j = 0; j < patterns.length; ++j) {
					if (patterns[j].length() > matchLength && configId.matches(patterns[j])) {
						debugConfig = dc;
						matchLength = patterns[j].length();
					}
				}
			}
		}

		if (debugConfig == null) {
			// Prompt the user if more then 1 debugger.
			String programCPU = bin.getCPU();
			String os = Platform.getOS();
			debugConfigs = CDebugCorePlugin.getDefault().getActiveDebugConfigurations();
			List<ICDebugConfiguration> debugList = new ArrayList<ICDebugConfiguration>(debugConfigs.length);
			for (int i = 0; i < debugConfigs.length; i++) {
				String platform = debugConfigs[i].getPlatform();
				if (debugConfigs[i].supportsMode(ICDTLaunchConfigurationConstants.DEBUGGER_MODE_RUN)) {
					if (platform.equals("*") || platform.equals(os)) { //$NON-NLS-1$
						if (debugConfigs[i].supportsCPU(programCPU))
							debugList.add(debugConfigs[i]);
					}
				}
			}
			debugConfigs = debugList.toArray(new ICDebugConfiguration[0]);
			if (debugConfigs.length >= 1) {
				debugConfig = debugConfigs[0];
			} else if (debugConfigs.length > 1) {
				// TODO: keep this?
				// debugConfig = chooseDebugConfig(debugConfigs, mode);
			}
		}
		return debugConfig;
	}

	@Override
	protected String getLauncherName() {
		return bin.getElementName();
	}

	@Override
	protected String getLauncherTypeId() {
		return ICDTLaunchConfigurationConstants.ID_LAUNCH_C_APP;
	}

	@Override
	protected void setUpConfiguration(ILaunchConfigurationWorkingCopy wc) throws Exception {
		String projectName = bin.getResource().getProjectRelativePath().toString();
		ICDebugConfiguration debugConfig = getDebugConfig();

		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, projectName);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, bin.getCProject().getElementName());
		wc.setMappedResources(new IResource[] { bin.getResource(), bin.getResource().getProject() });
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String) null);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, true);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE,
				ICDTLaunchConfigurationConstants.DEBUGGER_MODE_RUN);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_ID, debugConfig.getID());

		ICProjectDescription projDes = CCorePlugin.getDefault().getProjectDescription(bin.getCProject().getProject());
		if (projDes != null) {
			String buildConfigID = projDes.getActiveConfiguration().getId();
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, buildConfigID);
		}

		// Load up the debugger page to set the defaults. There should
		// probably be a separate
		// extension point for this.
		ICDebuggerPage page = CDebugUIPlugin.getDefault().getDebuggerPage(debugConfig.getID());
		page.setDefaults(wc);
	}

}
