package org.duniter.core.util.merkle;

/*-
 * #%L
 * Duniter4j :: Core Shared
 * %%
 * Copyright (C) 2014 - 2021 Duniter Team
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

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MerkleTreeTest {

    private static final Logger log = LoggerFactory.getLogger(MerkleTreeTest.class);

    @Test
    public void createMerkleTree() {

        MerkleTree tree;
        List<String> abcde = ImmutableList.of(
                "a",
                "b",
                "c",
                "d",
                "e"
        );

        // SHA1
        {
            tree = new MerkleTree("sha1", abcde, true);
            Assert.assertNotNull(tree);
            Assert.assertEquals("114B6E61CB5BB93D862CA3C1DFA8B99E313E66E9", tree.root());

            //
            List<String> nodes = tree.level(0);
            Assert.assertEquals(1, nodes.size());
            Assert.assertEquals("114B6E61CB5BB93D862CA3C1DFA8B99E313E66E9", nodes.get(0));

            //
            nodes = tree.level(1);
            Assert.assertEquals(2, nodes.size());
            Assert.assertEquals("585DD1B0A3A55D9A36DE747EC37524D318E2EBEE", nodes.get(0));
            Assert.assertEquals("58E6B3A414A1E090DFC6029ADD0F3555CCBA127F", nodes.get(1));

            //
            nodes = tree.level(2);
            Assert.assertEquals(3, nodes.size());
            Assert.assertEquals("F4D9EEA3797499E52CC2561F722F935F10365E40", nodes.get(0));
            Assert.assertEquals("734F7A56211B581395CB40129D307A0717538088", nodes.get(1));
            Assert.assertEquals("58E6B3A414A1E090DFC6029ADD0F3555CCBA127F", nodes.get(2));
        }

        // SHA2
        tree = new MerkleTree("sha256", abcde, true);
        Assert.assertNotNull(tree);
        Assert.assertEquals("16E6BEB3E080910740A2923D6091618CAA9968AEAD8A52D187D725D199548E2C", tree.root());

        // SHA2
        tree = new MerkleTree("md5", abcde, true);
        Assert.assertNotNull(tree);
        Assert.assertEquals("064705BD78652C090975702C9E02E229", tree.root());

        // SHA2
        tree = new MerkleTree("none", abcde, true);
        Assert.assertNotNull(tree);
        Assert.assertEquals("ABCDE", tree.root());
    }


    @Test
    public void createMerkleTreePeer() {

        MerkleTree tree;
        List<String> peersHash = ImmutableList.of(
                "0DC0D9D029FF6164866DC7DF256A15D6572C649B4B634629D4C6E8A2066005C8",
                "8EEA5E37DDC4074FE7E51A2BC347A34AC1A83ED1722411A65B61D8B848344E54",
                "706424BC360BA6F3F9ED1EC2AADA886D9082C2597874773402376B46EC77A627",
                "96FE4D8F1EB72260B9914A0134051E8E711EE1EC40ACAF08D9EF1A9A4CBED45D",
                "9F6F5B3A9AD734F657478D5D6966AE7E672FACD0520EAB929BB4E2A286B1EA9C",
                "292F985A37CC7DDAF1489919B3688A97D9C26AA3813D72029C395DED960B45D1",
                "8BC5139CF21E3369BD5F167EC0975415289ACD4E685A41661D7FC87EB099FD9A",
                "26274CF9EE574DC9C488968957219700430388DF9FA42D9C59EB9B423DA2B19E",
                "D404CFEB173ADBD249093CA720442A3BC45BF30A6F0E474A12E8F3DC38F7EB3C",
                "81466D5012223534DD73333E40E09F439821597A329C37E243FA4C1825569037",
                "916DED5460AD10C9703D00C94CBA98ADA57C70E99909F399E07E58044974D54A",
                "E9E7E2EFAB5D17627BAF6EC5BC01D125FF0499B9527080A00CA3920CF9051482",
                "34F8F4DA28515E65C422014124D9AA1B130BC6ECA1DA0EED612BE1D693F34FFF",
                "FC7ED841C6BB4A7B52F28AE4F39E5696F17779998CF5894DBF456BD243C31760",
                "DEF950E508A316DD71A1EF808B86C2F65354A5F81B82B66C22FF586966E18DD8",
                "D02E24D9947591D3074674C5DE69B86BD48B5C344F125CEF9392794822EAE62C",
                "2D914E851C90739609988FF3EC37A2360AFB8E8EA58FD1B1D747E4B391227964",
                "2F1984E0ABF7702346211402653E1C9DEE8C1FF6AEFE449D88D608985AA08740",
                "8870EC1E8763F77804B1A30616E68A0FEA8B4A1185EDCF89AA4B14545BB59AA7",
                "788CD0E2510C1F37299A10756FDF52B91465014069EA87A8CDC44A52EA52047D",
                "607B251FFEB53714AC36527CFB71D60DD9BD073134DF833C69BAF5D24BF0A340",
                "0CFC52A8811D04A692E49008E5019E37055DCA80115A62A6FFEAEFBE9E837044",
                "1C58D24C807B332986876CE92413618A9827E8A9E5D9D134D59E8BC123C4165A",
                "F9816088514831D59D9242E8D00F352692C52FCBCABD524A576FD68CB5DE4CA4",
                "2DDD2A4F9A44324FEC9C4D33F01C947A003EEBA5AE9C982EA21EFC087ECBC5E5",
                "521499BF20AB21D5C1998FBBE28182746593191BF6E8B9A81FB921E8F1F88E82",
                "7B4F84385064002780D578296C2DA570B4E8A1C82CF3657A6EE4A58148D72B6A",
                "FB589D44B4E6EF3513CAE2314840A4B9C736470E374784575343A4F66C039B37",
                "E8F486AD89E69ED2EE5D5DAFB7F985CD5EB011AE7425BB19324E08A53AE3CE14",
                "DB3357D2E09CFD994D076B4F5D4E720320DA8C701440C282DFA479DC7E26E116",
                "15AD4713526DCCC1D46F38A2E0B01D48EE6048818F4604D08CB38EC14A8F6D70",
                "C2E41814704D8B4FD95C77EF2129DBB9774C244F7D946B89C0191FD0F690F012",
                "820C470B1D68CF6AF9EAA478F43A7DF4AE9FC43E198DC0F159B2A8D80845E9CD",
                "EADF749753B422E7A198168005FE1BF846B164C5BEC2CA0446B4781883B720E0",
                "D7C99BD78A76CF8F0D0BEF87C9120C6717EAF12BD6E55611BCA3EF070637758C"
        );

        tree = new MerkleTree("sha256", peersHash, true);
        Assert.assertNotNull(tree);
        Assert.assertEquals("5B36C83DFDA6D32CC60AE1A7D0246C0E41A1300CCCAABF590E50200AD5ECE4BC", tree.root());
        Assert.assertEquals(6, tree.depth());
        Assert.assertEquals(38, tree.nodes());
    }
}
