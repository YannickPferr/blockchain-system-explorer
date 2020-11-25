package client;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import blockchain.Block;
import blockchain.BlockchainAdapter;

/**
 * Dummy Adapter written to test the client functionality. It simulates
 * Blockchain behavior
 * 
 * @author Yannick
 *
 */
public class TestAdapter extends BlockchainAdapter {

	int processedBlocks = 0;
	LinkedHashMap<String, Block> blockchain;

	public TestAdapter(String bcNodeAddress) {
		super(bcNodeAddress);
	}

	@Override
	protected boolean connect(String bcNodeAddress) {
		// TODO Auto-generated method stub
		blockchain = new LinkedHashMap<>();
		return true;
	}

	@Override
	protected void cleanUp() {
		// TODO Auto-generated method stub
		blockchain = new LinkedHashMap<>();
		processedBlocks = 0;
	}

	@Override
	public Block getBlock(String blockHash) {
		// TODO Auto-generated method stub

		return blockchain.get(blockHash);
	}

	@Override
	public List<String> getNextBlocks() {
		// TODO Auto-generated method stub

		LinkedList<String> nextBlock = new LinkedList<>();
		int index = 0;
		for (String hash : blockchain.keySet()) {
			if (processedBlocks++ == index)
				nextBlock.add(hash);
			index++;
		}

		return nextBlock;
	}

	@Override
	public boolean isSynced() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Helper method that adds block to the simulated blockchain
	 * 
	 * @param block
	 *            - The block that is added
	 */
	protected void mineBlock(Block block) {
		blockchain.put(block.getHash(), block);
	}
}
