package startup;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;

import blockchain.ethereum.EthereumAdapter;
import client.Client;
import influx.InfluxDBUtil;

/**
 * Main class that is called first on application start
 * 
 * @author Yannick
 *
 */
public class Main {

	// INSTALL CONSTANTS
	private final static String INFLUX_DIR = "influxdb-1.7.8-1";
	private final static String INFLUX_DL_WIN = "https://dl.influxdata.com/influxdb/releases/influxdb-1.7.8_windows_amd64.zip";
	private final static String INFLUX_DL_MAC = "https://dl.influxdata.com/influxdb/releases/influxdb-1.7.8_darwin_amd64.tar.gz";
	private final static String INFLUX_DL_LINUX = "https://dl.influxdata.com/influxdb/releases/influxdb-1.7.8_linux_amd64.tar.gz";

	// DEFAULT CONFIG VALUES
	private final static String DEFAULT_CLIENT = "DefaultClient";
	private final static String DEFAULT_INFLUX_URL = "http://localhost:8086";
	private final static String DEFAULT_ETH_NODES = "http://127.0.0.1:8545";
	private final static String DEFAULT_DB_NAME = "Blockchain";
	private final static String DEFAULT_INFLUX_USER = "root";
	private final static String DEFAULT_INFLUX_PWD = "root";
	private final static String DEFAULT_INFLUX_DUR = null;
	private final static String DEFAULT_INFLUX_REP = "1";
	private final static String DEFAULT_LOG_FILE = "config" + File.separator + "log4j.properties";
	private final static int DEFAULT_RETRIES = 10;
	private final static int DEFAULT_TIME_BETWEEN_RETRIES = 2000;

	private static Logger processLogger = Logger.getLogger("Process");
	private static Process influxProcess;

	/**
	 * Checks if InfluxDB is installed
	 * 
	 * @param installDir
	 *            - The path were Influx should be installed
	 * @param os
	 *            - The operating system of the user
	 * @return <code>true</code> if installed otehrwise <code>false</code>
	 */
	public static boolean isInfluxInstalled(String installDir, OS os) {
		String startFile = "";
		switch (os) {
		case Windows:
			startFile = installDir + INFLUX_DIR + File.separator + "influxd.exe";
			break;
		case Mac:
			startFile = installDir + INFLUX_DIR + File.separator + "usr" + File.separator + "bin" + File.separator
					+ "influxd";
			break;
		case Linux:
			startFile = installDir + INFLUX_DIR + File.separator + "usr" + File.separator + "bin" + File.separator
					+ "influxd";
			break;
		default:
			return false;
		}

		if (new File(startFile).exists())
			return true;

		return false;
	}

	/**
	 * Downloads Influx
	 * 
	 * @param installDir
	 *            - The path were Influx should be installed
	 * @param os
	 *            - The operating system of the user
	 * @return <code>true</code> if download was successful otherwise
	 *         <code>false</code>
	 */
	public static boolean downloadInflux(String installDir, OS os) {

		String dl = "";

		switch (os) {
		case Windows:
			dl = INFLUX_DL_WIN;
			break;
		case Mac:
			dl = INFLUX_DL_MAC;
			break;
		case Linux:
			dl = INFLUX_DL_LINUX;
			break;
		default:
			return false;
		}

		try {
			new File(installDir).mkdirs();
			InputStream inputStream = new URL(dl).openStream();
			InputStream file = new BufferedInputStream(inputStream);

			ArchiveInputStream input = null;
			if (dl.endsWith("zip"))
				input = new ZipArchiveInputStream(file);
			else if (dl.endsWith("tar.gz")) {
				InputStream gzi = new GzipCompressorInputStream(file);
				input = new TarArchiveInputStream(gzi);
			}
			extractArchive(input, installDir);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * Starts Influx
	 * 
	 * @param installDir
	 *            - The path were Influx should be installed
	 * @param os
	 *            - The operating system of the user
	 * @return <code>true</code> if Influx was started otherwise <code>false</code>
	 */
	public static boolean startInflux(String installDir, OS os) {
		final String startFile;
		switch (os) {
		case Windows:
			startFile = installDir + INFLUX_DIR + File.separator + "influxd.exe";
			break;
		case Mac:
			startFile = installDir + INFLUX_DIR + File.separator + "usr" + File.separator + "bin" + File.separator
					+ "influxd";
			break;
		case Linux:
			startFile = installDir + INFLUX_DIR + File.separator + "usr" + File.separator + "bin" + File.separator
					+ "influxd";
			break;
		default:
			return false;
		}

		// Make file executable
		new File(startFile).setExecutable(true);
		// start Influx in the background and log its output messages
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					influxProcess = new ProcessBuilder(startFile).redirectErrorStream(true).start();
					BufferedReader br = new BufferedReader(new InputStreamReader(influxProcess.getInputStream()));
					String line = null;
					while ((line = br.readLine()) != null)
						processLogger.info(line);
				} catch (IOException e) {
					processLogger.error("Error starting process!", e);
				}
			}
		});
		t.start();

		return t.isAlive();
	}

	/**
	 * Closes Influx
	 */
	public static void closeInflux() {
		if (influxProcess != null)
			influxProcess.destroy();
	}

	/**
	 * Extracts an archive file to the specified location
	 * 
	 * @param i
	 *            - The {@link ArchiveInputStream}
	 * @param targetDir
	 *            - The directory where the files should be extracted to
	 * @throws IOException
	 */
	private static void extractArchive(ArchiveInputStream i, String targetDir) throws IOException {
		ArchiveEntry entry = null;
		while ((entry = i.getNextEntry()) != null) {
			if (!i.canReadEntryData(entry)) {
				continue;
			}
			File f = new File(targetDir + entry.getName());
			if (entry.isDirectory()) {
				if (!f.isDirectory() && !f.mkdirs()) {
					throw new IOException("failed to create directory " + f);
				}
			} else {
				File parent = f.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("failed to create directory " + parent);
				}
				try (OutputStream o = Files.newOutputStream(f.toPath())) {
					IOUtils.copy(i, o);
				}
			}
		}
	}

	public static void main(String[] args) {
		// Default args needed to start
		String clientName = DEFAULT_CLIENT;
		String influxURL = DEFAULT_INFLUX_URL;
		List<String> ethNodes = new ArrayList<>();
		ethNodes.add(DEFAULT_ETH_NODES);
		String dbName = DEFAULT_DB_NAME;
		String influxUser = DEFAULT_INFLUX_USER;
		String influxPwd = DEFAULT_INFLUX_PWD;
		String dur = DEFAULT_INFLUX_DUR;
		String rep = DEFAULT_INFLUX_REP;
		String logFile = DEFAULT_LOG_FILE;
		int retries = DEFAULT_RETRIES;
		long timeBetweenRetries = DEFAULT_TIME_BETWEEN_RETRIES;
		String configType = "default";

		Options options = new Options();
		// config option
		Option configFile = Option.builder("c").longOpt("config").argName("file").hasArg()
				.desc("Specify location of config file").build();
		options.addOption(configFile);
		// cli mode
		Option cliMode = Option.builder("p").longOpt("props").argName("property=value").hasArgs()
				.desc("Specify the value of a property").build();
		options.addOption(cliMode);
		// multi node support
		Option multiNode = Option.builder("e").longOpt("ethNodes").argName("ethereum node addresses")
				.desc("Specify all ethereum nodes you want to connect to").numberOfArgs(Option.UNLIMITED_VALUES)
				.valueSeparator(',').build();
		options.addOption(multiNode);
		// help flag for to print help info
		Option help = Option.builder("h").longOpt("help").desc("Help flag to print usage message").build();
		options.addOption(help);

		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args, false);

			if (cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("BlockchainSystemExplorerClient", options);
			}

			if (cmd.hasOption("config")) {
				String filename = cmd.getOptionValue("config", "BlockchainSystemExplorer.properties");
				System.out.println("Starting with config file: " + filename);
				Configurations configs = new Configurations();
				try {
					Configuration config = configs.properties(new File(filename));
					clientName = config.getString("ClientName", "DefaultClient");
					influxURL = config.getString("InfluxURL", "http://localhost:8086");
					ethNodes = config.getList(String.class, "EthNodes");
					dbName = config.getString("DBName", "Blockchain");
					influxUser = config.getString("InfluxUser", "root");
					influxPwd = config.getString("InfluxPassword", "root");
					dur = config.getString("InfluxRetentionDuration");
					rep = config.getString("InfluxRetentionReplication", "1");
					logFile = config.getString("Log4jConfig", "config" + File.separator + "log4j.properties");
					retries = config.getInt("Retries", 10);
					timeBetweenRetries = config.getLong("TimeBetweenRetries", 2000);
				} catch (ConfigurationException e) {
					System.err.println("Error loading config!");
					e.printStackTrace();
					return;
				}
			} else {
				if (cmd.hasOption("props")) {
					configType = "custom";
					Properties props = cmd.getOptionProperties("props");
					clientName = props.getProperty("ClientName", "DefaultClient");
					influxURL = props.getProperty("InfluxURL", "http://localhost:8086");
					dbName = props.getProperty("DBName", "Blockchain");
					influxUser = props.getProperty("InfluxUser", "root");
					influxPwd = props.getProperty("InfluxPassword", "root");
					dur = props.getProperty("InfluxRetentionDuration");
					rep = props.getProperty("InfluxRetentionReplication", "1");
					logFile = props.getProperty("Log4jConfig", "config" + File.separator + "log4j.properties");
					retries = Integer.parseInt(props.getProperty("Retries", "10"));
					timeBetweenRetries = Long.parseLong(props.getProperty("TimeBetweenRetries", "2000"));
				}
				if (cmd.hasOption("ethNodes")) {
					configType = "custom";
					ethNodes = Arrays.asList(cmd.getOptionValues("ethNodes"));
				}

				System.out.println("Starting with " + configType + " configuration!");
			}
		} catch (ParseException e) {
			System.err.println("Error parsing command line args!");
			e.printStackTrace();
			return;
		}

		PropertyConfigurator.configure(logFile);

		// Connect to Influx
		InfluxDB influxDB = InfluxDBFactory.connect(influxURL, influxUser, influxPwd);
		System.out.println("Trying to connect to InfluxDB...");
		boolean isSuccess = InfluxDBUtil.connectToDB(influxDB, dbName);
		if (isSuccess)
			System.out.println("InfluxDB already running at " + influxURL + "!");
		else {
			// If Influx connection couldn't be established and Influx URL is default value,
			// try to start it
			if (influxURL.equals(Main.DEFAULT_INFLUX_URL)) {
				String cwd = System.getProperty("user.dir");
				String installDir = cwd + File.separator + "libs" + File.separator + "InfluxDB" + File.separator;

				OS os = OS.getOS();
				if (os == OS.Other) {
					System.err.println("Operating System not supported!");
					return;
				}

				boolean depExist = isInfluxInstalled(installDir, os);
				if (!depExist) {
					System.out.println("InfluxDB not found, downloading...");
					if (!downloadInflux(installDir, os)) {
						System.err.println("Error downloading InfluxDB!");
						return;
					}
				}
				System.out.println("Starting InfluxDB...");
				if (!startInflux(installDir, os)) {
					System.err.println("Error starting InfluxDB!");
					return;
				}
			}

			// try to connect again and retry if fails
			do {
				System.out.println("Connecting...");
				isSuccess = InfluxDBUtil.connectToDB(influxDB, dbName);
				if (isSuccess)
					break;
				try {
					Thread.sleep(timeBetweenRetries);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				retries--;
				if (retries == 0) {
					System.err.println("InfluxDB connection couldn't be established!");
					return;
				}
			} while (retries > 0);
		}
		System.out.println("InfluxDB connection established!");

		// if retention policy is setup
		if (dur != null) {
			String rpName = dbName + "_RP";
			influxDB.query(new Query("CREATE RETENTION POLICY " + rpName + " ON " + dbName + " DURATION " + dur
					+ " REPLICATION " + rep + " DEFAULT"));
			influxDB.setRetentionPolicy(rpName);
		}

		LinkedList<Client> clients = new LinkedList<>();
		for (String bcNode : ethNodes) {
			// start client
			EthereumAdapter bca = new EthereumAdapter(bcNode.trim());

			// if connection failed skip node
			if (!bca.isConnected())
				continue;

			Client client = new Client(clientName, influxDB, bca);
			clients.add(client);
			client.start();
		}

		if (!clients.isEmpty())
			System.out.println("Type in any Influx Query to export data! q to quit.");
		// end client when q is typed in console
		Scanner sc = new Scanner(System.in);
		while (!clients.isEmpty()) {
			String input = sc.nextLine();
			if (input.equals("q")) {
				System.out.println("Shutdown request received!");
				for (Client client : clients)
					client.stopClient();
				break;
			} else
				InfluxDBUtil.exportData(influxDB, input);
		}

		System.out.println("Shutting down...");
		influxDB.close();
		closeInflux();
		sc.close();
	}
}
