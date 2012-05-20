package lombok.eclipse.i18n;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "lombok.eclipse.i18n.messages"; //$NON-NLS-1$
	public static String LombokRefactorAction_unsupported_element;
	public static String LombokRefactorAction_wizard_title;
	public static String LombokRefactoringWizard_title;
	public static String LombokRefactoringWizardPage_equals_hashcode_check;
	public static String LombokRefactoringWizardPage_getter_check;
	public static String LombokRefactoringWizardPage_message;
	public static String LombokRefactoringWizardPage_setter_check;
	public static String LombokRefactoringWizardPage_tostring_check;
	public static String LombokRefactoring_change_comment_element;
	public static String LombokRefactoring_change_comment_elements_title;
	public static String LombokRefactoring_change_comment_equals_hashcode;
	public static String LombokRefactoring_change_comment_getter;
	public static String LombokRefactoring_change_comment_project;
	public static String LombokRefactoring_change_comment_setter;
	public static String LombokRefactoring_change_comment_tostring;
	public static String LombokRefactoring_change_description;
	public static String LombokRefactoring_create_change_monitor_begin;
	public static String LombokRefactoring_final_condition_monitor_begin;
	public static String LombokRefactoring_initial_condition_monitor_begin;
	public static String LombokRefactoring_more_than_one_project;
	public static String LombokRefactoring_no_types;
	public static String LombokRefactoring_title;
	public static String LombokRefactoringDescriptor_not_registered;
	public static String RefactoringElement_does_not_match_type;
	public static String RefactoringElement_impossible_binary;
	public static String RefactoringElement_non_existing_type;
	public static String RefactoringElement_not_consistent;
	public static String RefactoringElement_type_name_compilation_unit;
	public static String RefactoringElement_type_name_package;
	public static String RefactoringElement_type_name_package_root;
	public static String RefactoringElement_type_name_project;
	public static String RefactoringElement_type_name_type;
	public static String RefactoringElement_unknown_structure;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
