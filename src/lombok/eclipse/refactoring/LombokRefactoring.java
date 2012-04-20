package lombok.eclipse.refactoring;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.ui.JavaElementLabels;
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

	private IType type;
	private final Map<ICompilationUnit, TextFileChange> unitChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();

	@Override
	public String getName() {
		return "Refactor Lombok";
	}

	public void setType(IType type) {
		this.type = type;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("Checking preconditions", 1);

			RefactoringStatus status = new RefactoringStatus();
			if (this.type == null) {
				status.addError("No type selected");
			}
			if (!this.type.exists()) {
				status.addError("No type selected");
			}
			if (this.type.isBinary() || !this.type.isStructureKnown()) {
				status.addError("No type selected");
			}
			return status;
		} finally {
			monitor.done();
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("Sanity check", 1);

			final RefactoringStatus status = new RefactoringStatus();

			this.unitChanges.clear();

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
			parser.setSource(this.type.getCompilationUnit());
			parser.setResolveBindings(true);
			Collection<ICompilationUnit> collection = Collections
					.singletonList(this.type.getCompilationUnit());
			parser.createASTs(
					collection.toArray(new ICompilationUnit[collection.size()]),
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
					String project = LombokRefactoring.this.type
							.getJavaProject().getElementName();
					String description = MessageFormat.format(
							"Lombok Refactor for ''{0}''",
							new Object[] { LombokRefactoring.this.type
									.getElementName() });
					String typeLabel = JavaElementLabels.getTextLabel(
							LombokRefactoring.this.type,
							JavaElementLabels.ALL_FULLY_QUALIFIED);
					String comment = MessageFormat.format(
							"Lombok Refactor for ''{0}''",
							new Object[] { typeLabel });
					Map<String, String> arguments = new HashMap<String, String>();
					arguments.put("type",
							LombokRefactoring.this.type.getHandleIdentifier());
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

}
