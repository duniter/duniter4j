package org.duniter.core.client.model.bma;

/*-
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 EIS
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

import org.duniter.core.client.model.exception.InvalidFormatException;
import org.duniter.core.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by blavenie on 26/04/17.
 */
public final class BlockchainBlocks {

    public static final Pattern SIG_PUBKEY_PATTERN = Pattern.compile("SIG\\(([^)]+)\\)");

    public static final Pattern TX_INPUT_CONDITION_FUNCTION = Pattern.compile("(SIG|XHX)\\(([^)]+)\\)");
    public static final Pattern TX_INPUT_CONDITION = Pattern.compile(TX_INPUT_CONDITION_FUNCTION + "(:? " + TX_INPUT_CONDITION_FUNCTION + ")*");

    public static final Pattern TX_UNLOCK_PATTERN = Pattern.compile("([0-9]+):(" + TX_INPUT_CONDITION+")");
    public static final Pattern TX_OUTPUT_PATTERN = Pattern.compile("([0-9]+):([0-9]+):([^:]+)");
    public static final Pattern TX_INPUT_PATTERN = Pattern.compile("([0-9]+):([0-9]+):([TD]):([^:]+):([^:]+)");

    private BlockchainBlocks() {
        // helper class
    }

    public static BigInteger getTxAmount(BlockchainBlock block) {
        BigInteger result = BigInteger.valueOf(0l);
        Arrays.stream(block.getTransactions())
                .forEach(tx -> result.add(BigInteger.valueOf(getTxAmount(tx))));
        return result;
    }

    public static long getTxAmount(final BlockchainBlock.Transaction tx) {
        return getTxAmount(tx, null/*no issuer filter*/);
    }

    public static long getTxAmount(final BlockchainBlock.Transaction tx,
                                   Predicate<String> issuerFilter) {

        final Map<Integer, List<String>> inputIssuers = getInputIssuers(tx);

        return IntStream.range(0, tx.getIssuers().length)
                .mapToLong(issuerIndex -> {
                    final String issuer = tx.getIssuers()[issuerIndex];

                    // Skip if issuerFilter test failed
                    if (issuerFilter != null && !issuerFilter.test(issuer)) return 0;

                    long inputSum = IntStream.range(0, tx.getInputs().length)
                            .filter(inputIssuers::containsKey)
                            .mapToLong(inputIndex -> {
                                String[] inputParts = tx.getInputs()[inputIndex].split(":");
                                List<String> issuers = inputIssuers.get(inputIndex);
                                if (inputParts.length > 2 && issuers.contains(issuer)) {
                                    return powBase(Long.parseLong(inputParts[0]), Integer.parseInt(inputParts[1]), issuers.size());
                                }
                                return 0;
                            })
                            .sum();

                    long outputSum = Arrays.stream(tx.getOutputs())
                            .mapToLong(outputStr -> {
                                try {
                                    TxOutput txOutput = parseOutput(outputStr);
                                    if (issuer.equals(txOutput.recipient)) {
                                        return powBase(txOutput.amount, txOutput.unitbase);
                                    }
                                }
                                catch (InvalidFormatException e) {
                                    // not a simple unlock condition
                                }
                                return 0;
                            })
                            .sum();

                    return (inputSum - outputSum);
                })
                .sum();
    }

    public static long powBase(long amount, int unitbase) {
        if (unitbase == 0) return amount;
        return amount * (long)Math.pow(10, unitbase);
    }

    public static long powBase(long amount, int unitbase, int divisor) {
        if (unitbase == 0) return amount;
        return amount * (long)Math.pow(10, unitbase) / divisor;
    }

    public static List<TxInput> getTxInputs(final BlockchainBlock.Transaction tx) {
        Preconditions.checkNotNull(tx);

        final Map<Integer, List<String>> inputIssuers = getInputIssuers(tx);
        if (inputIssuers == null || inputIssuers.size() == 0) {
            throw new InvalidFormatException("No issuer found in TX: " + tx.toString());
        }

        return IntStream.range(0, tx.getInputs().length)
                .mapToObj(i -> {
                    TxInput txInput = parseInput(tx.getInputs()[i]);
                    if (txInput == null) {
                        throw new InvalidFormatException("Unable to parse TX inputs: " + tx.getInputs()[i]);
                    }
                    txInput.issuers = inputIssuers.get(i);
                    return txInput;
                })
                .collect(Collectors.toList());
    }

    public static List<TxOutput> getTxOutputs(final BlockchainBlock.Transaction tx) {
        Preconditions.checkNotNull(tx);
        return Arrays.stream(tx.getOutputs())
                .map(output -> parseOutput(output))
                .collect(Collectors.toList());
    }

    public static TxInput parseInput(String input) {
        TxInput result = null;
        Matcher matcher = TX_INPUT_PATTERN.matcher(input);
        if (matcher.matches()) {
            result = new TxInput();
            result.amount = Long.parseLong(matcher.group(1));
            result.unitbase = Integer.parseInt(matcher.group(2));
            result.type = matcher.group(3);
            result.txHashOrPubkey = matcher.group(4);
            result.indexOrBlockId = matcher.group(5);
        }
        return result;
    }

    public static TxOutput parseOutput(String output) {
        TxOutput result = null;
        Matcher matcher = TX_OUTPUT_PATTERN.matcher(output);
        if (matcher.matches()) {
            result = new TxOutput();
            result.amount = Long.parseLong(matcher.group(1));
            result.unitbase = Integer.parseInt(matcher.group(2));
            result.unlockCondition = matcher.group(3);

            // Parse unlock condition like 'SIG(<pubkey>)'
            matcher = SIG_PUBKEY_PATTERN.matcher(result.unlockCondition);
            if (matcher.matches()) {
                result.recipient = matcher.group(1);
            }
        }
        else {
            throw new InvalidFormatException("Unable to parse TX output: " + output);
        }
        return result;
    }

    public static long getTxInputAmountByIssuer(final List<TxInput> txInputs, final String issuer) {
        Preconditions.checkNotNull(txInputs);
        return txInputs.stream()
                // only keep inputs from issuer
                .filter(input -> input.issuers.contains(issuer))
                .mapToLong(input -> powBase(input.amount, input.unitbase, input.issuers.size()))
                .sum();
    }

    public static long getTxOutputAmountByIssuerAndRecipient(final List<BlockchainBlocks.TxOutput> txOutputs,
                                                             final String issuer,
                                                             final String recipient) {
        Preconditions.checkNotNull(txOutputs);
        return txOutputs.stream()
                // only keep the expected recipient, but not equals to the issuer
                .filter(output -> Objects.equals(recipient, output.recipient) && !Objects.equals(issuer, output.recipient))
                .mapToLong(output -> powBase(output.amount, output.unitbase))
                .sum();
    }

    public static Set<String> getTxRecipients(Collection<TxOutput> txOutputs) {
        Preconditions.checkNotNull(txOutputs);
        return txOutputs.stream()
                .map(output -> {
                    if (output == null) {
                        throw new InvalidFormatException("TX outputs should not be null");
                    }
                    return output;
                })
                .map(output -> output.recipient)
                .filter(Objects::nonNull)
                .distinct().collect(Collectors.toSet());
    }

    public static String buid(BlockchainBlock block) {
        if (block == null || block.getNumber() == null || block.getHash() == null) return null;
        return block.getNumber() + "-" + block.getHash();
    }

    public static class TxInput {
        public long amount;
        public int unitbase;
        public String type;
        public String txHashOrPubkey;
        public String indexOrBlockId;
        public List<String> issuers;

        public boolean isUD() {
            return "D".equals(type);
        }
    }

    public static class TxOutput {
        public long amount;
        public int unitbase;
        public String recipient;
        public String unlockCondition;
    }

    /* -- Internal methods -- */


    private static Map<Integer, List<String>> getInputIssuers(final BlockchainBlock.Transaction tx) {
        return Arrays.stream(tx.getUnlocks())
                .map(TX_UNLOCK_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(
                        matcher -> Integer.decode(matcher.group(1)),
                        matcher -> getUnlockConditionIssuers(tx.getIssuers(), matcher.group(2)))
                );
    }

    private static List<String> getUnlockConditionIssuers(String[] issuers, String condition) {
        // parse condition
        Matcher matcher = TX_INPUT_CONDITION_FUNCTION.matcher(condition);
        int start = 0;
        List<String> result = new ArrayList<>(1);
        while (matcher.find(start)) {
            String function = matcher.group(1);
            if ("SIG".equals(function)) {
                int issuerIndex = Integer.parseInt(matcher.group(2));
                String issuer = issuers[issuerIndex];
                result.add(issuer);
            }
            start = matcher.end();
        }
        return result;
    }
}
