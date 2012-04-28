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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

public class LombokRefactoringVisitor extends ASTVisitor {

	private final List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
	private final List<MethodDeclaration> getters = new ArrayList<MethodDeclaration>();
	private final List<MethodDeclaration> setters = new ArrayList<MethodDeclaration>();

	private final boolean refactorGetters;
	private final boolean refactorSetters;

	public LombokRefactoringVisitor(boolean refactorGetters, boolean refactorSetters) {
		this.refactorGetters = refactorGetters;
		this.refactorSetters = refactorSetters;
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
		if (this.refactorGetters && (identifier.startsWith("get") || identifier.startsWith("is"))) {
			analzyeGetter(node);
		} else if (this.refactorSetters && identifier.startsWith("set")) {
			analyzeSetter(node);
		}

		return true;
	}

	public void refactor(ASTRewrite astRewrite, ImportRewrite importRewrite) {
		LombokRewriter rewriter = new LombokRewriter(astRewrite, importRewrite, this);
		rewriter.refactorGetters();
		rewriter.refactorSetters();
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

		private void addAnnotationToField(FieldDeclaration field, String annotationName, int modifiers) {
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
