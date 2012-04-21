package lombok.eclipse.refactoring;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import lombok.eclipse.internal.LombokEclipsePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * 
 * @author gransberger
 * 
 */
public class LombokRefactoring extends Refactoring {

	private final Map<ICompilationUnit, TextFileChange> unitChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
	private Collection<IJavaElement> elements;
	private boolean refactorSetters;
	private boolean refactorGetters;

	@Override
	public String getName() {
		return "Use Lombok Annotations";
	}

	public int getNumberOfElements() {
		return this.elements.size();
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		try {
			monitor.beginTask("Checking preconditions", 1);

			RefactoringStatus status = new RefactoringStatus();
			if (this.elements == null || this.elements.isEmpty()) {
				status.addError("No type(s) selected");
			} else {
				for (IJavaElement element : this.elements) {
					if (!element.exists()) {
						status.addError("Non existing type: " + element.getElementName());
					}
					if (!element.isStructureKnown()) {
						status.addError("Unknown structure: " + element.getElementName());
					}
					if (element instanceof ICompilationUnit) {
						if (!((ICompilationUnit) element).isConsistent()) {
							status.addError("Not consistent: " + element.getElementName());
						}
					}
					if (element instanceof IType) {
						if (((IType) element).isBinary()) {
							status.addError("Refactoring of binary types is not possible: " + element.getElementName());
						}
					}
				}
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		try {
			monitor.beginTask("Sanity check", 1);

			final RefactoringStatus status = new RefactoringStatus();

			this.unitChanges.clear();

			Set<ICompilationUnit> units = new HashSet<ICompilationUnit>();
			IJavaProject selectedProject = queryCompilationUnits(status, units);

			processCompilationUnits(status, selectedProject, units);

			return status;
		} finally {
			monitor.done();
		}
	}

	private void processCompilationUnits(final RefactoringStatus status, IJavaProject project,
			Set<ICompilationUnit> units) {
		ASTRequestor requestors = new ASTRequestor() {

			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				try {
					rewriteCompilationUnit(this, source, ast, status);
				} catch (CoreException exception) {
					exception.printStackTrace();
				}
			}
		};
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setProject(project);
		parser.setResolveBindings(true);
		parser.createASTs(units.toArray(new ICompilationUnit[units.size()]), new String[0], requestors,
				new NullProgressMonitor());
	}

	private IJavaProject queryCompilationUnits(final RefactoringStatus status, Set<ICompilationUnit> units)
			throws JavaModelException {
		IJavaProject selectedProject = null;
		for (IJavaElement element : this.elements) {
			if (selectedProject == null) {
				selectedProject = element.getJavaProject();
			}
			if (!status.isOK()) {
				break;
			}
			if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit unit = (ICompilationUnit) element;
				IJavaProject javaProject = unit.getJavaProject();
				if (!javaProject.equals(selectedProject)) {
					status.addError("Only one project allowed");
				}
				units.add(unit);
			} else if (element.getElementType() == IJavaElement.JAVA_PROJECT) {
				IJavaProject project = (IJavaProject) element;
				if (!project.equals(selectedProject)) {
					status.addError("Only one project allowed");
				}
				for (IPackageFragment fragment : project.getPackageFragments()) {
					units.addAll(Arrays.asList(fragment.getCompilationUnits()));
				}
			} else if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				IPackageFragment packag = (IPackageFragment) element;
				if (!packag.getJavaProject().equals(selectedProject)) {
					status.addError("Only one project allowed");
				}
				units.addAll(Arrays.asList(packag.getCompilationUnits()));
			} else if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				IPackageFragmentRoot srcFolder = (IPackageFragmentRoot) element;
				if (!srcFolder.getJavaProject().equals(selectedProject)) {
					status.addError("Only one project allowed");
				}
				for (IJavaElement child : srcFolder.getChildren()) {
					if (child instanceof IPackageFragment) {
						IPackageFragment fragment = (IPackageFragment) child;
						units.addAll(Arrays.asList(fragment.getCompilationUnits()));
					}
				}
			}
		}
		return selectedProject;
	}

	protected void rewriteCompilationUnit(ASTRequestor requestor, ICompilationUnit unit, CompilationUnit node,
			RefactoringStatus status) throws CoreException {
		ASTRewrite astRewrite = ASTRewrite.create(node.getAST());
		ImportRewrite importRewrite = ImportRewrite.create(node, true);

		changeGetterSetter(requestor, astRewrite, importRewrite, unit, node);

		rewriteAST(unit, astRewrite, importRewrite);
	}

	private void changeGetterSetter(ASTRequestor requestor, ASTRewrite astRewrite, ImportRewrite importRewrite,
			ICompilationUnit unit, CompilationUnit node) {
		LombokRefactoringVisitor visitor = new LombokRefactoringVisitor(astRewrite, importRewrite,
				this.refactorGetters, this.refactorSetters);
		node.accept(visitor);
		visitor.refactor();
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changes = this.unitChanges.values();
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {

				@Override
				public ChangeDescriptor getDescriptor() {

					StringBuilder elementsBuilder = new StringBuilder();
					for (IJavaElement e : LombokRefactoring.this.elements) {
						elementsBuilder.append(e.getHandleIdentifier()).append(";");
					}

					IJavaProject javaProject = LombokRefactoring.this.elements.iterator().next().getJavaProject();
					String project = javaProject.getElementName();
					String comment = null;
					String description = MessageFormat.format("Lombok Refactor for {1} in project ''{0}''",
							new Object[] { project, getNumberOfElements() });
					Map<String, String> arguments = new HashMap<String, String>();
					arguments.put(LombokRefactoringDescriptor.ATTRIBUTE_PROJECT, javaProject.getElementName());
					arguments.put(LombokRefactoringDescriptor.ATTRIBUTE_REFACTOR_GETTERS,
							String.valueOf(LombokRefactoring.this.refactorGetters));
					arguments.put(LombokRefactoringDescriptor.ATTRIBUTE_REFACTOR_SETTERS,
							String.valueOf(LombokRefactoring.this.refactorSetters));
					arguments.put(LombokRefactoringDescriptor.ATTRIBUTE_ELEMENTS, elementsBuilder.toString());
					return new RefactoringChangeDescriptor(new LombokRefactoringDescriptor(project, description,
							comment, arguments));
				}
			};
			return change;
		} finally {
			monitor.done();
		}
	}

	private void rewriteAST(ICompilationUnit unit, ASTRewrite astRewrite, ImportRewrite importRewrite) {
		try {
			MultiTextEdit edit = new MultiTextEdit();
			TextEdit astEdit = astRewrite.rewriteAST();

			if (!isEmptyEdit(astEdit)) {
				edit.addChild(astEdit);
			}
			TextEdit importEdit = importRewrite.rewriteImports(new NullProgressMonitor());
			if (!isEmptyEdit(importEdit)) {
				edit.addChild(importEdit);
			}
			if (isEmptyEdit(edit)) {
				return;
			}

			addChange(unit, edit);
		} catch (MalformedTreeException e) {
			LombokEclipsePlugin.log(e);
		} catch (CoreException e) {
			LombokEclipsePlugin.log(e);
		}
	}

	private void addChange(ICompilationUnit unit, MultiTextEdit edit) {
		TextFileChange change = this.unitChanges.get(unit);
		if (change == null) {
			change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
			change.setTextType("java");
			change.setEdit(edit);

			this.unitChanges.put(unit, change);
		} else {
			change.getEdit().addChild(edit);
		}
	}

	private boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && !edit.hasChildren();
	}

	public void setElements(Collection<IJavaElement> elements) {
		this.elements = elements;
	}

	public void refactorGetters(boolean selection) {
		this.refactorGetters = selection;
	}

	public void refactorSetters(boolean selection) {
		this.refactorSetters = selection;
	}

	public boolean canApply() {
		return this.refactorSetters || this.refactorGetters;
	}

}
