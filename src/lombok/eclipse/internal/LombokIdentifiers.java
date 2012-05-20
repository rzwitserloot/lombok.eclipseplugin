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
package lombok.eclipse.internal;

public final class LombokIdentifiers {

	//TODO change to use Lombok Project
	public static final String ACCESS_LEVEL_PACKAGE = "PACKAGE"; //$NON-NLS-1$
	public static final String ACCESS_LEVEL_PROTECTED = "PROTECTED"; //$NON-NLS-1$
	public static final String ACCESS_LEVEL_PRIVATE = "PRIVATE"; //$NON-NLS-1$
	public static final String LOMBOK_ACCESS_LEVEL = "lombok.AccessLevel"; //$NON-NLS-1$
	public static final String LOMBOK_GETTER = "lombok.Getter"; //$NON-NLS-1$
	public static final String LOMBOK_SETTER = "lombok.Setter"; //$NON-NLS-1$
	public static final String LOMBOK_EQUALS_HASHCODE = "lombok.EqualsAndHashCode"; //$NON-NLS-1$
	public static final String LOMBOK_TOSTRING = "lombok.ToString"; //$NON-NLS-1$

	private LombokIdentifiers() {
		throw new AssertionError();
	}

}
