package influx.schema.ethereum;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import influx.schema.BlockSchema;

/**
 * Specific implementation of an Influx measurement for Ethereum
 * 
 * @author Yannick
 *
 */
@Measurement(name = "Ethereum")
public class EthereumSchema extends BlockSchema {

	@Column(name = "BlockCreationTime")
	private long blockCreationTime;

	@Column(name = "Difficulty")
	private long difficulty;

	@Column(name = "ExtraData")
	private String extraData;

	@Column(name = "GasLimit")
	private long gasLimit;

	@Column(name = "GasUsed")
	private long gasUsed;

	@Column(name = "Miner")
	private String miner;

	@Column(name = "PeerCount")
	private long peerCount;

	@Column(name = "Size")
	private long size;

	@Column(name = "TotalDifficulty")
	private String totalDifficulty;

	@Column(name = "Transactions")
	private long transactions;

	@Column(name = "Uncles")
	private long uncles;

	public long getBlockCreationTime() {
		return blockCreationTime;
	}

	public void setBlockCreationTime(long blockCreationTime) {
		this.blockCreationTime = blockCreationTime;
	}

	public long getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(long difficulty) {
		this.difficulty = difficulty;
	}

	public String getExtraData() {
		return extraData;
	}

	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}

	public long getGasLimit() {
		return gasLimit;
	}

	public void setGasLimit(long gasLimit) {
		this.gasLimit = gasLimit;
	}

	public long getGasUsed() {
		return gasUsed;
	}

	public void setGasUsed(long gasUsed) {
		this.gasUsed = gasUsed;
	}

	public String getMiner() {
		return miner;
	}

	public void setMiner(String miner) {
		this.miner = miner;
	}

	public long getPeerCount() {
		return peerCount;
	}

	public void setPeerCount(long peerCount) {
		this.peerCount = peerCount;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getTotalDifficulty() {
		return totalDifficulty;
	}

	public void setTotalDifficulty(String totalDifficulty) {
		this.totalDifficulty = totalDifficulty;
	}

	public long getTransactions() {
		return transactions;
	}

	public void setTransactions(long transactions) {
		this.transactions = transactions;
	}

	public long getUncles() {
		return uncles;
	}

	public void setUncles(long uncles) {
		this.uncles = uncles;
	}
}
