package io.ucoin.ucoinj.core.client.model.bma;

/*
 * #%L
 * UCoin Java Client :: Core API
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


import java.io.Serializable;

/**
 * Blockwhain parameters.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
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
    private Integer blocksRot;

    /**
     * The percent of previous issuers to reach for personalized difficulty
     */
    private Double percentRot;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getC() {
        return c;
    }

    public void setC(Double c) {
        this.c = c;
    }

    public Integer getDt() {
        return dt;
    }

    public void setDt(Integer dt) {
        this.dt = dt;
    }

    public Long getUd0() {
        return ud0;
    }

    public void setUd0(Long ud0) {
        this.ud0 = ud0;
    }

    public Integer getSigValidity() {
        return sigValidity;
    }

    public void setSigValidity(Integer sigValidity) {
        this.sigValidity = sigValidity;
    }

    public Integer getSigQty() {
        return sigQty;
    }

    public void setSigQty(Integer sigQty) {
        this.sigQty = sigQty;
    }


    public Integer getMsValidity() {
        return msValidity;
    }

    public void setMsValidity(Integer msValidity) {
        this.msValidity = msValidity;
    }

    public Integer getStepMax() {
        return stepMax;
    }

    public void setStepMax(Integer stepMax) {
        this.stepMax = stepMax;
    }

    public Integer getMedianTimeBlocks() {
        return medianTimeBlocks;
    }

    public void setMedianTimeBlocks(Integer medianTimeBlocks) {
        this.medianTimeBlocks = medianTimeBlocks;
    }

    public Integer getAvgGenTime() {
        return avgGenTime;
    }

    public void setAvgGenTime(Integer avgGenTime) {
        this.avgGenTime = avgGenTime;
    }

    public Integer getDtDiffEval() {
        return dtDiffEval;
    }

    public void setDtDiffEval(Integer dtDiffEval) {
        this.dtDiffEval = dtDiffEval;
    }

    public Integer getBlocksRot() {
        return blocksRot;
    }

    public void setBlocksRot(Integer blocksRot) {
        this.blocksRot = blocksRot;
    }

    public Double getPercentRot() {
        return percentRot;
    }

    public void setPercentRot(Double percentRot) {
        this.percentRot = percentRot;
    }

    public Integer getSigPeriod() {
        return sigPeriod;
    }

    public void setSigPeriod(Integer sigPeriod) {
        this.sigPeriod = sigPeriod;
    }

    public Integer getSigStock() {
        return sigStock;
    }

    public void setSigStock(Integer sigStock) {
        this.sigStock = sigStock;
    }

    public Integer getSigWindow() {
        return sigWindow;
    }

    public void setSigWindow(Integer sigWindow) {
        this.sigWindow = sigWindow;
    }

    public Integer getIdtyWindow() {
        return idtyWindow;
    }

    public void setIdtyWindow(Integer idtyWindow) {
        this.idtyWindow = idtyWindow;
    }

    public Integer getMsWindow() {
        return msWindow;
    }

    public void setMsWindow(Integer msWindow) {
        this.msWindow = msWindow;
    }

    public Double getXpercent() {
        return xpercent;
    }

    public void setXpercent(Double xpercent) {
        this.xpercent = xpercent;
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
                .append("\nblocksRot=").append(blocksRot)
                .append("\npercentRot=").append(percentRot)
                .toString();
    }
}
