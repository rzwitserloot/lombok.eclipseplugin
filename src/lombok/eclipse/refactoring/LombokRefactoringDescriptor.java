package lombok.eclipse.refactoring;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

public class LombokRefactoringDescriptor extends JavaRefactoringDescriptor {

	public static final String ID = "lombok.eclipse.refactoring.tolombok";

	public static final String ATTRIBUTE_PROJECT = "project";
	public static final String ATTRIBUTE_REFACTOR_GETTERS = "getters";
	public static final String ATTRIBUTE_REFACTOR_SETTERS = "setters";
	public static final String ATTRIBUTE_ELEMENTS = "elements";

	public LombokRefactoringDescriptor() {
		super(ID);
	}

	public LombokRefactoringDescriptor(String project, String description, String comment, Map arguments) {
		super(ID, project, description, comment, arguments, RefactoringDescriptor.NONE);
	}

	@Override
	protected Map getArguments() {
		return super.getArguments();
	}

}