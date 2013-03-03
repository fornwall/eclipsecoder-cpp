package net.fornwall.eclipsecoder.ccsupport;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class CCPreferencesPage implements IWorkbenchPreferencePage {

	private Text codeTemplateEditor;

	private Label codeTemplateLabel;

	private Label toolChainLabel;

	private CCombo toolChainCombo;

	private ArrayList<String> toolChainId;

	private Composite composite;

	public Point computeSize() {
		return composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}


	private void initializeToolChainCombo() {
		// TODO: duplicated with CCLanguageSupport.java, maybe to extract as separate function is preferred
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
							toolChainCombo.add(config.getToolChain().getName());
							toolChainId.add(config.getId());
							if(config.getId().equals(CCSupportPlugin.getInstance().getToolchain())) {
								toolChainCombo.select(toolChainCombo.getItemCount()-1);
							}
						}
					}
				}
			}
		}
	}

	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		codeTemplateLabel = new Label(composite, SWT.NONE);
		codeTemplateLabel.setText("Code template:");

		codeTemplateEditor = new Text(composite, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);
		codeTemplateEditor.setFont(JFaceResources
				.getFont(JFaceResources.TEXT_FONT));
		codeTemplateEditor.setTabs(5);
		codeTemplateEditor.setText(CCSupportPlugin.getInstance()
				.getCodeTemplate());
		codeTemplateEditor.setLayoutData(new GridData(GridData.FILL_BOTH));

		toolChainLabel = new Label(composite, SWT.NONE);
		toolChainLabel.setText("Toolchain:");

		toolChainCombo = new CCombo(composite, SWT.READ_ONLY | SWT.BORDER);
		toolChainId = new ArrayList<String>();
		initializeToolChainCombo();
	}

	public void dispose() {
		composite.dispose();
		// children of composite disposed as well? (probably)
		// codeTemplateEditor.dispose();
		// codeTemplateEditor.dispose();
	}

	public Control getControl() {
		return composite; // codeTemplateEditor;
	}

	public String getDescription() {
		return getTitle();
	}

	public String getErrorMessage() {
		return null;
	}

	public Image getImage() {
		return null;
	}

	public String getMessage() {
		return null;
	}

	public String getTitle() {
		return "EclipseCoder C++ Preferences";
	}

	public void init(IWorkbench workbench) {
		// do nothing
	}

	public boolean isValid() {
		return true;
	}

	public boolean okToLeave() {
		return true;
	}

	public boolean performCancel() {
		return true;
	}

	public void performHelp() {
		// do nothing
	}

	public boolean performOk() {
		CCSupportPlugin.getInstance().getPreferenceStore().setValue(
				CCSupportPlugin.CODE_TEMPLATE_PREFERENCE,
				codeTemplateEditor.getText());
		if(toolChainCombo.getSelectionIndex() < 0) {
			MessageBox box = new MessageBox(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OK);
			box.setText("EclipseCoder - C++ configuration");
			box.setMessage("You need to specify a toolchain.");
			box.open();
			return false;
		}
		CCSupportPlugin.getInstance().getPreferenceStore().setValue(
				CCSupportPlugin.TOOLCHAIN_PREFERENCE,
				toolChainId.get(toolChainCombo.getSelectionIndex()));
		return true;
	}

	public void setContainer(IPreferencePageContainer preferencePageContainer) {
		// do nothing
	}

	public void setDescription(String description) {
		// do nothing
	}

	public void setImageDescriptor(ImageDescriptor image) {
		// do nothing
	}

	public void setSize(Point size) {
		composite.setSize(size);
	}

	public void setTitle(String title) {
		// do nothing
	}

	public void setVisible(boolean visible) {
		composite.setVisible(visible);
	}

}
