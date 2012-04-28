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
import java.util.Collection;
import java.util.HashMap;

import lombok.eclipse.internal.LombokEclipsePlugin.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class LombokRefactoringDescriptor extends RefactoringDescriptor {

	public static final String ID = "lombok.eclipse.refactoring.tolombok";
	
	private static final String UNKNOWN = "unknown";

	private final Attributes arguments = new Attributes();

	public LombokRefactoringDescriptor() {
		super(ID, null, UNKNOWN, null, RefactoringDescriptor.NONE);
	}

	public LombokRefactoringDescriptor(String project, String description, String comment) {
		super(ID, project, description, comment, RefactoringDescriptor.NONE);
	}

	protected Attributes getArguments() {
		return this.arguments;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Refactoring createRefactoring(final RefactoringStatus status) throws CoreException {
		Refactoring refactoring = null;
		final String id = getID();
		final RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(id);
		if (contribution != null) {
			if (contribution instanceof LombokRefactoringContribution) {
				LombokRefactoringContribution lombokContrib = (LombokRefactoringContribution) contribution;
				refactoring = lombokContrib.createRefactoring(this, status);
			} else {
				String message = MessageFormat
						.format("Refactoring contribution registered for id '{0}' returned null as result of createDescriptor(String, String, String, String, Map, int)",
								new Object[] { id });
				Logger.error(message);
			}
		}
		return refactoring;
	}

	protected static class Attributes extends HashMap<String, String> {

		private static final long serialVersionUID = 4870727884514586453L;

		public static final String ATTRIBUTE_PROJECT = "project";
		public static final String ATTRIBUTE_REFACTOR_GETTERS = "getters";
		public static final String ATTRIBUTE_REFACTOR_SETTERS = "setters";
		public static final String ATTRIBUTE_ELEMENTS = "elements";

		protected void setProject(IJavaProject project) {
			put(ATTRIBUTE_PROJECT, project.getElementName());
		}

		protected void setRefactorGetters(boolean refactorGetters) {
			put(ATTRIBUTE_REFACTOR_GETTERS, String.valueOf(refactorGetters));
		}

		protected void setRefactorSetters(boolean refactorSetters) {
			put(ATTRIBUTE_REFACTOR_SETTERS, String.valueOf(refactorSetters));
		}

		protected void setElements(Collection<RefactoringElement> elements) {
			StringBuilder elementsBuilder = new StringBuilder();
			for (RefactoringElement e : elements) {
				elementsBuilder.append(e.getHandleIdentifier()).append(";");
			}

			put(ATTRIBUTE_ELEMENTS, elementsBuilder.toString());
		}

	}
}