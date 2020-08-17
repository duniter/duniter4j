package org.duniter.core.util.merkle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.crypto.DigestUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * MerkleTree is an implementation of a Merkle hash tree
 *
 * This is a Java implementation of the JS lib: https://github.com/c-geek/merkle
 * (used by Duniter)
 */
public class MerkleTree {

    private static final Map<String, String> REGEXP_STR_BY_DIGEST = ImmutableMap.<String, String>builder()
            .put("md5",      "^[0-9a-f]{32}$")
            .put("sha1",      "^[0-9a-f]{40}$")
            .put("ripemd160", "^[0-9a-f]{40}$")
            .put("sha256",    "^[0-9a-f]{64}$")
            .put("sha512",    "^[0-9a-f]{128}$")
            .put("DEFAULT",   "^$").build();

    private Predicate<String> regexp;
    private boolean useUpperCaseForHash;
    private Function<String, String> shaToHex;
    private List<String> leaves;
    private int treeDepth = 0;
    private List<List<String>> rows = Lists.newArrayList();
    private int nodesCount = 0;

    /**
     * Create a MerkleTree from a list of leaf digests. Merkle tree is built
     * from the bottom up.
     *
     * @param digests
     *            array of leaf digests (bottom level)
     */
    public MerkleTree(String digestionAlgo, List<String> digests, boolean useUpperCaseForHash) {
        this.useUpperCaseForHash = useUpperCaseForHash;
        String regexpStr;
        switch (digestionAlgo.toLowerCase()) {
            case "sha1":
                shaToHex = (str) -> DigestUtils.sha1Hex(str);
                regexpStr = REGEXP_STR_BY_DIGEST.get("sha1");
                break;
            case "sha256":
                shaToHex = (str) -> DigestUtils.toHex("SHA-256", str, "UTF-8");
                regexpStr = REGEXP_STR_BY_DIGEST.get("sha256");
                break;
            case "sha512":
                shaToHex = (str) -> DigestUtils.toHex("SHA-512", str, "UTF-8");
                regexpStr = REGEXP_STR_BY_DIGEST.get("sha256");
                break;
            case "md5":
                shaToHex = (str) -> DigestUtils.toHex("MD5", str, "UTF-8");
                regexpStr = REGEXP_STR_BY_DIGEST.get("md5");
                break;
            default:
                shaToHex = (str) -> str;
                regexpStr = REGEXP_STR_BY_DIGEST.get("DEFAULT");
                break;
        }
        this.regexp = Pattern.compile(useUpperCaseForHash ? regexpStr.toUpperCase() : regexpStr).asPredicate();
        this.leaves = new ArrayList<>();

        constructTree(digests);
    }

    /**
     * Create a MerkleTree from a list of leaf digests. Merkle tree is built
     * from the bottom up.
     *
     * @param digests
     *            array of leaf digests (bottom level)
     */
    public MerkleTree(List<String> digests) {
        this("sha1", digests, true);
    }


    private void feed(String str) {
        if(StringUtils.isNotBlank(str) && regexp.test(str)){
            // Push leaf without hashing it since it is already a hash
            this.leaves.add(str);
        }
        else{
            String hash = this.shaToHex.apply(str);
            if (useUpperCaseForHash) {
                hash = hash.toUpperCase();
            }
            leaves.add(hash);
        }
    }

    public int depth() {
        // Compute tree depth
        if(treeDepth == 0){
            int pow = 0;
            while(Math.pow(2, pow) < leaves.size()){
                pow++;
            }
            treeDepth = pow;
        }
        return treeDepth;
    }

    public int levels() {
        return depth() + 1;
    }

    public int nodes() {
        return nodesCount;
    }

    public String root() {
        return rows.get(0).get(0);
    }

    public List<String> level(int i) {
        return rows.get(i);
    }

    private void compute() {
        int theDepth = depth();
        if(rows.size() == 0){
            // Compute the nodes of each level
            for (int i = 0; i < theDepth; i++) {
                rows.add(Lists.newArrayList());
            }

            rows.add(this.leaves);

            for (int j = theDepth-1; j >= 0; j--) {
                List<String> jRow = rows.get(j);
                jRow.clear();
                jRow.addAll(getNodes(rows.get(j+1)));
                nodesCount += jRow.size();
            }
        }
    }

    private List<String> getNodes(List<String> leaves) {
        int remainder = leaves.size() % 2;
        List<String> nodes = Lists.newArrayList();
        String hash;
        for (int i = 0; i < leaves.size() - 1; i = i + 2) {
            hash = shaToHex.apply(leaves.get(i) + leaves.get(i+1));
            if (useUpperCaseForHash) {
                hash = hash.toUpperCase();
            }
            nodes.add(hash);
        }
        if (remainder == 1){
            nodes.add(leaves.get(leaves.size() - 1));
        }
        return nodes;
    }


    /**
     * Computes merkle tree synchronously, returning json result.
     **/
    private void constructTree(List<String> leaves) {
        leaves.forEach(leaf -> feed(leaf));
        compute();
    };

}
