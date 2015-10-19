package com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.base;

 /*
 * Copyright 2013-2015 Lakshmisha Bhat
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

import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.MlStormWekaState;
import com.github.lbhat1.mlstorm.streaming.utils.fields.FieldTemplate;
import com.github.lbhat1.mlstorm.streaming.utils.stats.MlStatistics;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

 /*
 * Copyright 2013-2015 Lakshmisha Bhat
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

public abstract class BaseWekaState implements MlStormWekaState {

    protected final MlStatistics statistics;
    protected final int windowSize;
    private final FieldTemplate fieldTemplate;
    protected Map<Integer, double[]> featureVectorsInWindow;
    protected ArrayList<Attribute> wekaAttributes;
    protected Instances dataset;

    /**
     * Construct the State representation for any weka based learning algorithm
     *
     * @param windowSize the size of the sliding window (cache size)
     */
    public BaseWekaState(final int windowSize, FieldTemplate fieldTemplate) {
        this.windowSize = windowSize;
        this.fieldTemplate = fieldTemplate;
        featureVectorsInWindow = new LinkedHashMap<Integer, double[]>(windowSize, 0.75f /*load factor*/, false) {
            public boolean removeEldestEntry(Map.Entry<Integer, double[]> eldest) {
                return size() > windowSize;
            }
        };
        this.statistics = new MlStatistics();
    }

    /**
     * Predict the class label for the test instance
     * The input parameter is a Weka MlStormFeatureTemplate without the class label
     *
     * @param testInstance
     * @return double, as in the cluster/classification no.
     */
    public abstract double predict(final Instance testInstance) throws Exception;

    /**
     * Do any DB setup etc work here before you commit
     *
     * @param txId
     */
    @Override
    public abstract void beginCommit(final Long txId);

    protected abstract void emptyDataset();

    /**
     * Create a weka dataset and perform any necessary transformations
     *
     * @throws Exception
     */
    protected abstract void createDataSet() throws Exception;

    /**
     * @param featureCount number of features including class attributes
     *                     you must load weka attributes in the createDataSet method to
     *                     guarantee that you have all the details necessary to create a data-set.
     */
    protected abstract void lazyLoadWekaAttributes(final int featureCount);

    /**
     * @throws Exception Train the classifier/clusterer on the above generated dataset.
     */
    protected abstract void train() throws Exception;

    /**
     * do anything you want after updating the classifier
     */
    protected void postUpdate() {
        statistics.setCommitLag(System.currentTimeMillis() - statistics.getCommitLag());
    }

    /**
     * return the feature collection of the most recent window
     */

    public Map<Integer, double[]> getFeatureVectorsInCurrentWindow() {
        return featureVectorsInWindow;
    }

    /**
     * returns average training time
     *
     * @return
     */
    public MlStatistics getStatistics() {
        return statistics;
    }

    /**
     * This is where you do state commit
     * In our case we train the examples and create the model to for this sliding window
     *
     * @param txId
     */
    @Override
    public synchronized void commit(final Long txId) {
        // this is windowed learning.
        final Collection<double[]> groundValues = getFeatureVectorsInCurrentWindow().values();

        try {
            preUpdate();
            update(groundValues);
            postUpdate();

        } catch (Exception e) {
            java.util.logging.Logger.getAnonymousLogger().log(Level.SEVERE, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            persistOrCleanup();
        }
    }

    protected void update(Collection<double[]> groundValues) throws Exception {
        final long start = System.currentTimeMillis();
        for (double[] features : groundValues) {
            Instance trainingInstance = new DenseInstance(wekaAttributes.size());
            trainingInstance.setDataset(dataset);
            for (int i = 0; i < features.length && i < wekaAttributes.size(); i++) {
                trainingInstance.setValue(i, features[i]);
            }
            dataset.add(trainingInstance);
        }
        train();
        statistics.setLastUpdate(System.currentTimeMillis() - start);
    }

    public void persistOrCleanup() {
        dataset.clear();
    }

    // usually one might want to initialize commitLag stats here
    protected void preUpdate() throws Exception {
        statistics.setCommitLag(System.currentTimeMillis());
        createDataSet();
    }

    public FieldTemplate getFieldTemplate() {
        return fieldTemplate;
    }
}

