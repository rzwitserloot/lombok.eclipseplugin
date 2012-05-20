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

import static lombok.eclipse.refactoring.ASTUtils.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.eclipse.internal.LombokIdentifiers;
import lombok.eclipse.refactoring.LombokRefactoringDescriptor.Attributes;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class LombokRefactoringVisitor extends ASTVisitor {

	private final List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
	private final List<MethodDeclaration> getters = new ArrayList<MethodDeclaration>();
	private final List<MethodDeclaration> setters = new ArrayList<MethodDeclaration>();
	private final List<MethodDeclaration> equalsHashCode = new ArrayList<MethodDeclaration>();
	private final List<MethodDeclaration> toString = new ArrayList<MethodDeclaration>();

	private final LombokRefactoring refactoring;

	public LombokRefactoringVisitor(LombokRefactoring refactoring) {
		this.refactoring = refactoring;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		this.fields.add(node);
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if (isGenerated(node)) {
			return true;
		}
		String identifier = node.getName().getIdentifier();
		LombokRefactoringDescriptor descriptor = this.refactoring.getDescriptor();
		Attributes arguments = descriptor.getArguments();
		if (arguments.isRefactorGetters() && (identifier.startsWith("get") || identifier.startsWith("is"))) { //$NON-NLS-1$ //$NON-NLS-2$
			analzyeGetter(node);
		} else if (arguments.isRefactorSetters() && identifier.startsWith("set")) { //$NON-NLS-1$
			analyzeSetter(node);
		} else if (arguments.isRefactorEqualsHashCode() && (identifier.startsWith("equals"))) { //$NON-NLS-1$
			analyzeEquals(node);// TODO canEqual
		} else if (arguments.isRefactorEqualsHashCode() && (identifier.startsWith("hashCode"))) { //$NON-NLS-1$
			analyzeHashCode(node);
		} else if (arguments.isRefactorToString() && identifier.startsWith("toString")) { //$NON-NLS-1$
			analyzeToString(node);
		}

		return true;
	}

	public void refactor(ASTRewrite astRewrite, ImportRewrite importRewrite) {
		LombokRewriter rewriter = new LombokRewriter(astRewrite, importRewrite, this);
		rewriter.refactorGetters();
		rewriter.refactorSetters();
		rewriter.refactorEqualsHashCode();
		rewriter.refactorToString();
	}

	private void analzyeGetter(MethodDeclaration node) {
		List<ASTNode> statements = getStatements(node);
		if (statements.size() == 1) {
			for (Iterator<ASTNode> it = statements.iterator(); it.hasNext();) {
				ASTNode child = it.next();
				if (child.getNodeType() == ASTNode.RETURN_STATEMENT) {
					this.getters.add(node);
				}
			}
		}
	}

	private void analyzeSetter(MethodDeclaration node) {
		List<ASTNode> statements = getStatements(node);
		if (statements.size() == 1) {
			for (Iterator<ASTNode> it = statements.iterator(); it.hasNext();) {
				ASTNode child = it.next();
				if (child.getNodeType() == ASTNode.EXPRESSION_STATEMENT
						&& ((ExpressionStatement) child).getExpression().getNodeType() == ASTNode.ASSIGNMENT) {
					this.setters.add(node);
				}
			}
		}
	}

	private void analyzeToString(MethodDeclaration node) {
		if (Modifier.isPublic(node.getModifiers())) {
			if (node.getReturnType2().resolveBinding().getQualifiedName().equals(String.class.getName())) {
				List<?> parameters = node.parameters();
				if (parameters.isEmpty()) {
					this.toString.add(node);
				}
			}
		}
	}

	private void analyzeEquals(MethodDeclaration node) {
		if (Modifier.isPublic(node.getModifiers())) {
			if (node.getReturnType2().resolveBinding().getQualifiedName().equals(boolean.class.getName())) {
				@SuppressWarnings("unchecked")
				List<SingleVariableDeclaration> parameters = node.parameters();
				if (parameters.size() == 1) {
					SingleVariableDeclaration parameterNode = parameters.get(0);
					if (parameterNode.getType().resolveBinding().getQualifiedName().equals(Object.class.getName())) {
						this.equalsHashCode.add(node);
					}
				}
			}
		}
	}

	private void analyzeHashCode(MethodDeclaration node) {
		if (Modifier.isPublic(node.getModifiers())) {
			if (node.getReturnType2().resolveBinding().getQualifiedName().equals(int.class.getName())) {
				List<?> parameters = node.parameters();
				if (parameters.isEmpty()) {
					this.equalsHashCode.add(node);
				}
			}
		}
	}

	private static class LombokRewriter {

		private final ImportRewrite importRewrite;
		private final ASTRewrite astRewrite;
		private final LombokRefactoringVisitor visitor;

		public LombokRewriter(ASTRewrite astRewrite, ImportRewrite importRewrite, LombokRefactoringVisitor visitor) {
			this.astRewrite = astRewrite;
			this.importRewrite = importRewrite;
			this.visitor = visitor;
		}

		private void refactorSetters() {
			for (MethodDeclaration method : this.visitor.setters) {
				refactorSetter(method);
			}
		}

		private void refactorGetters() {
			for (MethodDeclaration method : this.visitor.getters) {
				refactorGetter(method);
			}
		}

		private void refactorEqualsHashCode() {
			for (MethodDeclaration method : this.visitor.equalsHashCode) {
				refactorEqualsHashCode(method);
			}
		}

		private void refactorToString() {
			for (MethodDeclaration method : this.visitor.toString) {
				refactorToString(method);
			}
		}

		private void refactorSetter(MethodDeclaration method) {
			// this.value = value;
			Assignment statement = (Assignment) ((ExpressionStatement) method.getBody().statements().get(0))
					.getExpression();
			Expression lhs = statement.getLeftHandSide();
			if (lhs.getNodeType() == ASTNode.FIELD_ACCESS) {
				FieldDeclaration field = checkMatch(((FieldAccess) lhs).getName());
				if (field != null) {
					Expression rightHandSide = statement.getRightHandSide();
					if (rightHandSide.getNodeType() == ASTNode.SIMPLE_NAME) {
						String setterType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_SETTER);
						addAnnotationToField(field, setterType, method.getModifiers());
						this.astRewrite.remove(method, null);
					}
				}
			}
		}

		private void refactorGetter(MethodDeclaration method) {
			// return this.value;
			ReturnStatement statement = (ReturnStatement) method.getBody().statements().get(0);
			Expression expression = statement.getExpression();
			FieldDeclaration field = checkMatch(expression);
			if (field != null) {
				String getterType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_GETTER);

				addAnnotationToField(field, getterType, method.getModifiers());

				this.astRewrite.remove(method, null);
			}
		}

		private void refactorEqualsHashCode(MethodDeclaration method) {
			String annotationType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_EQUALS_HASHCODE);
			addAnnotationToType(method, annotationType);

			this.astRewrite.remove(method, null);
		}

		private void refactorToString(MethodDeclaration method) {
			String annotationType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_TOSTRING);

			addAnnotationToType(method, annotationType);

			this.astRewrite.remove(method, null);
		}

		public void addAnnotationToType(MethodDeclaration method, String annotationType) {
			TypeDeclaration typeNode = (TypeDeclaration) method.getParent();

			if (!ASTUtils.isAnnotationPresent(this.astRewrite, typeNode, annotationType)) {
				MarkerAnnotation annotation = typeNode.getAST().newMarkerAnnotation();
				annotation.setTypeName(typeNode.getAST().newName(annotationType));

				ListRewrite listRewrite = this.astRewrite.getListRewrite(typeNode, typeNode.getModifiersProperty());
				listRewrite.insertFirst(annotation, null);
			}

		}

		private void addAnnotationToField(FieldDeclaration field, String annotationName, int modifiers) {
			if (!ASTUtils.isAnnotationPresent(this.astRewrite, field, annotationName)) {
				// import lombok.AccessLevel;
				// PUBLIC, PROTECTED, PACKAGE, and PRIVATE.
				final Annotation annotation;
				if (Modifier.isPublic(modifiers)) {
					// default
					MarkerAnnotation newMarkerAnnotation = field.getAST().newMarkerAnnotation();
					annotation = newMarkerAnnotation;
				} else {
					String accessLevelType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_ACCESS_LEVEL);

					SingleMemberAnnotation singleAnnotation = field.getAST().newSingleMemberAnnotation();

					FieldAccess newFieldAccess = field.getAST().newFieldAccess();
					newFieldAccess.setExpression(field.getAST().newSimpleName(accessLevelType));
					newFieldAccess.setName(field.getAST().newSimpleName(getFieldNameForModifier(modifiers)));
					singleAnnotation.setValue(newFieldAccess);

					annotation = singleAnnotation;
				}
				annotation.setTypeName(field.getAST().newName(annotationName));

				this.astRewrite.getListRewrite(field, field.getModifiersProperty()).insertFirst(annotation, null);
			}
		}

		private String getFieldNameForModifier(int modifiers) {
			final String fieldName;
			if (Modifier.isPrivate(modifiers)) {
				fieldName = LombokIdentifiers.ACCESS_LEVEL_PRIVATE;
			} else if (Modifier.isProtected(modifiers)) {
				fieldName = LombokIdentifiers.ACCESS_LEVEL_PROTECTED;
			} else {
				fieldName = LombokIdentifiers.ACCESS_LEVEL_PACKAGE;
			}
			return fieldName;
		}

		private FieldDeclaration checkMatch(Expression expression) {
			if (expression.getNodeType() == ASTNode.SIMPLE_NAME) {
				SimpleName simple = (SimpleName) expression;
				IBinding expressionBinding = simple.resolveBinding();
				if (expressionBinding != null) {
					for (FieldDeclaration field : this.visitor.fields) {
						IBinding binding = getBinding(field);
						if (expressionBinding.equals(binding)) {
							return field;
						}
					}
				}
			}
			return null;
		}
	}

}
