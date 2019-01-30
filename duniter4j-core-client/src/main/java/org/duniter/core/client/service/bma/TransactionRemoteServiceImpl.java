package org.duniter.core.client.service.bma;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.base.Joiner;
import org.duniter.core.client.model.TxOutput;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.Protocol;
import org.duniter.core.client.model.bma.TxHistory;
import org.duniter.core.client.model.bma.TxSource;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.exception.InsufficientCreditException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.crypto.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


public class TransactionRemoteServiceImpl extends BaseRemoteServiceImpl implements TransactionRemoteService {

	private static final Logger log = LoggerFactory.getLogger(TransactionRemoteServiceImpl.class);

    public static final String URL_TX_BASE = "/tx";

    public static final String URL_TX_PROCESS = URL_TX_BASE + "/process";

    public static final String URL_TX_SOURCES = URL_TX_BASE + "/sources/%s";

    public static final String URL_TX_HISTORY = URL_TX_BASE + "/history/%s/blocks/%s/%s";


	private CryptoService cryptoService;

	public TransactionRemoteServiceImpl() {
		super();
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
        cryptoService = ServiceLocator.instance().getCryptoService();
	}


	public String transfer(Wallet wallet, String destPubKey, long amount,
						   String comment) throws InsufficientCreditException {
		Preconditions.checkNotNull(wallet);
		Preconditions.checkNotNull(wallet.getCurrencyId());

		return transfer(null, wallet, destPubKey, amount, comment);
	}

	public String transfer(Peer peer, Wallet wallet, String destPubKey, long amount,
						   String comment) throws InsufficientCreditException {
		Preconditions.checkNotNull(wallet);
		Preconditions.checkArgument(peer != null || wallet.getCurrencyId() != null);

		peer = peer != null ? peer : peerService.getActivePeerByCurrencyId(wallet.getCurrencyId());
		// Get current block
		BlockchainBlock currentBlock = httpService.executeRequest(peer, BlockchainRemoteServiceImpl.URL_BLOCK_CURRENT, BlockchainBlock.class);

		// http post /tx/process
		HttpPost httpPost = new HttpPost(httpService.getPath(peer, URL_TX_PROCESS));

		// compute transaction
		String transaction = getSignedTransaction(peer, wallet, currentBlock, destPubKey, 0, amount,
				comment);

		if (log.isDebugEnabled()) {
			log.debug(String.format(
					"Will send transaction document: \n------\n%s------",
					transaction));
		}

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("transaction", transaction));

		try {
			httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
		} catch (UnsupportedEncodingException e) {
			throw new TechnicalException(e);
		}

		String selfResult = httpService.executeRequest(httpPost, String.class);
		if (log.isDebugEnabled()) {
			log.debug("Received from /tx/process: " + selfResult);
		}


		String fingerprint = DigestUtils.sha1Hex(transaction);
		if (log.isDebugEnabled()) {
			log.debug(String.format(
					"Fingerprint: %s",
					fingerprint));
		}
		return fingerprint;

	}


	public TxSource getSources(Peer peer, String pubKey) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Get sources by pubKey: %s from ", pubKey, peer.toString()));
		}

		// get parameter
		String path = String.format(URL_TX_SOURCES, pubKey);
		TxSource result = httpService.executeRequest(peer, path, TxSource.class);

		return result;
	}

	public TxSource getSources(String currencyId, String pubKey) {
		return getSources(peerService.getActivePeerByCurrencyId(currencyId), pubKey);
	}

    public long getCreditOrZero(Peer peer, String pubKey) {
        Long credit = getCredit(peer, pubKey);

        if (credit == null) {
            return 0;
        }
        return credit.longValue();
    }

	public long getCreditOrZero(String currencyId, String pubKey) {
		return getCreditOrZero(peerService.getActivePeerByCurrencyId(currencyId), pubKey);
	}

    public Long getCredit(Peer peer, String pubKey) {
        if (log.isDebugEnabled()) {
			log.debug(String.format("Get credit by pubKey [%s] from peer [%s]", pubKey, peer.getUrl()));
		}

        // get parameter
        String path = String.format(URL_TX_SOURCES, pubKey);
        TxSource result = httpService.executeRequest(peer, path, TxSource.class);

        if (result == null) {
            return null;
        }

        // Compute the credit
        return computeCredit(result.getSources());
    }

	public Long getCredit(String currencyId, String pubKey) {
		return getCredit(peerService.getActivePeerByCurrencyId(currencyId), pubKey);
	}

	public long computeCredit(TxSource.Source[] sources) {
        if (CollectionUtils.isEmpty(sources)) {
            return 0;
        }

        long credit = 0;
        for (TxSource.Source source : sources) {
            credit += source.getAmount();
        }
        return credit;
    }

    public TxHistory getTxHistory(Peer peer, String pubKey, long start, long end) {
		Preconditions.checkNotNull(pubKey);
        Preconditions.checkArgument(start >= 0);
        Preconditions.checkArgument(start <= end);

        if (log.isDebugEnabled()) {
			log.debug(String.format("Get TX history by pubKey [%s], from block [%s -> %s]", pubKey, start, end));
		}

        // get parameter
        String path = String.format(URL_TX_HISTORY, pubKey, start, end);
		TxHistory result = httpService.executeRequest(peer, path, TxHistory.class);

        return result;
    }

	public TxHistory getTxHistory(String currencyId, String pubKey, long start, long end) {
		return getTxHistory(peerService.getActivePeerByCurrencyId(currencyId), pubKey, start, end);
	}

	/* -- internal methods -- */

	protected String getSignedTransaction(Peer peer,
									   Wallet wallet,
									   BlockchainBlock block,
									   String destPubKey,
									   int locktime,
									   long amount,
									   String comment) throws InsufficientCreditException {
		Preconditions.checkNotNull(wallet);
        Preconditions.checkArgument(StringUtils.isNotBlank(wallet.getCurrency()));
        Preconditions.checkArgument(StringUtils.isNotBlank(wallet.getPubKeyHash()));

		// Retrieve the wallet sources
		TxSource sourceResults = peer != null ?
				getSources(peer, wallet.getPubKeyHash()) :
				getSources(wallet.getCurrencyId(), wallet.getPubKeyHash());
		if (sourceResults == null) {
			throw new TechnicalException("Unable to load user sources.");
		}

		TxSource.Source[] sources = sourceResults.getSources();
		if (CollectionUtils.isEmpty(sources)) {
			throw new InsufficientCreditException(
					"Insufficient credit: no credit found.");
		}

		List<TxSource.Source> txInputs = new ArrayList<>();
		List<TxOutput> txOutputs = new ArrayList<>();
		computeTransactionInputsAndOuputs(block.getUnitbase(),
				wallet.getPubKeyHash(), destPubKey,
				sources, amount, txInputs, txOutputs);

		String transaction = getTransaction(wallet.getCurrency(),
				block.getNumber(), block.getHash(),
				wallet.getPubKeyHash(), locktime, txInputs, txOutputs,
				comment);

		String signature = cryptoService.sign(transaction, wallet.getSecKey());

		return new StringBuilder().append(transaction).append(signature)
				.append('\n').toString();
	}

	public String getTransaction(String currency,
								 long blockNumber,
								 String blockHash,
								 String srcPubKey,
								 int locktime,
								 List<TxSource.Source> inputs, List<TxOutput> outputs,
			String comments) {

		StringBuilder sb = new StringBuilder();
		sb.append("Version: ").append(Protocol.TX_VERSION).append("\n")
				.append("Type: ").append(Protocol.TYPE_TRANSACTION).append("\n")
				.append("Currency: ").append(currency).append('\n')
				.append("Blockstamp: ").append(blockNumber).append('-').append(blockHash).append("\n")
				.append("Locktime: ").append(locktime).append('\n')
				.append("Issuers:\n")
				// add issuer pubkey
				.append(srcPubKey).append('\n');

		// Inputs coins
		sb.append("Inputs:\n");
		Joiner joiner = Joiner.on(':');
		for (TxSource.Source input : inputs) {
			// if D : AMOUNT:BASE:D:PUBLIC_KEY:BLOCK_ID
			// if T : AMOUNT:BASE:T:T_HASH:T_INDEX
			joiner.appendTo(sb, new String[]{
					String.valueOf(input.getAmount()),
					String.valueOf(input.getBase()),
					input.getType(),
					input.getIdentifier(),
					input.getNoffset()
			});
			sb.append('\n');
		}

		// Unlocks
		sb.append("Unlocks:\n");
		for (int i = 0; i< inputs.size() ; i++) {
			// INPUT_INDEX:UNLOCK_CONDITION
			sb.append(i).append(":SIG(0)").append('\n');
		}

		// Output
		sb.append("Outputs:\n");
		for (TxOutput output : outputs) {
			// AMOUNT:BASE:CONDITIONS
			sb.append(output.getAmount()).append(':')
					.append(output.getBase()).append(':')
					.append("SIG(").append(output.getPubKey()).append(')')
					.append('\n');
		}

		// Comment
		sb.append("Comment: ");
		if (comments != null) sb.append(comments);
		sb.append('\n');

		return sb.toString();
	}

	public String getCompactTransaction(String srcPubKey,
			List<TxSource.Source> inputs, List<TxOutput> outputs,
			String comments) {

		boolean hasComment = comments != null && comments.length() > 0;
		StringBuilder sb = new StringBuilder();
		sb.append("TX:")
				// VERSION
				.append(Protocol.TX_VERSION).append(':')
				// NB_ISSUERS
				.append("1:")
				// NB_INPUTS
				.append(inputs.size()).append(':')
				// NB_OUTPUTS
				.append(outputs.size()).append(':')
				// HAS_COMMENT
				.append(hasComment ? 1 : 0).append('\n')
				// issuer pubkey
				.append(srcPubKey).append('\n');

		// Inputs coins
		for (TxSource.Source input : inputs) {
			// INDEX:SOURCE:NUMBER:FINGERPRINT:AMOUNT
			sb.append(0).append(':').append(input.getType()).append(':')
					.append(input.getIdentifier()).append(':')
					.append(input.getNoffset()).append(':')
					.append(input.getAmount()).append('\n');
		}

		// Output
		for (TxOutput output : outputs) {
			// ISSUERS:AMOUNT
			sb.append(output.getPubKey()).append(':')
					.append(output.getAmount()).append('\n');
		}

		// Comment
		if (hasComment) {
		sb.append(comments).append('\n');
		}
		return sb.toString();
	}

	public void computeTransactionInputsAndOuputs(int currentUnitBase,
												  String srcPubKey,
			String destPubKey, TxSource.Source[] sources, long amount,
			List<TxSource.Source> resultInputs, List<TxOutput> resultOutputs) throws InsufficientCreditException{

		TxInputs inputs = new TxInputs();
		inputs.amount = 0;
		inputs.minBase = currentUnitBase;
		inputs.maxBase = currentUnitBase + 1;

		// Get inputs, starting to use current base sources
		int amountBase = 0;
		while (inputs.amount < amount && amountBase <= currentUnitBase) {
			inputs = getInputs(sources, amount, currentUnitBase, currentUnitBase);

			if (inputs.amount < amount) {
				// try to reduce amount (replace last digits to zero)
				amountBase++;
				if (amountBase <= currentUnitBase) {
					amount = truncBase(amount, amountBase);
				}
			}
		}

		if (inputs.amount < amount) {
			throw new InsufficientCreditException("Insufficient credit");
		}

		// Avoid to get outputs on lower base
		if (amountBase < inputs.minBase && !isBase(amount, inputs.minBase)) {
			amount = truncBaseOrMinBase(amount, inputs.minBase);
			log.debug("TX Amount has been truncate to " + amount);
		}
		else if (amountBase > 0) {
			log.debug("TX Amount has been truncate to " + amount);
		}
		resultInputs.addAll(inputs.sources);

		long rest = amount;
		int outputBase = inputs.maxBase;
		long outputAmount;
		while(rest > 0) {
			outputAmount = truncBase(rest, outputBase);
			rest -= outputAmount;
			if (outputAmount > 0) {
				outputAmount = inversePowBase(outputAmount, outputBase);
				TxOutput output = new TxOutput();
				output.setAmount(outputAmount);
				output.setBase(outputBase);
				output.setPubKey(destPubKey);
				resultOutputs.add(output);
			}
			outputBase--;
		}
		rest = inputs.amount - amount;
		outputBase = inputs.maxBase;
		while(rest > 0) {
			outputAmount = truncBase(rest, outputBase);
			rest -= outputAmount;
			if (outputAmount > 0) {
				outputAmount = inversePowBase(outputAmount, outputBase);
				TxOutput output = new TxOutput();
				output.setAmount(outputAmount);
				output.setBase(outputBase);
				output.setPubKey(srcPubKey);
				resultOutputs.add(output);
			}
			outputBase--;
		}
	}

	private long truncBase(long amount, int base) {
		long pow = (long)Math.pow(10, base);
		if (amount < pow) return 0;
		return (long)(Math.floor(amount / pow ) * pow);
	}

	private long truncBaseOrMinBase(long amount, int base) {
		long pow = (long)Math.pow(10, base);
		if (amount < pow) return pow;
		return (long)(Math.floor(amount / pow ) * pow);
	}


	private long powBase(long amount, int base) {
		if (base <= 0) return amount;
		return (long) (amount * Math.pow(10, base));
	}

	private long inversePowBase(long amount, int base) {
		if (base <= 0) return amount;
		return (long) (amount / Math.pow(10, base));
	}

	private boolean isBase(long amount, int base) {
		if (base <= 0) return true;
		if (amount < Math.pow(10, base)) return false;
		String rest = "00000000" + amount;
		long lastDigits = Integer.parseInt(rest.substring(rest.length()-base));
		return lastDigits == 0; // no rest
	}

	private TxInputs getInputs(TxSource.Source[] availableSources, long amount, int outputBase, int filterBase) {
		if (filterBase < 0) {
			filterBase = outputBase;
		}
		long sourcesAmount = 0;
		TxInputs result = new TxInputs();
		result.minBase = filterBase;
		result.maxBase = filterBase;
		for (TxSource.Source source: availableSources) {
			if (source.getBase() == filterBase){
				sourcesAmount += powBase(source.getAmount(), source.getBase());
				result.sources.add(source);
				// Stop if enough sources
				if (sourcesAmount >= amount) {
					break;
				}
			}
		}

		// IF not enough sources, get add inputs from lower base (recursively)
		if (sourcesAmount < amount && filterBase > 0) {
			filterBase -= 1;
			long missingAmount = amount - sourcesAmount;
			TxInputs lowerInputs = getInputs(availableSources, missingAmount, outputBase, filterBase);

			// Add lower base inputs to result
			if (lowerInputs.amount > 0) {
				result.minBase = lowerInputs.minBase;
				sourcesAmount += lowerInputs.amount;
				result.sources.addAll(lowerInputs.sources);
			}
		}

		result.amount = sourcesAmount;

		return result;
	}

	private class TxInputs {
		long amount;
		int minBase;
		int maxBase;
		List<TxSource.Source> sources = new ArrayList<>();
	}

}
