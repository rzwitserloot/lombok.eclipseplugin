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
package lombok.eclipse.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.eclipse.i18n.Messages;
import lombok.eclipse.internal.LombokEclipsePlugin.Logger;
import lombok.eclipse.refactoring.LombokRefactoring;
import lombok.eclipse.refactoring.LombokRefactoringDescriptor;
import lombok.eclipse.refactoring.RefactoringElement;
import lombok.eclipse.wizards.LombokRefactoringWizard;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class LombokRefactorAction implements IObjectActionDelegate {

	private IWorkbenchPart activePart;
	private final List<RefactoringElement> elements = new ArrayList<RefactoringElement>();

	public void dispose() {
		// Do nothing
	}

	@Override
	public void run(IAction action) {
		if (!this.elements.isEmpty()) {
			LombokRefactoringDescriptor descriptor = new LombokRefactoringDescriptor();
			descriptor.getArguments().setElements(this.elements);
			LombokRefactoring refactoring = new LombokRefactoring(descriptor);
			run(new LombokRefactoringWizard(refactoring), this.activePart.getSite().getShell(),
					Messages.LombokRefactorAction_wizard_title);
		}
	}

	public void run(RefactoringWizard wizard, Shell parent, String failedCheckDialogTitle) {
		try {
			RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
			operation.run(parent, failedCheckDialogTitle);
		} catch (InterruptedException exception) {
			// if the initial condition checking got canceled by the user.
			// do nothing, just return
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.elements.clear();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection extended = (IStructuredSelection) selection;
			for (Iterator<?> it = extended.iterator(); it.hasNext();) {
				Object o = it.next();
				checkAndAddSelectedElement(o);
			}
		}
		action.setEnabled(!this.elements.isEmpty());
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart activePart) {
		this.activePart = activePart;
	}

	private void checkAndAddSelectedElement(Object o) {
		RefactoringElement element = isSupportedElement(o);
		if (element != null) {
			this.elements.add(element);
		} else {
			Logger.warn(MessageFormat.format(Messages.LombokRefactorAction_unsupported_element, o.getClass().getName()));
		}
	}

	private RefactoringElement isSupportedElement(Object o) {
		if (o instanceof IJavaElement) {
			RefactoringElement supported = RefactoringElement.Factory.create((IJavaElement) o);
			return supported;
		} else {
			return null;
		}
	}
}
