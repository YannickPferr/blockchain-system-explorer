package blockchain;

import java.util.List;

/**
 * Template class for different Blockchain adapters
 * 
 * @author Yannick
 *
 */
public abstract class BlockchainAdapter {

	private String bcNodeAddress;
	private boolean isConnected;

	/**
	 * Constructor tries to establish a connection
	 * 
	 * @param bcNodeAddress
	 *            - The blockchain node to connect to
	 */
	public BlockchainAdapter(String bcNodeAddress) {
		// TODO Auto-generated constructor stub
		this.bcNodeAddress = bcNodeAddress;
		isConnected = connect(bcNodeAddress);
	}

	/**
	 * Connects to the provided node address. Gets called when object is created
	 * 
	 * @param bcNodeAddress
	 *            - The blockchain node to connect to
	 * @return <code>true</code> if connection was successful otherwise
	 *         <code>false</code>
	 */
	protected abstract boolean connect(String bcNodeAddress);

	/**
	 * Closes all opened resources. Gets called when connection is closed
	 */
	protected abstract void cleanUp();

	/**
	 * Retrieves all block data for the given block hash
	 * 
	 * @param blockHash
	 *            - The hash of the block to be retrieved
	 * @return The requested {@link Block} or <code>null</code> if data couldn't be
	 *         retrieved
	 */
	public abstract Block getBlock(String blockHash);

	/**
	 * Retrieves all hashes of new blocks since the last request
	 * 
	 * @return A {@link List} of {@link String} which contains all new block hashes
	 */
	public abstract List<String> getNextBlocks();

	/**
	 * Returns the connection status of the node
	 * 
	 * @return <code>true</code> if connection is alive otherwise <code>false</code>
	 */
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * Returns the synchronization status of the node
	 * 
	 * @return <code>true</code> if sync is finished, <code>false</code> if node is
	 *         currently syncing
	 */
	public abstract boolean isSynced();

	/**
	 * Stops the connection
	 */
	public void stopConnection() {
		isConnected = false;
		cleanUp();
	}

	/**
	 * Returns the address of the connected node
	 * 
	 * @return The address of the node this adapter is connected to
	 */
	public String getNodeAddress() {
		return bcNodeAddress;
	}
}
