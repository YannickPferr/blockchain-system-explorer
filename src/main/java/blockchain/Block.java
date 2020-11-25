package blockchain;

import java.util.List;

import org.influxdb.annotation.Measurement;

import influx.schema.BlockSchema;

/**
 * Block wrapper class for different Influx measurement schemas. Can be used for every Blockchain
 * @author Yannick
 *
 */
public class Block {

	private List<String> orphanedBlocks;
	private BlockSchema blockData;
	
	/**
	 * Constructor
	 * @param blockData - The schema of the Influx measurement for a specific Blockchain
	 * @param orphanedBlocks - A list of possible blocks that will become orphans
	 */
	public Block(BlockSchema blockData, List<String> orphanedBlocks) {
		this.blockData = blockData;
		this.orphanedBlocks = orphanedBlocks;
	}
	
	public String getHash() {
		return blockData.getHash();
	}

	public long getBlockNumber() {
		return blockData.getBlockNumber();
	}

	public long getTimestamp() {
		return blockData.getTime().getEpochSecond();
	}

	public boolean hasOrphanedBlocks() {
		if(orphanedBlocks == null || orphanedBlocks.isEmpty())
			return false;
		else 
			return true;
	}

	public List<String> getOrphanedBlocks() {
		return orphanedBlocks;
	}
	
	public BlockSchema getBlockData() {
		return blockData;
	}
	
	public Class<? extends BlockSchema> getMeasurement(){
		return blockData.getClass();
	}
	
	public String getMeasurementName() {
		Measurement measurement = blockData.getClass().getAnnotation(Measurement.class);
		return measurement.name();
	}
}
