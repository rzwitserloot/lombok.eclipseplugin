package lombok.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

public class GetterSetterRefactor extends ASTVisitor {

	private final List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
	private final List<MethodDeclaration> getters = new ArrayList<MethodDeclaration>();
	private final List<MethodDeclaration> setters = new ArrayList<MethodDeclaration>();
	private final ImportRewrite importRewrite;
	private final ASTRewrite astRewrite;

	public GetterSetterRefactor(ASTRewrite astRewrite,
			ImportRewrite importRewrite) {
		this.astRewrite = astRewrite;
		this.importRewrite = importRewrite;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		this.fields.add(node);
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		String identifier = node.getName().getIdentifier();
		if (identifier.startsWith("get") || identifier.startsWith("is")) {
			analzyeGetter(node);
		} else if (identifier.startsWith("set")) {
			analyzeSetter(node);
		}

		return true;
	}

	private void analzyeGetter(MethodDeclaration node) {
		@SuppressWarnings("unchecked")
		List<ASTNode> statements = node.getBody().statements();
		if (statements.size() == 1) {
			for (Iterator<ASTNode> it = statements.iterator(); it.hasNext();) {
				ASTNode child = it.next();
				if (child instanceof ReturnStatement) {
					this.getters.add(node);
				}
			}
		}
	}

	private void analyzeSetter(MethodDeclaration node) {
		@SuppressWarnings("unchecked")
		List<ASTNode> statements = node.getBody().statements();
		if (statements.size() == 1) {
			for (Iterator<ASTNode> it = statements.iterator(); it.hasNext();) {
				ASTNode child = it.next();
				if (child instanceof ExpressionStatement
						&& ((ExpressionStatement) child).getExpression() instanceof Assignment) {
					this.setters.add(node);
				}
			}
		}
	}

	public void refactor() {
		refactorGetters();
		refactorSetters();
	}

	private void refactorSetters() {
		for (MethodDeclaration x : this.setters) {
			Assignment statement = (Assignment) ((ExpressionStatement) x
					.getBody().statements().get(0)).getExpression();
			Expression lhs = statement.getLeftHandSide();
			if (lhs instanceof FieldAccess) {
				FieldDeclaration field = checkMatch(((FieldAccess) lhs)
						.getName());
				if (field != null) {
					Expression rightHandSide = statement.getRightHandSide();
					if (rightHandSide instanceof SimpleName) {
						String setterType = this.importRewrite
								.addImport("lombok.Setter");

						addAnnotationToField(field, setterType,
								x.getModifiers());
						this.astRewrite.remove(x, null);
					}
				}
			}

		}
	}

	private void addAnnotationToField(FieldDeclaration field,
			String annotationName, int modifiers) {
		// import lombok.AccessLevel;
		// PUBLIC, PROTECTED, PACKAGE, and PRIVATE.
		final Annotation annotation;
		if (Modifier.isPublic(modifiers)) {
			// default
			MarkerAnnotation newMarkerAnnotation = field.getAST()
					.newMarkerAnnotation();
			annotation = newMarkerAnnotation;

		} else {
			String accessLevelType = this.importRewrite
					.addImport("lombok.AccessLevel");

			SingleMemberAnnotation singleAnnotation = field.getAST()
					.newSingleMemberAnnotation();
			annotation = singleAnnotation;

			final String fieldName;
			if (Modifier.isPrivate(modifiers)) {
				fieldName = "PRIVATE";
			} else if (Modifier.isProtected(modifiers)) {
				fieldName = "PROTECTED";
			} else {
				fieldName = "PACKAGE";
			}
			FieldAccess newFieldAccess = field.getAST().newFieldAccess();
			newFieldAccess.setExpression(field.getAST().newSimpleName(
					accessLevelType));
			newFieldAccess.setName(field.getAST().newSimpleName(fieldName));
			singleAnnotation.setValue(newFieldAccess);
		}
		annotation.setTypeName(field.getAST().newName(annotationName));

		this.astRewrite.getListRewrite(field, field.getModifiersProperty())
				.insertFirst(annotation, null);
	}

	private void refactorGetters() {
		for (MethodDeclaration x : this.getters) {
			ReturnStatement statement = (ReturnStatement) x.getBody()
					.statements().get(0);
			Expression expression = statement.getExpression();
			FieldDeclaration field = checkMatch(expression);
			if (field != null) {
				String getterType = this.importRewrite
						.addImport("lombok.Getter");

				addAnnotationToField(field, getterType, x.getModifiers());

				this.astRewrite.remove(x, null);
			}
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

		public static IBinding find(ASTNode node) {
			BindingFinder finder = new BindingFinder();
			node.accept(finder);
			return finder.getBinding();
		}
	}

	private FieldDeclaration checkMatch(Expression expression) {
		if (expression instanceof SimpleName) {
			SimpleName simple = (SimpleName) expression;
			IBinding expressionBinding = simple.resolveBinding();
			for (FieldDeclaration field : this.fields) {
				IBinding binding = BindingFinder.find(field);
				if (binding.equals(expressionBinding)) {
					return field;
				}
			}
		}
		return null;
	}

}
