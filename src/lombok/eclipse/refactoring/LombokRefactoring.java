package lombok.eclipse.refactoring;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

	@Override
	public String getName() {
		return "Refactor Lombok";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("Checking preconditions", 1);

			RefactoringStatus status = new RefactoringStatus();
			if (this.elements == null || this.elements.isEmpty()) {
				status.addError("No type(s) selected");
			} else {
				for (IJavaElement element : this.elements) {
					if (!element.exists()) {
						status.addError("No type selected");
					}
					if (!element.isStructureKnown()) {
						status.addError("No type selected");
					}
					if (element instanceof ICompilationUnit) {
						if (!((ICompilationUnit) element).isConsistent()) {
							status.addError("No type selected");
						}
					}
					if (element instanceof IType) {
						if (((IType) element).isBinary()) {
							status.addError("No type selected");
						}
					}
				}
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	private final <K, V> void put(Map<K, Set<V>> map, K key, V value) {
		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>();
			map.put(key, set);
		}
		set.add(value);
	}

	private final <K, V> void put(Map<K, Set<V>> map, K key,
			Collection<V> values) {
		Set<V> set = map.get(key);
		if (set == null) {
			set = new HashSet<V>();
			map.put(key, set);
		}
		set.addAll(values);
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("Sanity check", 1);

			final RefactoringStatus status = new RefactoringStatus();

			this.unitChanges.clear();

			IJavaProject selectedProject = null;

			Set<ICompilationUnit> units = new HashSet<ICompilationUnit>();
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
					for (IPackageFragment fragment : project
							.getPackageFragments()) {
						units.addAll(Arrays.asList(fragment
								.getCompilationUnits()));
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
							units.addAll(Arrays.asList(fragment
									.getCompilationUnits()));
						}
					}
				}
			}

			ASTRequestor requestors = new ASTRequestor() {

				@Override
				public void acceptAST(ICompilationUnit source,
						CompilationUnit ast) {
					try {
						rewriteCompilationUnit(this, source, ast, status);
					} catch (CoreException exception) {
						exception.printStackTrace();
					}
				}
			};
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setProject(selectedProject);
			parser.setResolveBindings(true);
			parser.createASTs(
					units.toArray(new ICompilationUnit[units.size()]),
					new String[0], requestors, new NullProgressMonitor());

			return status;
		} finally {
			monitor.done();
		}
	}

	protected void rewriteCompilationUnit(ASTRequestor requestor,
			ICompilationUnit unit, CompilationUnit node,
			RefactoringStatus status) throws CoreException {
		ASTRewrite astRewrite = ASTRewrite.create(node.getAST());
		ImportRewrite importRewrite = ImportRewrite.create(node, true);

		changeGetterSetter(requestor, astRewrite, importRewrite, unit, node);

		rewriteAST(unit, astRewrite, importRewrite);

	}

	private void changeGetterSetter(ASTRequestor requestor,
			ASTRewrite astRewrite, ImportRewrite importRewrite,
			ICompilationUnit unit, CompilationUnit node) {

		GetterSetterRefactor x = new GetterSetterRefactor(astRewrite,
				importRewrite);
		node.accept(x);
		x.refactor();

	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		try {
			monitor.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changes = this.unitChanges
					.values();
			CompositeChange change = new CompositeChange(getName(),
					changes.toArray(new Change[changes.size()])) {

				@Override
				public ChangeDescriptor getDescriptor() {
					IJavaProject javaProject = LombokRefactoring.this.elements
							.iterator().next().getJavaProject();
					String project = javaProject.getElementName();
					String comment = MessageFormat.format(
							"Lombok Refactor for ''{0}''",
							new Object[] { project });
					String description = comment;
					Map<String, String> arguments = new HashMap<String, String>();
					arguments.put("project", javaProject.getElementName());
					// FIXME
					return new RefactoringChangeDescriptor(
							new LombokRefactoringDescriptor(project,
									description, comment, arguments));
				}
			};
			return change;
		} finally {
			monitor.done();
		}
	}

	private void rewriteAST(ICompilationUnit unit, ASTRewrite astRewrite,
			ImportRewrite importRewrite) {
		try {
			MultiTextEdit edit = new MultiTextEdit();
			TextEdit astEdit = astRewrite.rewriteAST();

			if (!isEmptyEdit(astEdit)) {
				edit.addChild(astEdit);
			}
			TextEdit importEdit = importRewrite
					.rewriteImports(new NullProgressMonitor());
			if (!isEmptyEdit(importEdit)) {
				edit.addChild(importEdit);
			}
			if (isEmptyEdit(edit)) {
				return;
			}

			TextFileChange change = this.unitChanges.get(unit);
			if (change == null) {
				change = new TextFileChange(unit.getElementName(),
						(IFile) unit.getResource());
				change.setTextType("java");
				change.setEdit(edit);

				this.unitChanges.put(unit, change);
			} else {
				change.getEdit().addChild(edit);
			}
		} catch (MalformedTreeException e) {
			log(e);
		} catch (CoreException e) {
			log(e);
		}
	}

	private boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && !edit.hasChildren();
	}

	private void log(Exception exception) {
		exception.printStackTrace();

	}

	public void setElements(Collection<IJavaElement> elements) {
		this.elements = elements;
	}

}
