package org.duniter.core.client.model.bma;

/*
 * #%L
 * Duniter4j :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

/**
 * Blockchain parameters.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
@Data
@FieldNameConstants
// FIXME: next ignore is due to issue on Duniter v1.4 - should be removed later
@JsonIgnoreProperties(ignoreUnknown=true)
public class BlockchainParameters implements Serializable{

    private static final long serialVersionUID = 929951447031659549L;

    private String currency;

    /**
     * The %growth of the UD every [dt] period
     */
    private Double c;

    /**
     * Time period between two UD
     */
    private Integer dt;

    /**
     * UD(0), i.e. initial Universal Dividend
     */
    private Long ud0;

    /**
     * Time for UD(0)
     */
    private Long udTime0;

    /**
     * First time for reveal UD
     */
    private Long udReevalTime0;

    /**
     * Time between two reveal UD
     */
    private Long dtReeval;

    /**
     * Minimum delay between 2 certifications of a same issuer, in seconds. Must be positive or zero.
     */
    private Integer sigPeriod;

    /**
     * Maximum quantity of active certifications made by member.
     */
    private Integer sigStock;

    /**
     * 	Maximum delay a certification can wait before being expired for non-writing.
     */
    private Integer sigWindow;

    /**
     * Maximum age of a active signature (in seconds) (e.g. 2629800)
     */
    private Integer sigValidity;

    /**
     * Minimum quantity of signatures to be part of the WoT(e.g. 3)
     */
    private Integer sigQty;

    /**
     * 	Maximum delay an identity can wait before being expired for non-writing.
     */
    private Integer idtyWindow;

    /**
     * Maximum delay a membership can wait before being expired for non-writing.
     */
    private Integer msWindow;

    /**
     * Minimum percent of sentries to reach to match the distance rule
     */
    private Double xpercent;

    /**
     * Maximum age of an active membership (in seconds)
     */
    private Integer msValidity;

    /**
     * Maximum distance between each WoT member and a newcomer
     */
    private Integer stepMax;

    /**
     * Number of blocks used for calculating median time.
     */
    private Integer medianTimeBlocks;

    /**
     * The average time for writing 1 block (wished time)
     */
    private Integer avgGenTime;

    /**
     * The number of blocks required to evaluate again PoWMin value
     */
    private Integer dtDiffEval;

    /**
     * The number of previous blocks to check for personalized difficulty
     */
    @Deprecated
    private Integer blocksRot;

    /**
     * The percent of previous issuers to reach for personalized difficulty
     */
    private Double percentRot;

    @Deprecated
    public Integer getBlocksRot() {
        return blocksRot;
    }

    @Deprecated
    public void setBlocksRot(Integer blocksRot) {
        this.blocksRot = blocksRot;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("currency=" ).append(currency)
                .append("\nc=").append(c)
                .append("\ndt=").append(dt)
                .append("\nud0=").append(ud0)
                .append("\nsigPeriod=").append(sigPeriod)
                .append("\nsigStock=").append(sigStock)
                .append("\nsigWindow=").append(sigWindow)
                .append("\nsigValidity=").append(sigValidity)
                .append("\nsigQty=").append(sigQty)
                .append("\nidtyWindow=").append(idtyWindow)
                .append("\nmsWindow=").append(msWindow)
                .append("\nxpercent=").append(xpercent)
                .append("\nmsValidity=").append(msValidity)
                .append("\nstepMax=").append(stepMax)
                .append("\nmedianTimeBlocks=").append(medianTimeBlocks)
                .append("\navgGenTime=").append(avgGenTime)
                .append("\ndtDiffEval=").append(dtDiffEval)
                .append("\nudTime0=").append(udTime0)
                .append("\nudReevalTime0=").append(udReevalTime0)
                .append("\ndtReeval=").append(dtReeval)
                .append("\npercentRot=").append(percentRot)
                .toString();
    }
}
