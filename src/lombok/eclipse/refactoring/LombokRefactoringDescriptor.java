package lombok.eclipse.refactoring;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class LombokRefactoringDescriptor extends JavaRefactoringDescriptor {

	public static final String ID = "lombok.refactoring.test";

	public LombokRefactoringDescriptor() {
		super(ID);
	}

	public LombokRefactoringDescriptor(String project, String description,
			String comment, Map arguments) {
		super(ID, project, description, comment, arguments, 0);
	}

	@Override
	protected Map getArguments() {
		return super.getArguments();
	}

}