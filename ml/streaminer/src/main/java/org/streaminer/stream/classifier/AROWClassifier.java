/**
 * Copyright 2013-2015 Pierre Merienne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.streaminer.stream.classifier;

import org.jblas.DoubleMatrix;
import org.streaminer.util.math.MathUtil;

/**
 * Adaptive Regularization of Weight Vectors implementation.
 * 
 * @see http://www.cs.jhu.edu/~mdredze/publications/nips09_arow.pdf
 * @author pmerienne
 * 
 */
public class AROWClassifier extends SimpleClassifier<Boolean> {
    private static final long serialVersionUID = 206770369174442259L;

    private double r = 1.0;
    private double[] weights;
    private double[][] variance;

    public AROWClassifier() {
    }

    public AROWClassifier(double r) {
        this.r = r;
    }

    @Override
    public Boolean predict(double[] features) {
        if (weights == null || variance == null) {
            init(features.length);
        }

        double margin = new DoubleMatrix(weights).dot(new DoubleMatrix(features));
        Boolean prediction = margin > 0 ? Boolean.TRUE : Boolean.FALSE;

        return prediction;
    }

    @Override
    public void learn(Boolean label, double[] features) {
        if (weights == null || variance == null) {
            init(features.length);
        }

        DoubleMatrix weightsVector = new DoubleMatrix(1, weights.length, weights);
        DoubleMatrix varianceMatrix = new DoubleMatrix(variance);
        DoubleMatrix featuresVector = new DoubleMatrix(1, features.length, features);

        double margin = weightsVector.dot(featuresVector);

        double labelAsDouble = label ? 1.0 : -1.0;
        if (margin * labelAsDouble < 1) {
            double confidence = featuresVector.dot(featuresVector.mmul(varianceMatrix));

            double beta = 1 / (confidence + r);
            double alpha = Math.max(0, beta * (1 - labelAsDouble * margin));
            DoubleMatrix delta = featuresVector.mmul(varianceMatrix).mul(alpha * labelAsDouble);

            boolean zeroVector = MathUtil.isZeros(delta);

            if (!zeroVector) {
                weights = weightsVector.add(delta).toArray();

                // Matrix library needed!
                variance = varianceMatrix.sub(featuresVector.mmul(varianceMatrix)
                        .transpose().mmul(featuresVector).mul(beta).mmul(varianceMatrix)).toArray2();
            }
        }
    }

    public void reset() {
        this.weights = null;
        this.variance = null;
    }

    private void init(int featureSize) {
        // Init weights
        this.weights = new double[featureSize];

        // Init variance
        this.variance = new double[featureSize][featureSize];
        for (int i = 0; i < featureSize; i++) {
            this.variance[i][i] = 1.0;
        }
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public double[][] getVariance() {
        return variance;
    }

    public void setVariance(double[][] variance) {
        this.variance = variance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(r);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((variance == null) ? 0 : variance.hashCode());
        result = prime * result + ((weights == null) ? 0 : weights.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        AROWClassifier other = (AROWClassifier) obj;
        if (Double.doubleToLongBits(r) != Double.doubleToLongBits(other.r))
            return false;
        if (variance == null) {
            if (other.variance != null)
                return false;
        } else if (!variance.equals(other.variance))
            return false;
        if (weights == null) {
            if (other.weights != null)
                return false;
        } else if (!weights.equals(other.weights))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AROWClassifier [r=" + r + "]";
    }

}