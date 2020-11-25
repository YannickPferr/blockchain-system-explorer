package client;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;

import blockchain.Block;
import blockchain.BlockchainAdapter;
import influx.schema.BlockSchema;

/**
 * Connects to a Blockchain node and continuously retrieves data from it
 * 
 * @author Yannick
 *
 */
public class Client extends Thread {

	public static Logger logger = Logger.getLogger("Client");

	private final int DEFAULT_POLLING_INTERVAL = 10000;

	private String id;
	private Timer timer;
	private InfluxDB influxDB;
	private BlockchainAdapter bca;
	private int pollingInterval;

	/**
	 * Constructor
	 * 
	 * @param id
	 *            - The ID of this client
	 * @param influxDB
	 *            - An instance of a {@link InfluxDB}
	 * @param bca
	 *            - An instance of a {@link BlockchainAdapter}
	 */
	public Client(String id, InfluxDB influxDB, BlockchainAdapter bca) {

		this.id = id;
		this.influxDB = influxDB;
		this.bca = bca;
		this.pollingInterval = DEFAULT_POLLING_INTERVAL;
	}

	/**
	 * Constructor
	 * 
	 * @param id
	 *            - The ID of this client
	 * @param influxDB
	 *            - An instance of a {@link InfluxDB}
	 * @param bca
	 *            - An instance of a {@link BlockchainAdapter}
	 * @param pollingInterval
	 *            - The amount of time in milliseconds this client waits to poll for
	 *            new data
	 */
	public Client(String id, InfluxDB influxDB, BlockchainAdapter bca, int pollingInterval) {

		this.id = id;
		this.influxDB = influxDB;
		this.bca = bca;
		this.pollingInterval = pollingInterval;
	}

	/**
	 * Stops the client and closes the Blockchain connection
	 */
	public void stopClient() {
		timer.cancel();
		bca.stopConnection();
		logger.info("Client stopped!");
	}

	/**
	 * Asynchronously polls the {@link BlockchainAdapter} for new data and processes
	 * it
	 */
	@Override
	public void run() {
		logger.info(id + " started for node: " + bca.getNodeAddress() + "!");
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				// Gather Data if Blockchain Adapter is connected else stop this client
				if (bca.isConnected()) {
					// wait for sync then collect new blocks
					if (bca.isSynced())
						gatherBlockchainData(bca);
					else
						logger.info("Node is still syncing, waiting for sync to finish...");

				} else
					stopClient();
			}
		}, 0, pollingInterval);
	}

	/**
	 * Retrieves new blocks from the {@link BlockchainAdapter}, processes them and
	 * stores them in Influx
	 * 
	 * @param bca
	 *            - The {@link BlockchainAdapter} to read from
	 */
	private void gatherBlockchainData(BlockchainAdapter bca) {

		if (bca == null)
			throw new IllegalArgumentException();

		logger.debug("Polling for new blocks...");
		// Stop if error was encountered
		List<String> newBlocks = bca.getNextBlocks();
		logger.debug("Recieved " + newBlocks.size() + " new blocks!");

		for (String blockHash : newBlocks) {
			logger.debug("Processing block " + blockHash + "...");
			Block block = bca.getBlock(blockHash);
			if (block == null) {
				// TODO Possibly retry?
				logger.warn("Block data couldn't be retrieved, skipping block " + blockHash + "!");
				continue;
			}

			// Query InfluxDB to retrieve all stored blocks which are not part of the
			// mainchain anymore
			if (block.hasOrphanedBlocks()) {
				logger.debug("Block (Nr: " + block.getBlockNumber() + ", Hash: " + blockHash
						+ "): Block has orphaned blocks! Checking DB for mainchain continuity...");
				// Build the where clause: WHERE Hash=<OrphanHash1> OR Hash=<OrphanHash2> ...
				StringBuilder whereCond = new StringBuilder(" WHERE Hash=");
				for (int i = 0; i < block.getOrphanedBlocks().size() - 1; i++)
					whereCond.append("'" + block.getOrphanedBlocks().get(i) + "'" + " OR Hash=");
				whereCond.append("'" + block.getOrphanedBlocks().get(block.getOrphanedBlocks().size() - 1) + "'");

				// perform query and store results in list
				Query query = new Query("SELECT * FROM " + block.getMeasurementName() + whereCond);
				InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
				QueryResult qr = influxDB.query(query);
				List<? extends BlockSchema> queryResults = resultMapper.toPOJO(qr, block.getMeasurement());

				// iterate results and adjust all found blocks
				for (BlockSchema record : queryResults) {
					Builder builder = Point.measurement(block.getMeasurementName())
							.time(record.getTime().getEpochSecond(), TimeUnit.SECONDS).tag("Hash", record.getHash())
							.tag("Client", record.getClient()).tag("Node", record.getNode()).addField("Orphan", true);
					influxDB.write(builder.build());
					logger.debug("Found block " + record.getHash() + " and removed it from the mainchain!");
				}
				logger.debug("Mainchain updated!");
			}

			Builder builder = Point.measurement(block.getMeasurementName()).time(block.getTimestamp(), TimeUnit.SECONDS)
					.tag("Hash", block.getHash()).tag("Client", id).tag("Node", bca.getNodeAddress())
					.addField("BlockNumber", block.getBlockNumber()).addField("Orphan", false);

			builder.addFieldsFromPOJO(block.getBlockData());
			influxDB.write(builder.build());

			logger.info("Block (Nr: " + block.getBlockNumber() + ", Hash: " + blockHash + "): stored in DB!");
		}
	}
}
