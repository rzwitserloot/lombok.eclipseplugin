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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * 
 * @author gransberger
 * 
 */
public class LombokRefactoringContribution extends RefactoringContribution {

	@Override
	public RefactoringDescriptor createDescriptor() {
		return new LombokRefactoringDescriptor();
	}

	@SuppressWarnings("unchecked")
	@Override
	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment,
			@SuppressWarnings("rawtypes") Map arguments, int flags) throws IllegalArgumentException {
		LombokRefactoringDescriptor descr = new LombokRefactoringDescriptor(project, description, comment);
		descr.getArguments().putAll(arguments);
		return descr;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map retrieveArgumentMap(RefactoringDescriptor descriptor) {
		return ((LombokRefactoringDescriptor) descriptor).getArguments();
	}

	public Refactoring createRefactoring(LombokRefactoringDescriptor descriptor, RefactoringStatus status)
			throws CoreException {
		return new LombokRefactoring();
	}

}
