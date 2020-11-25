package blockchain.bitcoin;

import java.util.List;

import blockchain.Block;
import blockchain.BlockchainAdapter;

/**
 * Serves as a placeholder for future implementation of a bitcoin adapter
 * 
 * @author Yannick
 *
 */
public class BitcoinAdapter extends BlockchainAdapter {

	public BitcoinAdapter(String bcNodeAddress) {
		super(bcNodeAddress);
	}

	@Override
	protected boolean connect(String bcNodeAddress) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void cleanUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public Block getBlock(String blockHash) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getNextBlocks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSynced() {
		// TODO Auto-generated method stub
		return false;
	}

}
