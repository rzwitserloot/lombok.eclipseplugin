package lombok.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.eclipse.refactoring.LombokRefactoring;
import lombok.eclipse.wizards.LombokRefactoringWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
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
	private final List<IJavaElement> elements = new ArrayList<IJavaElement>();

	public void dispose() {
		// Do nothing
	}

	@Override
	public void run(IAction action) {
		if (!this.elements.isEmpty()) {
			LombokRefactoring refactoring = new LombokRefactoring();
			refactoring.setElements(new ArrayList<IJavaElement>(this.elements));
			run(new LombokRefactoringWizard(refactoring, "Lombok Refactor"),
					this.activePart.getSite().getShell(), "Lombok Refactor");
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

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.elements.clear();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection extended = (IStructuredSelection) selection;
			for (Iterator<Object> it = extended.iterator(); it.hasNext();) {
				Object o = it.next();
				if (o instanceof IType || o instanceof ICompilationUnit
						|| o instanceof IPackageFragment
						|| o instanceof IPackageFragmentRoot
						|| o instanceof IJavaProject) {
					this.elements.add((IJavaElement) o);
				} else {
					System.out.println(o.getClass().getName());
				}
			}
		}
		action.setEnabled(!this.elements.isEmpty());
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart activePart) {
		this.activePart = activePart;

	}
}
