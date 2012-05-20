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
package lombok.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.eclipse.i18n.Messages;
import lombok.eclipse.internal.LombokEclipsePlugin.Logger;
import lombok.eclipse.refactoring.LombokRefactoringDescriptor.Attributes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public class LombokRefactoring extends Refactoring {

	private final Map<ICompilationUnit, TextFileChange> unitChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
	private final LombokRefactoringDescriptor descriptor;
	private final List<RefactoringElement> elements;

	public LombokRefactoring(LombokRefactoringDescriptor descriptor) {
		this.descriptor = descriptor;
		this.elements = new ArrayList<RefactoringElement>(descriptor.getArguments().getElements());
	}

	@Override
	public String getName() {
		return Messages.LombokRefactoring_title;
	}

	public int getNumberOfElements() {
		return this.elements.size();
	}

	public LombokRefactoringDescriptor getDescriptor() {
		return this.descriptor;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException,
			OperationCanceledException {
		try {
			monitor.beginTask(Messages.LombokRefactoring_initial_condition_monitor_begin, 1);

			RefactoringStatus status = new RefactoringStatus();
			if (this.elements == null || this.elements.isEmpty()) {
				status.addError(Messages.LombokRefactoring_no_types);
			} else {
				for (RefactoringElement element : this.elements) {
					element.updateStatus(status);
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
			monitor.beginTask(Messages.LombokRefactoring_final_condition_monitor_begin, 1);

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

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask(Messages.LombokRefactoring_create_change_monitor_begin, 1);
			final Collection<TextFileChange> changes = this.unitChanges.values();
			final CompositeChange change = new LombokCompositeChange(getName(), changes);
			return change;
		} finally {
			monitor.done();
		}
	}

	private IJavaProject queryCompilationUnits(final RefactoringStatus status, Set<ICompilationUnit> units)
			throws JavaModelException {
		IJavaProject selectedProject = null;
		try {
			for (RefactoringElement element : this.elements) {
				if (selectedProject == null) {
					selectedProject = element.getJavaProject();
				}
				if (!element.getJavaProject().equals(selectedProject)) {
					throw new IllegalStateException(Messages.LombokRefactoring_more_than_one_project);
				}

				units.addAll(element.getCompilationUnits());
			}
		} catch (IllegalStateException e) {
			status.addError(e.getMessage());
		}
		return selectedProject;
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

	protected void rewriteCompilationUnit(ASTRequestor requestor, ICompilationUnit unit, CompilationUnit node,
			RefactoringStatus status) throws CoreException {
		ASTRewrite astRewrite = ASTRewrite.create(node.getAST());
		ImportRewrite importRewrite = ImportRewrite.create(node, true);

		refactor(requestor, astRewrite, importRewrite, unit, node);

		rewriteAST(unit, astRewrite, importRewrite);
	}

	private void refactor(ASTRequestor requestor, ASTRewrite astRewrite, ImportRewrite importRewrite,
			ICompilationUnit unit, CompilationUnit node) {
		LombokRefactoringVisitor visitor = new LombokRefactoringVisitor(this);
		node.accept(visitor);
		visitor.refactor(astRewrite, importRewrite);
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
			Logger.error(e);
		} catch (CoreException e) {
			Logger.error(e);
		}
	}

	private void addChange(ICompilationUnit unit, MultiTextEdit edit) {
		TextFileChange change = this.unitChanges.get(unit);
		if (change == null) {
			change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
			change.setTextType("java"); //$NON-NLS-1$
			change.setEdit(edit);

			this.unitChanges.put(unit, change);
		} else {
			change.getEdit().addChild(edit);
		}
	}

	private boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && !edit.hasChildren();
	}

	public boolean canApply() {
		Attributes arguments = this.descriptor.getArguments();
		return arguments.isRefactorEqualsHashCode() || arguments.isRefactorGetters() || arguments.isRefactorSetters()
				|| arguments.isRefactorToString();
	}

	private final class LombokCompositeChange extends CompositeChange {

		private LombokCompositeChange(String name, Collection<TextFileChange> changes) {
			super(name, changes.toArray(new Change[changes.size()]));
		}

		@Override
		public ChangeDescriptor getDescriptor() {
			return LombokRefactoring.this.descriptor.getChange();
		}
	}

}
