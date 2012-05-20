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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import lombok.eclipse.i18n.Messages;
import lombok.eclipse.internal.LombokEclipsePlugin.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class LombokRefactoringDescriptor extends RefactoringDescriptor {
	private static final String LINE_DELIMITER = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ITEM = "- "; //$NON-NLS-1$

	public static final String ID = "lombok.eclipse.refactoring.tolombok"; //$NON-NLS-1$

	private static final String UNKNOWN = "unknown"; //$NON-NLS-1$

	private final Attributes arguments = new Attributes();

	public LombokRefactoringDescriptor() {
		super(ID, null, UNKNOWN, null, RefactoringDescriptor.NONE);
	}

	public LombokRefactoringDescriptor(String project, String description, String comment) {
		super(ID, project, description, comment, RefactoringDescriptor.NONE);
	}

	public Attributes getArguments() {
		return this.arguments;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		LombokRefactoring refactoring = null;
		final String id = getID();
		final RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(id);
		if (contribution != null) {
			if (contribution instanceof LombokRefactoringContribution) {
				LombokRefactoringContribution lombokContrib = (LombokRefactoringContribution) contribution;
				refactoring = (LombokRefactoring) lombokContrib.createRefactoring(this, status);
			} else {
				String message = MessageFormat.format(Messages.LombokRefactoringDescriptor_not_registered, id);
				Logger.error(message);
			}
		}
		return refactoring;
	}

	public ChangeDescriptor getChange() {
		Attributes oldArguments = getArguments();
		Collection<RefactoringElement> elements = oldArguments.getElements();

		IJavaProject javaProject = elements.iterator().next().getJavaProject();
		String project = javaProject.getElementName();
		String description = MessageFormat.format(Messages.LombokRefactoring_change_description, new Object[] {
				project, elements.size() });

		StringBuilder comments = new StringBuilder();
		comments.append(ITEM)
				.append(MessageFormat.format(Messages.LombokRefactoring_change_comment_project,
						javaProject.getElementName())).append(LINE_DELIMITER);
		comments.append(ITEM)
				.append(MessageFormat.format(Messages.LombokRefactoring_change_comment_getter,
						String.valueOf(oldArguments.isRefactorGetters()))).append(LINE_DELIMITER);
		comments.append(ITEM)
				.append(MessageFormat.format(Messages.LombokRefactoring_change_comment_setter,
						String.valueOf(oldArguments.isRefactorSetters()))).append(LINE_DELIMITER);
		comments.append(ITEM)
				.append(MessageFormat.format(Messages.LombokRefactoring_change_comment_equals_hashcode,
						String.valueOf(oldArguments.isRefactorEqualsHashCode()))).append(LINE_DELIMITER);
		comments.append(ITEM)
				.append(MessageFormat.format(Messages.LombokRefactoring_change_comment_tostring,
						String.valueOf(oldArguments.isRefactorToString()))).append(LINE_DELIMITER);
		comments.append(ITEM).append(Messages.LombokRefactoring_change_comment_elements_title);
		for (RefactoringElement element : elements) {
			comments.append(
					MessageFormat.format(Messages.LombokRefactoring_change_comment_element, element.getTypeName(),
							element.getElementName())).append(", "); //$NON-NLS-1$
		}
		comments.append(LINE_DELIMITER);

		LombokRefactoringDescriptor descr = new LombokRefactoringDescriptor(project, description, comments.toString());
		Attributes arguments = descr.getArguments();
		arguments.putAll(oldArguments);
		arguments.setProject(javaProject);
		arguments.setElements(elements);

		return new RefactoringChangeDescriptor(descr);
	}

	public static class Attributes extends HashMap<String, String> {

		private static final long serialVersionUID = 4870727884514586453L;

		public static final String ATTRIBUTE_PROJECT = "project"; //$NON-NLS-1$
		public static final String ATTRIBUTE_REFACTOR_GETTERS = "getters"; //$NON-NLS-1$
		public static final String ATTRIBUTE_REFACTOR_SETTERS = "setters"; //$NON-NLS-1$
		public static final String ATTRIBUTE_REFACTOR_EQUALS_HASHCODE = "equalsHashCode"; //$NON-NLS-1$
		public static final String ATTRIBUTE_REFACTOR_TOSTRING = "toString"; //$NON-NLS-1$
		public static final String ATTRIBUTE_ELEMENTS = "elements"; //$NON-NLS-1$

		public void setProject(IJavaProject project) {
			put(ATTRIBUTE_PROJECT, project.getElementName());
		}

		public void setRefactorGetters(boolean refactorGetters) {
			put(ATTRIBUTE_REFACTOR_GETTERS, String.valueOf(refactorGetters));
		}

		public void setRefactorSetters(boolean refactorSetters) {
			put(ATTRIBUTE_REFACTOR_SETTERS, String.valueOf(refactorSetters));
		}

		public void setRefactorEqualsAndHashCode(boolean refactor) {
			put(ATTRIBUTE_REFACTOR_EQUALS_HASHCODE, String.valueOf(refactor));
		}

		public void setRefactorToString(boolean refactor) {
			put(ATTRIBUTE_REFACTOR_TOSTRING, String.valueOf(refactor));
		}

		public void setElements(Collection<RefactoringElement> elements) {
			StringBuilder elementsBuilder = new StringBuilder();
			for (RefactoringElement e : elements) {
				elementsBuilder.append(e.getHandleIdentifier()).append(";"); //$NON-NLS-1$
			}

			put(ATTRIBUTE_ELEMENTS, elementsBuilder.toString());
		}

		public boolean isRefactorToString() {
			return Boolean.parseBoolean(get(ATTRIBUTE_REFACTOR_TOSTRING));
		}

		public boolean isRefactorGetters() {
			return Boolean.parseBoolean(get(ATTRIBUTE_REFACTOR_GETTERS));
		}

		public boolean isRefactorSetters() {
			return Boolean.parseBoolean(get(ATTRIBUTE_REFACTOR_SETTERS));
		}

		public boolean isRefactorEqualsHashCode() {
			return Boolean.parseBoolean(get(ATTRIBUTE_REFACTOR_EQUALS_HASHCODE));
		}

		protected Collection<RefactoringElement> getElements() {
			List<RefactoringElement> elements = new ArrayList<RefactoringElement>();
			String string = get(ATTRIBUTE_ELEMENTS);

			String[] split = string.split(";");
			if (split != null) {
				for (String s : split) {
					if (s.trim().length() > 0) {
						elements.add(RefactoringElement.Factory.create(JavaCore.create(s)));
					}
				}
			}
			return elements;
		}

	}
}