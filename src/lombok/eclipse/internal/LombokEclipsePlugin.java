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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author gransberger
 */
public class LombokEclipsePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "lombok.eclipse"; //$NON-NLS-1$

	// The shared instance
	private static LombokEclipsePlugin plugin;

	/**
	 * The constructor
	 */
	public LombokEclipsePlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static LombokEclipsePlugin getDefault() {
		return plugin;
	}
	
	public static class Logger {

		public static void error(Exception e) {
			LombokEclipsePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, LombokEclipsePlugin.PLUGIN_ID, e.getMessage(), e));
		}

		public static void error(String message, Exception e) {
			LombokEclipsePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, LombokEclipsePlugin.PLUGIN_ID, message, e));
		}

		public static void error(String message) {
			LombokEclipsePlugin.getDefault().getLog().log(new Status(IStatus.ERROR, LombokEclipsePlugin.PLUGIN_ID, message));
		}

		public static void warn(String message) {
			LombokEclipsePlugin.getDefault().getLog().log(new Status(IStatus.WARNING, LombokEclipsePlugin.PLUGIN_ID, message));
		}
		
	}

}
