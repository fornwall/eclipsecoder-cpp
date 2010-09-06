package net.fornwall.eclipsecoder.ccsupport;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import net.fornwall.eclipsecoder.languages.LanguageSupport;
import net.fornwall.eclipsecoder.stats.CodeGenerator;
import net.fornwall.eclipsecoder.stats.ProblemStatement;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ITargetPlatform;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;

/**
 * The C++ language support class.
 */
public class CCLanguageSupport extends LanguageSupport {

	@Override
	protected CodeGenerator createCodeGenerator(ProblemStatement problemStatement) {
		return new CCCodeGenerator(problemStatement);
	}

	@Override
	public IFile createLanguageProject(IProject project) throws Exception {
		CCorePlugin corePlugin = CCorePlugin.getDefault();

		final ICProject cProject = CoreModel.getDefault().create(project);

		project = corePlugin.createCProject(project.getDescription(), project, null,
				ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);
		corePlugin.convertProjectFromCtoCC(project, null);

		IManagedBuildInfo managedBuildInfo = ManagedBuildManager.createBuildInfo(project);
		ManagedCProjectNature.addManagedNature(project, null);
		ManagedCProjectNature.addManagedBuilder(project, null);

		IProjectType parentProjectType = null;
		IConfiguration parentConfig = null;

		// look at CProjectPlatformPage#populateTypes()
		for (IProjectType type : ManagedBuildManager.getDefinedProjectTypes()) {
			if (!type.isAbstract() && type.isSupported() && !type.isTestProjectType() && type.getId().contains("exe")) {
				// prevent other languages from being used (e.g. fortran and pascal):
				if (!type.getId().contains("cdt."))
					continue;

				for (IConfiguration config : type.getConfigurations()) {
					if (!config.getId().contains("debug")) {
						continue;
					}
					List<String> osList = Arrays.asList(config.getToolChain().getOSList());
					if (osList.contains("all") || osList.contains(Platform.getOS())) {
						List<String> archList = Arrays.asList(config.getToolChain().getArchList());
						if (archList.contains("all") || archList.contains(Platform.getOSArch())) {
							parentProjectType = type;
							parentConfig = config;
						}
					}
				}
			}
		}

		if (parentConfig == null) {
			throw new RuntimeException("Unable to find parent project type - defined project types are: "
					+ Arrays.toString(ManagedBuildManager.getDefinedProjectTypes()));
		}

		IManagedProject newManagedProject = ManagedBuildManager.createManagedProject(project, parentProjectType);

		IConfiguration newConfig = newManagedProject.createConfiguration(parentConfig, parentConfig.getId() + "."
				+ ManagedBuildManager.getRandomNumber());

		newConfig.setArtifactName(newManagedProject.getDefaultArtifactName());
		ManagedBuildManager.setDefaultConfiguration(project, newConfig);
		ManagedBuildManager.setSelectedConfiguration(project, newConfig);
		ManagedBuildManager.setNewProjectVersion(project);

		ICDescriptor projectDescription = corePlugin.getCProjectDescription(project, true);
		projectDescription.create(CCorePlugin.BUILD_SCANNER_INFO_UNIQ_ID, ManagedBuildManager.INTERFACE_IDENTITY);

		// see NewManagedProjectWizard#doRunEpilogue(IProgressMonitor monitor)
		IToolChain toolChain = newConfig.getToolChain();
		ITargetPlatform targetPlatform = toolChain.getTargetPlatform();
		projectDescription.create(CCorePlugin.BINARY_PARSER_UNIQ_ID, targetPlatform.getBinaryParserList()[0]);

		// fixes directory setups for content assist -
		// see NewManagedProjectWizard#doRunEpilogue(IProgressMonitor monitor)
		ManagedBuildManager.initBuildInfoContainer(project);

		IFile testsFile = project.getFile(getProblemStatement().getSolutionClassName() + "Test.cpp");
		testsFile.create(new ByteArrayInputStream(getCodeGenerator().getTestsSource().getBytes()), true, null);

		IFile sourceFile = project.getFile(getSolutionFileName());
		sourceFile.create(new ByteArrayInputStream(getInitialSource().getBytes()), true, null);

		managedBuildInfo.setDirty(true);
		managedBuildInfo.setValid(true);
		ManagedBuildManager.saveBuildInfo(project, true);

		Utilities.buildAndRun(project, new Runnable() {
			public void run() {
				// we want to wait until after build since constructor scans for binaries
				new CBinaryLauncher(cProject).launch();
			}
		});

		return sourceFile;
	}

	@Override
	public String getCodeEditorID() {
		return CUIPlugin.EDITOR_ID;
	}

	@Override
	public String getCodeTemplate() {
		return CCSupportPlugin.getInstance().getCodeTemplate();
	}

	@Override
	public String getLanguageName() {
		return LanguageSupport.LANGUAGE_NAME_CPP;
	}

	@Override
	public String getPerspectiveID() {
		return CUIPlugin.ID_CPERSPECTIVE;
	}

	@Override
	protected String getSolutionFileName() {
		return getProblemStatement().getSolutionClassName() + ".h";
	}

}
