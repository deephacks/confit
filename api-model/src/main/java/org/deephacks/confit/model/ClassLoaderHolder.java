package org.deephacks.confit.model;

/**
 * Holds an instance of {@link java.lang.ClassLoader} to be used by the system
 * to resolve the configuration classes.
 * 
 * @author <a href="mailto:lacerda@socialpro.com.br">Thiago Lacerda</a>
 */
public class ClassLoaderHolder {

	/** The class loader. */
	private static ClassLoader classLoader;

	static {
		classLoader = ClassLoader.getSystemClassLoader();
	}

	/**
	 * Sets the class loader.
	 * 
	 * @param classLoader
	 *            the new class loader
	 */
	public static void setClassLoader(ClassLoader classLoader) {
		ClassLoaderHolder.classLoader = classLoader;
	}

	/**
	 * Gets the class loader.
	 * 
	 * @return the class loader
	 */
	public static ClassLoader getClassLoader() {
		return classLoader;
	}

}
