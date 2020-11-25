package startup;

import java.util.Locale;

public enum OS {

	Windows, Mac, Linux, Other;
	
	/**
	 * Determines the operating system
	 * @return the {@link OS} that was determined
	 */
	public static OS getOS() {
		String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		if (os.contains("mac") || os.contains("darwin"))
			return OS.Mac;
		else if (os.contains("win"))
			return OS.Windows;
		else if (os.contains("nux"))
			return OS.Linux;
		else
			return OS.Other;
	}
}
