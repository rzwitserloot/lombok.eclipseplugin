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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public final class ASTUtils {

	private ASTUtils() {
		throw new AssertionError();
	}

	public static boolean isAnnotationPresent(ASTNode node, String annotationName) {
		AnnotationFinder finder = new AnnotationFinder();
		node.accept(finder);

		for (Annotation annotation : finder.getAnnotations()) {
			String typeName = annotation.getTypeName().getFullyQualifiedName();
			if (typeName.equals(annotationName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isAnnotationPresent(ASTRewrite astRewrite, BodyDeclaration node, String annotationName) {
		ListRewrite listRewrite = astRewrite.getListRewrite(node, node.getModifiersProperty());
		for (Object o : listRewrite.getRewrittenList()) {
			if (ASTUtils.isAnnotationPresent((ASTNode) o, annotationName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Resolves and returns the binding for the given node, if there's a
	 * binding.
	 * 
	 * @param node
	 * @return The resolved binding of the node or <code>null</code> if none was
	 *         found.
	 */
	public static IBinding getBinding(ASTNode node) {
		BindingFinder finder = new BindingFinder();
		node.accept(finder);
		return finder.getBinding();
	}

	/**
	 * Get the statements of the body of the given method.
	 * 
	 * @param node
	 *            The method
	 * @return A List of statements from the body of the method.
	 * @see org.eclipse.jdt.core.dom.Block#statements()
	 */
	public static List<ASTNode> getStatements(MethodDeclaration node) {
		Block body = node.getBody();
		if (body == null) {
			// abstract method/interface
			return Collections.emptyList();
		}
		@SuppressWarnings("unchecked")
		List<ASTNode> statements = body.statements();
		return statements;
	}

	/**
	 * Determines if the given node was auto-generated instead of beeing created
	 * from hand-written code.
	 * 
	 * @param node
	 * @return <code>true</code> if the code was generated (f.e. by Lombok)
	 */
	public static boolean isGenerated(ASTNode node) {
		return GeneratedBy.isGenerated(node);
	}

	// #####

	private static class GeneratedBy {
		private static Field generatedByField;

		static {
			try {
				generatedByField = ASTNode.class.getDeclaredField("$isGenerated"); //$NON-NLS-1$
			} catch (Throwable t) {
				// ignore - no $generatedBy exists when running in ecj.
			}
		}

		public static Boolean getGeneratedBy(ASTNode node) {
			if (generatedByField != null) {
				try {
					return (Boolean) generatedByField.get(node);
				} catch (Exception e) {
				}
			}
			return Boolean.FALSE;
		}

		public static boolean isGenerated(ASTNode node) {
			return Boolean.TRUE.equals(getGeneratedBy(node));
		}
	}

	private static class AnnotationFinder extends ASTVisitor {

		private final List<Annotation> annotations = new ArrayList<Annotation>();

		@Override
		public boolean visit(MarkerAnnotation node) {
			this.annotations.add(node);
			return super.visit(node);
		}

		@Override
		public boolean visit(NormalAnnotation node) {
			this.annotations.add(node);
			return super.visit(node);
		}

		@Override
		public boolean visit(SingleMemberAnnotation node) {
			this.annotations.add(node);
			return super.visit(node);
		}

		public List<Annotation> getAnnotations() {
			return this.annotations;
		}

	}

	private static class BindingFinder extends ASTVisitor {

		private IBinding binding;

		public IBinding getBinding() {
			return this.binding;
		}

		@Override
		public boolean visit(SimpleName node) {
			this.binding = node.resolveBinding();
			return super.visit(node);
		}

	}
}
