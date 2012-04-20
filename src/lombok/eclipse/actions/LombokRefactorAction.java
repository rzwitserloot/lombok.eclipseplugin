package lombok.eclipse.actions;

import java.util.Iterator;

import lombok.eclipse.refactoring.LombokRefactoring;
import lombok.eclipse.wizards.LombokRefactoringWizard;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class LombokRefactorAction implements IObjectActionDelegate {

	private IType type;
	private IWorkbenchPart activePart;

	public void dispose() {
		// Do nothing
	}

	public void run(IAction action) {
		if (this.type != null) {
			LombokRefactoring refactoring = new LombokRefactoring();
			refactoring.setType(type);
			run(new LombokRefactoringWizard(refactoring, "Lombok Refactor"),
					activePart.getSite().getShell(), "Lombok Refactor");
		}
	}

	public void run(RefactoringWizard wizard, Shell parent, String dialogTitle) {
		try {
			RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
					wizard);
			operation.run(parent, dialogTitle);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.type = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection extended = (IStructuredSelection) selection;
			for (Iterator<Object> it = extended.iterator(); it.hasNext();) {
				Object o = it.next();
				if (o instanceof IType) {
					this.type = (IType) o;
					break;
				}
			}
		}
		try {
			action.setEnabled(this.type != null && this.type.exists()
					&& this.type.isStructureKnown());
		} catch (JavaModelException exception) {
			action.setEnabled(false);
		}
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart activePart) {
		this.activePart = activePart;

	}
}
