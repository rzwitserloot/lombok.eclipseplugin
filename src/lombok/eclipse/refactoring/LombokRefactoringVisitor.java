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
	private final ImportRewrite importRewrite;
	private final ASTRewrite astRewrite;
	private final boolean refactorGetters;
	private final boolean refactorSetters;

	public LombokRefactoringVisitor(ASTRewrite astRewrite, ImportRewrite importRewrite, boolean refactorGetters,
			boolean refactorSetters) {
		this.astRewrite = astRewrite;
		this.importRewrite = importRewrite;
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
		String identifier = node.getName().getIdentifier();
		if (this.refactorGetters && (identifier.startsWith("get") || identifier.startsWith("is"))) {
			analzyeGetter(node);
		} else if (this.refactorSetters && identifier.startsWith("set")) {
			analyzeSetter(node);
		}

		return true;
	}

	private void analzyeGetter(MethodDeclaration node) {
		if (isGenerated(node)) {
			return;
		}

		List<ASTNode> statements = getStatements(node);

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
		if (isGenerated(node)) {
			return;
		}
		List<ASTNode> statements = getStatements(node);
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
		for (MethodDeclaration method : this.setters) {
			Assignment statement = (Assignment) ((ExpressionStatement) method.getBody().statements().get(0))
					.getExpression();
			Expression lhs = statement.getLeftHandSide();
			if (lhs instanceof FieldAccess) {
				FieldDeclaration field = checkMatch(((FieldAccess) lhs).getName());
				if (field != null) {
					Expression rightHandSide = statement.getRightHandSide();
					if (rightHandSide instanceof SimpleName) {
						String setterType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_SETTER);

						addAnnotationToField(field, setterType, method.getModifiers());
						this.astRewrite.remove(method, null);
					}
				}
			}

		}
	}

	private void refactorGetters() {
		for (MethodDeclaration method : this.getters) {
			ReturnStatement statement = (ReturnStatement) method.getBody().statements().get(0);
			Expression expression = statement.getExpression();
			FieldDeclaration field = checkMatch(expression);
			if (field != null) {
				String getterType = this.importRewrite.addImport(LombokIdentifiers.LOMBOK_GETTER);

				addAnnotationToField(field, getterType, method.getModifiers());

				this.astRewrite.remove(method, null);
			}
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
			annotation = singleAnnotation;

			final String fieldName;
			if (Modifier.isPrivate(modifiers)) {
				fieldName = LombokIdentifiers.ACCESS_LEVEL_PRIVATE;
			} else if (Modifier.isProtected(modifiers)) {
				fieldName = LombokIdentifiers.ACCESS_LEVEL_PROTECTED;
			} else {
				fieldName = LombokIdentifiers.ACCESS_LEVEL_PACKAGE;
			}
			FieldAccess newFieldAccess = field.getAST().newFieldAccess();
			newFieldAccess.setExpression(field.getAST().newSimpleName(accessLevelType));
			newFieldAccess.setName(field.getAST().newSimpleName(fieldName));
			singleAnnotation.setValue(newFieldAccess);
		}
		annotation.setTypeName(field.getAST().newName(annotationName));

		this.astRewrite.getListRewrite(field, field.getModifiersProperty()).insertFirst(annotation, null);
	}

	private FieldDeclaration checkMatch(Expression expression) {
		if (expression instanceof SimpleName) {
			SimpleName simple = (SimpleName) expression;
			IBinding expressionBinding = simple.resolveBinding();
			for (FieldDeclaration field : this.fields) {
				IBinding binding = getBinding(field);
				if (binding.equals(expressionBinding)) {
					return field;
				}
			}
		}
		return null;
	}

}
