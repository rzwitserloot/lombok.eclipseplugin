package lombok.eclipse.wizards;

import lombok.eclipse.refactoring.LombokRefactoring;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class LombokRefactoringWizard extends RefactoringWizard {

	private final LombokRefactoring refactoring;

	public LombokRefactoringWizard(LombokRefactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		this.refactoring = refactoring;
		setDefaultPageTitle("Refactor boilerplate Java to Lombok");
	}

	@Override
	protected void addUserInputPages() {
		addPage(new LombokRefactoringWizardPage("LombokRefactorWizardPage", this.refactoring));
	}
}