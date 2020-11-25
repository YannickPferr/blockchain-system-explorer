package client;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import blockchain.Block;
import influx.InfluxDBUtil;
import influx.schema.BlockSchema;
import influx.schema.ethereum.EthereumSchema;
import startup.Main;
import startup.OS;

/**
 * Test that tests the functionality of the Client
 * 
 * @author Yannick
 *
 */
public class ClientTest {

	private static final String DB_NAME = "Test";
	private static final String NODE_NAME = "TestNode";
	private static final String CLIENT_NAME = "TestClient";
	private static final int POLLING_INTERVAL = 1000;
	private static final int RETRIES = 3;
	private static final int TIME_BETWEEN_RETRIES = 1000;
	private static final String INFLUX_URL = "http://localhost:8086";
	private static final String INFLUX_USER = "root";
	private static final String INFLUX_PWD = "root";

	private static final int NR_BLOCKS_TO_PRODUCE = 5;

	private static InfluxDB influxDB;
	private static TestAdapter bca;
	private static Client client;

	/**
	 * Helper method that sets up Influx
	 */
	private static void setupInflux() {
		// Connect to Influx
		influxDB = InfluxDBFactory.connect(INFLUX_URL, INFLUX_USER, INFLUX_PWD);
		System.out.println("Trying to connect to InfluxDB...");
		boolean isSuccess = InfluxDBUtil.connectToDB(influxDB, DB_NAME);
		if (isSuccess)
			System.out.println("InfluxDB already running at " + INFLUX_URL + "!");
		else {
			// If Influx connection couldn't be established ty to start it
			String cwd = System.getProperty("user.dir");
			String installDir = cwd + File.separator + "libs" + File.separator + "InfluxDB" + File.separator;

			OS os = OS.getOS();
			if (os == OS.Other) {
				System.err.println("Operating System not supported!");
				return;
			}

			boolean depExist = Main.isInfluxInstalled(installDir, os);
			if (!depExist) {
				System.out.println("InfluxDB not found, downloading...");
				if (!Main.downloadInflux(installDir, os)) {
					System.err.println("Error downloading InfluxDB!");
					return;
				}
			}
			System.out.println("Starting InfluxDB...");
			if (!Main.startInflux(installDir, os)) {
				System.err.println("Error starting InfluxDB!");
				return;
			}

			// try to connect again and retry if fails
			int retries = RETRIES;
			do {
				System.out.println("Connecting...");
				isSuccess = InfluxDBUtil.connectToDB(influxDB, DB_NAME);
				if (isSuccess)
					break;
				try {
					Thread.sleep(TIME_BETWEEN_RETRIES);
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
	}

	/**
	 * Runs once before the tests and sets up needed components
	 */
	@BeforeClass
	public static void setup() {
		setupInflux();
System.out.println("test");
		bca = new TestAdapter(NODE_NAME);
		client = new Client(CLIENT_NAME, influxDB, bca, POLLING_INTERVAL);
		client.start();
	}

	/**
	 * Runs before every tests and cleans the database
	 */
	@Before
	public void startup() {
		System.out.println("test2");
		// Make sure Database is empty
		influxDB.query(new Query("DROP DATABASE " + DB_NAME));
		influxDB.query(new Query("CREATE DATABASE " + DB_NAME));
		influxDB.setDatabase(DB_NAME);
	}

	/**
	 * Runs after every test and resets the blockchain adapter
	 */
	@After
	public void cleanup() {
		bca.cleanUp();
	}

	/**
	 * Runs once after the tests and cleans up all components
	 */
	@AfterClass
	public static void cleanUp() {
		client.stopClient();
		influxDB.close();
		Main.closeInflux();
	}

	/**
	 * Adds some randomly generated Hashes to the Blockchain and tests if the client
	 * receives and stores them correctly
	 */
	@Test
	public void testAddToDB() {
		int blocksToProduce = NR_BLOCKS_TO_PRODUCE;

		int blockNumber = 1;
		LinkedList<String> expectedHashes = new LinkedList<>();
		while (blocksToProduce > 0) {
			String hash = UUID.randomUUID().toString();

			EthereumSchema blockData = new EthereumSchema();
			blockData.setHash(hash);
			blockData.setBlockNumber(blockNumber++);
			blockData.setTime(new Date().toInstant());
			Block block = new Block(blockData, null);

			bca.mineBlock(block);
			expectedHashes.add(hash);
			blocksToProduce--;
		}

		// Wait till blocks are stored
		try {
			Thread.sleep(2 * POLLING_INTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
		Query query = new Query("SELECT * FROM Ethereum");
		QueryResult qr = influxDB.query(query);
		List<? extends BlockSchema> queryResults = resultMapper.toPOJO(qr, EthereumSchema.class);
		LinkedList<String> receivedHashes = new LinkedList<>();
		for (BlockSchema record : queryResults) {
			receivedHashes.add(record.getHash());
		}

		Assert.assertEquals(NR_BLOCKS_TO_PRODUCE, receivedHashes.size());
		Assert.assertTrue(expectedHashes.containsAll(receivedHashes) && receivedHashes.containsAll(expectedHashes));
	}

	/**
	 * Adds some randomly generated Hashes to the Blockchain and then adds a block
	 * which contains the first of the created blocks as an uncle to test if client
	 * updates the mainchain correctly
	 */
	@Test
	public void testUpdateMainchain() {
		int blocksToProduce = NR_BLOCKS_TO_PRODUCE;

		int blockNumber = 1;
		LinkedList<String> expectedHashes = new LinkedList<>();
		while (blocksToProduce > 0) {
			String hash = UUID.randomUUID().toString();

			EthereumSchema blockData = new EthereumSchema();
			blockData.setHash(hash);
			blockData.setBlockNumber(blockNumber++);
			blockData.setTime(new Date().toInstant());
			Block block = new Block(blockData, null);

			bca.mineBlock(block);
			expectedHashes.add(hash);
			blocksToProduce--;
		}

		// get block with block number 1 from list
		List<String> sublist = new LinkedList<>();
		sublist.add(expectedHashes.get(0));
		expectedHashes.remove(0);

		// add new block that has block with number 1 as uncle
		String hash = UUID.randomUUID().toString();
		EthereumSchema blockData = new EthereumSchema();
		blockData.setHash(hash);
		blockData.setBlockNumber(blockNumber++);
		blockData.setTime(new Date().toInstant());
		Block block = new Block(blockData, sublist);
		expectedHashes.add(hash);
		bca.mineBlock(block);

		// wait till blocks are stored
		try {
			Thread.sleep(2 * POLLING_INTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Query query = new Query("SELECT * FROM Ethereum");
		InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
		QueryResult qr = influxDB.query(query);
		List<? extends BlockSchema> queryResults = resultMapper.toPOJO(qr, EthereumSchema.class);

		HashMap<String, BlockSchema> mainchain = new HashMap<>();
		HashMap<String, BlockSchema> orphans = new HashMap<>();
		for (BlockSchema record : queryResults) {
			if (record.isOrphan())
				orphans.put(record.getHash(), record);
			else
				mainchain.put(record.getHash(), record);
		}

		Assert.assertEquals(NR_BLOCKS_TO_PRODUCE, mainchain.size());
		Assert.assertEquals(sublist.size(), orphans.size());
		Assert.assertTrue(sublist.containsAll(orphans.keySet()) && orphans.keySet().containsAll(sublist));
		Assert.assertTrue(
				mainchain.keySet().containsAll(expectedHashes) && expectedHashes.containsAll(mainchain.keySet()));
		Assert.assertEquals(orphans.get(sublist.get(0)).getBlockNumber(), 1);
	}
}
