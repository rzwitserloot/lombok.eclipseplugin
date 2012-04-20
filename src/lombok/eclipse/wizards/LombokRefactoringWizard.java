package lombok.eclipse.wizards;

import lombok.eclipse.refactoring.LombokRefactoring;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class LombokRefactoringWizard extends RefactoringWizard {

	public LombokRefactoringWizard(LombokRefactoring refactoring, String pageTitle) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(pageTitle);
	}

	@Override
	protected void addUserInputPages() {
		addPage(new LombokRefactoringWizardPage("LombokRefactorWizardPage"));
	}
}