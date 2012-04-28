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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public interface RefactoringElement {

	public int getElementType();

	public Collection<ICompilationUnit> getCompilationUnits() throws JavaModelException;

	public String getHandleIdentifier();

	public IJavaProject getJavaProject();

	public void updateStatus(RefactoringStatus status) throws JavaModelException;

	public static class Factory {
		public static RefactoringElement create(IJavaElement element) {
			if (element.getElementType() == IJavaElement.TYPE) {
				return new TypeRefactoringElement(element);
			}
			if (element.getElementType() == IJavaElement.COMPILATION_UNIT) {
				return new CompilationUnitRefactoringElement(element);
			}
			if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				return new PackageRefactoringElement(element);
			}
			if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
				return new PackageRootRefactoringElement(element);
			}
			if (element.getElementType() == IJavaElement.TYPE) {
				return new ProjectRefactoringElement(element);
			}
			return null;
		}
	}

	public static abstract class ARefactoringElement implements RefactoringElement {
		protected final IJavaElement element;

		public ARefactoringElement(IJavaElement element) {
			if (element.getElementType() != getElementType()) {
				throw new IllegalArgumentException("Element does not match expected type: " + getElementType());
			}
			this.element = element;
		}

		@Override
		public void updateStatus(RefactoringStatus status) throws JavaModelException {
			if (!element.exists()) {
				status.addError("Non existing type: " + element.getElementName());
			}
			if (!element.isStructureKnown()) {
				status.addError("Unknown structure: " + element.getElementName());
			}
		}

		@Override
		public String getHandleIdentifier() {
			return element.getHandleIdentifier();
		}

		@Override
		public IJavaProject getJavaProject() {
			return element.getJavaProject();
		}

	}

	public static class TypeRefactoringElement extends ARefactoringElement {

		public TypeRefactoringElement(IJavaElement element) {
			super(element);
		}

		@Override
		public int getElementType() {
			return IJavaElement.TYPE;
		}

		@Override
		public Collection<ICompilationUnit> getCompilationUnits() {
			IType type = (IType) element;
			ICompilationUnit unit = type.getCompilationUnit();
			return Collections.singleton(unit);
		}

		@Override
		public void updateStatus(RefactoringStatus status) throws JavaModelException {
			if (((IType) element).isBinary()) {
				status.addError("Refactoring of binary types is not possible: " + element.getElementName());
			}
			super.updateStatus(status);
		}

	}

	public static class CompilationUnitRefactoringElement extends ARefactoringElement {

		public CompilationUnitRefactoringElement(IJavaElement element) {
			super(element);
		}

		@Override
		public int getElementType() {
			return IJavaElement.COMPILATION_UNIT;
		}

		@Override
		public Collection<ICompilationUnit> getCompilationUnits() {
			ICompilationUnit unit = (ICompilationUnit) element;
			return Collections.singleton(unit);
		}

		@Override
		public void updateStatus(RefactoringStatus status) throws JavaModelException {
			if (!((ICompilationUnit) element).isConsistent()) {
				status.addError("Not consistent: " + element.getElementName());
			}
			super.updateStatus(status);
		}

	}

	public static class PackageRefactoringElement extends ARefactoringElement {

		public PackageRefactoringElement(IJavaElement element) {
			super(element);
		}

		@Override
		public int getElementType() {
			return IJavaElement.PACKAGE_FRAGMENT;
		}

		@Override
		public Collection<ICompilationUnit> getCompilationUnits() throws JavaModelException {
			IPackageFragment packag = (IPackageFragment) element;
			return Arrays.asList(packag.getCompilationUnits());
		}

	}

	public static class PackageRootRefactoringElement extends ARefactoringElement {

		public PackageRootRefactoringElement(IJavaElement element) {
			super(element);
		}

		@Override
		public int getElementType() {
			return IJavaElement.PACKAGE_FRAGMENT_ROOT;
		}

		@Override
		public Collection<ICompilationUnit> getCompilationUnits() throws JavaModelException {
			IPackageFragmentRoot srcFolder = (IPackageFragmentRoot) element;
			Set<ICompilationUnit> units = new HashSet<ICompilationUnit>();
			for (IJavaElement child : srcFolder.getChildren()) {
				Collection<ICompilationUnit> childUnits = Factory.create(child).getCompilationUnits();
				units.addAll(childUnits);
			}
			return units;
		}

	}

	public static class ProjectRefactoringElement extends ARefactoringElement {

		public ProjectRefactoringElement(IJavaElement element) {
			super(element);
		}

		@Override
		public int getElementType() {
			return IJavaElement.JAVA_PROJECT;
		}

		@Override
		public Collection<ICompilationUnit> getCompilationUnits() throws JavaModelException {
			IJavaProject project = (IJavaProject) element;
			Set<ICompilationUnit> units = new HashSet<ICompilationUnit>();

			for (IPackageFragment fragment : project.getPackageFragments()) {
				Collection<ICompilationUnit> childUnits = Factory.create(fragment).getCompilationUnits();
				units.addAll(childUnits);
			}
			return units;
		}

	}

}
