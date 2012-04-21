package lombok.eclipse.internal;

public final class LombokIdentifiers {

	public static final String ACCESS_LEVEL_PACKAGE = "PACKAGE";
	public static final String ACCESS_LEVEL_PROTECTED = "PROTECTED";
	public static final String ACCESS_LEVEL_PRIVATE = "PRIVATE";
	public static final String LOMBOK_ACCESS_LEVEL = "lombok.AccessLevel";
	public static final String LOMBOK_GETTER = "lombok.Getter";
	public static final String LOMBOK_SETTER = "lombok.Setter";

	private LombokIdentifiers() {
		throw new AssertionError();
	}

}
