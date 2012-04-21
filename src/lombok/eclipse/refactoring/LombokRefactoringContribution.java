package lombok.eclipse.refactoring;

import java.util.Map;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

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

	@Override
	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment,
			Map arguments, int flags) throws IllegalArgumentException {
		return new LombokRefactoringDescriptor(project, description, comment, arguments);
	}

	@Override
	public String getId() {
		return LombokRefactoringDescriptor.ID;
	}

	@Override
	public Map retrieveArgumentMap(RefactoringDescriptor descriptor) {
		return ((LombokRefactoringDescriptor) descriptor).getArguments();
	}

}
