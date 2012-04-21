package lombok.eclipse.refactoring;

import java.lang.reflect.Field;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

public final class ASTUtils {

	private ASTUtils() {
		throw new AssertionError();
	}

	public static IBinding getBinding(ASTNode node) {
		BindingFinder finder = new BindingFinder();
		node.accept(finder);
		return finder.getBinding();
	}

	public static List<ASTNode> getStatements(MethodDeclaration node) {
		@SuppressWarnings("unchecked")
		List<ASTNode> statements = node.getBody().statements();
		return statements;
	}

	private static Field generatedByField;

	static {
		try {
			generatedByField = ASTNode.class.getDeclaredField("$isGenerated");
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
