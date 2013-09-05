/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import com.bc.ceres.binding.PropertySet;
import binning.AbstractAggregator;
import binning.Aggregator;
import binning.AggregatorConfig;
import binning.AggregatorDescriptor;
import binning.BinContext;
import binning.Observation;
import binning.VariableContext;
import binning.Vector;
import binning.WritableVector;
import binning.support.GrowableVector;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * An aggregator that computes the p-th percentile,
 * the value of a variable below which a certain percent (p) of observations fall.
 *
 * @author MarcoZ
 * @author Norman
 */
public class AggregatorMedian extends AbstractAggregator {

    private final int varIndex;
    //private final int percentage;
    private final String mlName;
    private final String icName;

    public AggregatorMedian(VariableContext varCtx, String varName) {
        this(getVarIndex(varCtx, varName), varName);
    }

    private AggregatorMedian(int varIndex, String varName) {
        super(Descriptor.NAME,
                createFeatureNames(varName, "sum"),
                createFeatureNames(varName, "median"),
                createFeatureNames(varName, "median"),
                Float.NaN);

        if (varName == null) {
            throw new NullPointerException("varName");
        }
        /*if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("percentage < 0 || percentage > 100");
        }*/
        this.varIndex = varIndex;
        //this.percentage = percentage;
        this.mlName = "ml." + varName;
        this.icName = "ic." + varName;
    }

    @Override
    public void initSpatial(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        ctx.put(icName, new int[1]);
    }

    @Override
    public void aggregateSpatial(BinContext ctx, Observation observationVector, WritableVector spatialVector) {
        float value = observationVector.get(varIndex);
        if (!Float.isNaN(value)) {
            spatialVector.set(0, spatialVector.get(0) + value);
        } else {
            // We count invalids rather than valid because it is more efficient.
            // (Key/value map operations are relatively slow, and it is more likely that we will receive valid measurements.)
            ((int[]) ctx.get(icName))[0]++;
        }
    }

    @Override
    public void completeSpatial(BinContext ctx, int numSpatialObs, WritableVector spatialVector) {
        Integer invalidCount = ((int[]) ctx.get(icName))[0];
        int effectiveCount = numSpatialObs - invalidCount;
        if (effectiveCount > 0) {
            spatialVector.set(0, spatialVector.get(0) / effectiveCount);
        } else {
            spatialVector.set(0, Float.NaN);
        }
    }

    @Override
    public void initTemporal(BinContext ctx, WritableVector vector) {
        vector.set(0, 0.0f);
        ctx.put(mlName, new GrowableVector(256));
    }

    @Override
    public void aggregateTemporal(BinContext ctx, Vector spatialVector, int numSpatialObs, WritableVector temporalVector) {
        GrowableVector measurementsVec = ctx.get(mlName);
        float value = spatialVector.get(0);
        if (!Float.isNaN(value)) {
            measurementsVec.add(value);
        }
    }

    @Override
    public void completeTemporal(BinContext ctx, int numTemporalObs, WritableVector temporalVector) {
        GrowableVector measurementsVec = ctx.get(mlName);
        float[] measurements = measurementsVec.getElements();
        Arrays.sort(measurements);
        temporalVector.set(0, computeMedian(measurements));
    }


    @Override
    public void computeOutput(Vector temporalVector, WritableVector outputVector) {
        float value = temporalVector.get(0);
        if (!Float.isNaN(value)) {
            outputVector.set(0, value);
        } else {
            outputVector.set(0, Float.NaN);
        }
    }

    @Override
    public String toString() {
        return "AggregatorMedian{" +
                "varIndex=" + varIndex +
                //", percentage=" + percentage +
                ", spatialFeatureNames=" + Arrays.toString(getSpatialFeatureNames()) +
                ", temporalFeatureNames=" + Arrays.toString(getTemporalFeatureNames()) +
                ", outputFeatureNames=" + Arrays.toString(getOutputFeatureNames()) +
                '}';
    }

    public static float computeMedian(float[] measurements) {
        int middle = measurements.length / 2;
        float median = Float.NaN;
        if (measurements.length > 0) {
            if (measurements.length % 2 == 1) {
                median = measurements[middle];
            } else {
                median = (measurements[middle-1] + measurements[middle]) / 2.0f;
            }
        }

        return median;
    }

    public static class Config extends AggregatorConfig {

        @Parameter
        String varName;
        //@Parameter
        //Integer percentage;

        public Config() {
            super(Descriptor.NAME);
        }

        public void setVarName(String varName) {
            this.varName = varName;
        }

        /*public void setPercentage(Integer percentage) {
            this.percentage = percentage;
        }*/

        @Override
        public String[] getVarNames() {
            return new String[]{varName};
        }
    }


    private static int getVarIndex(VariableContext varCtx, String varName) {
        if (varCtx == null) {
            throw new NullPointerException("varCtx");
        }

        return varCtx.getVariableIndex(varName);
    }

    /*private static int getEffectivePercentage(Integer percentage) {
        return (percentage != null ? percentage : 90);
    }*/

    public static class Descriptor implements AggregatorDescriptor {

        public static final String NAME = "MEDIAN";

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public AggregatorConfig createAggregatorConfig() {
            return new Config();
        }

        /*@Override
        public AggregatorConfig createConfig() {
            return new Config();
        }*/

        @Override
        public Aggregator createAggregator(VariableContext varCtx, AggregatorConfig aggregatorConfig) {
            PropertySet propertySet = aggregatorConfig.asPropertySet();
            return new AggregatorMedian(varCtx, (String) propertySet.getValue("varName"));
        }
    }

    public static String[] createFeatureNames(String varName, String... postfixes) {
        ArrayList<String> featureNames = new ArrayList<String>(postfixes.length);
        for (final String postfix : postfixes) {
            if (postfix != null) {
                featureNames.add(varName + "_" + postfix);
            }
        }
        return featureNames.toArray(new String[featureNames.size()]);
    }
}
