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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
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

    public static final Pattern TX_UNLOCK_PATTERN = Pattern.compile("([0-9]+):SIG\\(([^)]+)\\)");
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

        final Map<Integer, Integer> inputIndexByIssuerIndex = Maps.newHashMap();
        Arrays.stream(tx.getUnlocks())
                .map(TX_UNLOCK_PATTERN::matcher)
                .filter(Matcher::matches)
                .forEach(matcher -> inputIndexByIssuerIndex.put(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)))
                );

        return IntStream.range(0, tx.getIssuers().length)
                .mapToLong(i -> {
                    final String issuer = tx.getIssuers()[i];

                    // Skip if issuerFilter test failed
                    if (issuerFilter != null && !issuerFilter.test(issuer)) return 0;

                    long inputSum = IntStream.range(0, tx.getInputs().length)
                            .filter(j -> i == inputIndexByIssuerIndex.get(j))
                            .mapToObj(j -> tx.getInputs()[j])
                            .map(input -> input.split(":"))
                            .filter(inputParts -> inputParts.length > 2)
                            .mapToLong(inputParts -> powBase(Long.parseLong(inputParts[0]), Integer.parseInt(inputParts[1])))
                            .sum();

                    long outputSum = Arrays.stream(tx.getOutputs())
                            .map(TX_OUTPUT_PATTERN::matcher)
                            .filter(Matcher::matches)
                            .filter(matcher -> issuer.equals(matcher.group(3)))
                            .mapToLong(matcher -> powBase(Long.parseLong(matcher.group(1)), Integer.parseInt(matcher.group(2))))
                            .sum();

                    return (inputSum - outputSum);
                })
                .sum();
    }

    public static long powBase(long amount, int unitbase) {
        if (unitbase == 0) return amount;
        return amount * (long)Math.pow(10, unitbase);
    }

    public static List<TxInput> getTxInputs(final BlockchainBlock.Transaction tx) {
        Preconditions.checkNotNull(tx);

        final Function<Integer, String> issuerByInputIndex = transformInputIndex2Issuer(tx);

        return IntStream.range(0, tx.getInputs().length)
                .mapToObj(i -> {
                    TxInput txInput = parseInput(tx.getInputs()[i]);
                    txInput.issuer = issuerByInputIndex.apply(i);
                    return txInput;
                })
                .collect(Collectors.toList());
    }

    public static List<TxOutput> getTxOutputs(final BlockchainBlock.Transaction tx) {
        Preconditions.checkNotNull(tx);
        return Arrays.stream(tx.getOutputs())
                .map(output -> parseOuput(output))
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

    public static TxOutput parseOuput(String output) {
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
        return result;
    }

    public static long getTxInputAmountByIssuer(final List<TxInput> txInputs, final String issuer) {
        Preconditions.checkNotNull(txInputs);
        return txInputs.stream()
                // only keep inputs from issuer
                .filter(input -> Objects.equals(issuer, input.issuer))
                .mapToLong(input -> powBase(input.amount, input.unitbase))
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
        return txOutputs.stream().map(output -> output.recipient).distinct().collect(Collectors.toSet());
    }

    public static class TxInput {
        public long amount;
        public int unitbase;
        public String type;
        public String txHashOrPubkey;
        public String indexOrBlockId;
        public String issuer;

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


    private static Function<Integer, String> transformInputIndex2Issuer(final BlockchainBlock.Transaction tx) {
        final Map<Integer, Integer> inputIndexByIssuerIndex = Maps.newHashMap();
        Arrays.stream(tx.getUnlocks())
                .map(TX_UNLOCK_PATTERN::matcher)
                .filter(Matcher::matches)
                .forEach(matcher -> inputIndexByIssuerIndex.put(
                        Integer.parseInt(matcher.group(1)),
                        Integer.parseInt(matcher.group(2)))
                );


        return (inputIndex -> tx.getIssuers()[inputIndexByIssuerIndex.get(inputIndex)]);
    }

}
