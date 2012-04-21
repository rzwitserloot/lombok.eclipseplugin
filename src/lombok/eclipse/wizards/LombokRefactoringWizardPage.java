package lombok.eclipse.wizards;

import lombok.eclipse.refactoring.LombokRefactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class LombokRefactoringWizardPage extends UserInputWizardPage {

	private final LombokRefactoring refactoring;
	private Button setterButton;
	private Button getterButton;

	public LombokRefactoringWizardPage(String name, LombokRefactoring refactoring) {
		super(name);
		this.refactoring = refactoring;
	}

	@Override
	public void createControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		setControl(result);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		result.setLayout(layout);

		setMessage("Select the refactorings to apply for the " + this.refactoring.getNumberOfElements()
				+ " selected elements.");

		this.getterButton = new Button(result, SWT.CHECK);
		this.getterButton.setText("Change getters to @Getter annotation");

		this.setterButton = new Button(result, SWT.CHECK);
		this.setterButton.setText("Change setters to @Setter annotation");

		// ######
		this.getterButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleInputChanged();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		this.setterButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleInputChanged();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// ######

		this.getterButton.setSelection(true);
		this.setterButton.setSelection(true);

		// ######

		handleInputChanged();
	}

	void handleInputChanged() {
		this.refactoring.refactorGetters(this.getterButton.getSelection());
		this.refactoring.refactorSetters(this.setterButton.getSelection());
		setPageComplete(this.refactoring.canApply());
	}

}