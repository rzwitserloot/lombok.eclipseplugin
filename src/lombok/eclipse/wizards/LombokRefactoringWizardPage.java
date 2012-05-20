/*
 * Copyright (C) 2009 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.eclipse.wizards;

import java.text.MessageFormat;

import lombok.eclipse.i18n.Messages;
import lombok.eclipse.refactoring.LombokRefactoring;
import lombok.eclipse.refactoring.LombokRefactoringDescriptor;
import lombok.eclipse.refactoring.LombokRefactoringDescriptor.Attributes;

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
	private Button equalsHashCodeButton;
	private Button toStringButton;

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

		setMessage(MessageFormat.format(Messages.LombokRefactoringWizardPage_message,
				this.refactoring.getNumberOfElements()));

		this.getterButton = new Button(result, SWT.CHECK);
		this.getterButton.setText(Messages.LombokRefactoringWizardPage_getter_check);

		this.setterButton = new Button(result, SWT.CHECK);
		this.setterButton.setText(Messages.LombokRefactoringWizardPage_setter_check);

		// FIXME
		this.equalsHashCodeButton = new Button(result, SWT.CHECK);
		this.equalsHashCodeButton.setText(Messages.LombokRefactoringWizardPage_equals_hashcode_check);

		this.toStringButton = new Button(result, SWT.CHECK);
		this.toStringButton.setText(Messages.LombokRefactoringWizardPage_tostring_check);

		// ######
		final SelectionListener inputChangeListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleInputChanged();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};
		this.getterButton.addSelectionListener(inputChangeListener);
		this.setterButton.addSelectionListener(inputChangeListener);
		this.equalsHashCodeButton.addSelectionListener(inputChangeListener);
		this.toStringButton.addSelectionListener(inputChangeListener);

		// ######

		this.getterButton.setSelection(true);
		this.setterButton.setSelection(true);
		this.equalsHashCodeButton.setSelection(true);
		this.toStringButton.setSelection(true);

		// ######

		handleInputChanged();
	}

	void handleInputChanged() {
		LombokRefactoringDescriptor descriptor = this.refactoring.getDescriptor();
		Attributes arguments = descriptor.getArguments();

		arguments.setRefactorGetters(this.getterButton.getSelection());
		arguments.setRefactorSetters(this.setterButton.getSelection());
		arguments.setRefactorEqualsAndHashCode(this.equalsHashCodeButton.getSelection());
		arguments.setRefactorToString(this.toStringButton.getSelection());
		setPageComplete(this.refactoring.canApply());
	}

}