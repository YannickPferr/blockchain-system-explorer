package influx.schema;

import java.time.Instant;

import org.influxdb.annotation.Column;

/**
 * Abstract class that serves as a basis for an Influx measurement for
 * blockchains. Implementations should define all columns that should be stored
 * and provide getters/setters.
 * 
 * @author Yannick
 *
 */
public abstract class BlockSchema {

	// Timestamp of the block
	@Column(name = "time")
	private Instant time;

	// The hash of the block
	@Column(name = "Hash", tag = true)
	private String hash;

	// Client id which wrote this block data
	@Column(name = "Client", tag = true)
	private String client;

	// Node address this block data comes from
	@Column(name = "Node", tag = true)
	private String node;

	// The block number of this block
	@Column(name = "BlockNumber")
	private long blockNumber;

	// Marks this block with true if it is not part of the mainchain
	@Column(name = "Orphan")
	private boolean isOrphan;

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public long getBlockNumber() {
		return blockNumber;
	}

	public void setBlockNumber(long blockNumber) {
		this.blockNumber = blockNumber;
	}

	public boolean isOrphan() {
		return isOrphan;
	}

	public void setOrphan(boolean isOrphan) {
		this.isOrphan = isOrphan;
	}
}
