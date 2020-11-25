package blockchain.ethereum;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedList;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;
import org.web3j.protocol.http.HttpService;

import blockchain.Block;
import blockchain.BlockchainAdapter;
import client.Client;
import influx.schema.ethereum.EthereumSchema;

/**
 * Ethereum implementation of {@link BlockchainAdapter} class
 * 
 * @author Yannick
 *
 */
public class EthereumAdapter extends BlockchainAdapter {
	private Web3j web3;

	private BigInteger filterId;

	public EthereumAdapter(String bcNodeAddress) {
		super(bcNodeAddress);
	}

	@Override
	protected boolean connect(String bcNodeAddress) {
		try {
			web3 = Web3j.build(new HttpService(bcNodeAddress));
			Client.logger.info(
					"Connected to ethereum client version: " + web3.web3ClientVersion().send().getWeb3ClientVersion());
			return true;
		} catch (Exception e) {
			Client.logger.error("Couldn't connect to ethereum node at address: " + bcNodeAddress);
		}
		return false;
	}

	@Override
	protected void cleanUp() {
		// TODO Auto-generated method stub
		web3.shutdown();
	}

	@Override
	public Block getBlock(String blockHash) {
		// TODO Auto-generated method stub
		if (!isConnected()) {
			Client.logger.error("Not connected to ethereum node!");
			return null;
		}

		Block block = null;
		try {
			EthBlock.Block ethBlock = web3.ethGetBlockByHash(blockHash, false).send().getBlock();

			BigInteger peerCount = web3.netPeerCount().send().getQuantity();
			// Block with specified number doesn't exist
			if (ethBlock == null)
				return block;
			EthBlock.Block parentBlock = web3.ethGetBlockByHash(ethBlock.getParentHash(), false).send().getBlock();

			// Calculate block creation time
			BigInteger creationTime = ethBlock.getTimestamp().subtract(parentBlock.getTimestamp());

			// Set block data
			EthereumSchema blockData = new EthereumSchema();
			blockData.setTime(Instant.ofEpochSecond(ethBlock.getTimestamp().longValue()));
			blockData.setHash(blockHash);
			blockData.setBlockNumber(ethBlock.getNumber().longValue());
			blockData.setBlockCreationTime(creationTime.longValue());
			blockData.setPeerCount(peerCount.longValue());
			blockData.setDifficulty(ethBlock.getDifficulty().longValue());
			blockData.setGasUsed(ethBlock.getGasUsed().longValue());
			blockData.setGasLimit(ethBlock.getGasLimit().longValue());
			blockData.setMiner(ethBlock.getMiner());
			blockData.setExtraData(ethBlock.getExtraData());
			blockData.setTotalDifficulty(ethBlock.getTotalDifficulty().toString());
			blockData.setTransactions(ethBlock.getTransactions().size());
			blockData.setSize(ethBlock.getSize().longValue());
			blockData.setUncles(ethBlock.getUncles().size());

			block = new Block(blockData, ethBlock.getUncles());
		} catch (IOException e) {
			Client.logger.error("Error getting Block data for Block: " + blockHash + "!", e);
		}

		return block;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public LinkedList<String> getNextBlocks() {
		// TODO Auto-generated method stub
		if (!isConnected()) {
			Client.logger.error("No connection to an ethereum node was established!");
			throw new IllegalStateException("No connection to an ethereum node was established!");
		}

		LinkedList<String> newBlocks = new LinkedList<>();
		try {
			if (filterId == null)
				filterId = web3.ethNewBlockFilter().send().getFilterId();
			for (LogResult log : web3.ethGetFilterChanges(filterId).send().getLogs()) {
				String hash = ((EthLog.Hash) log).get();
				newBlocks.add(hash);
			}
		} catch (IOException e) {
			Client.logger.error("Error retrieving new blocks from filter!", e);
		}

		return newBlocks;
	}

	@Override
	public boolean isSynced() {
		boolean isSynced = true;
		try {
			isSynced = !web3.ethSyncing().send().isSyncing();
		} catch (IOException e) {
			Client.logger.error("Error retrieving sync status!", e);
		}
		
		return isSynced;
	}
}
