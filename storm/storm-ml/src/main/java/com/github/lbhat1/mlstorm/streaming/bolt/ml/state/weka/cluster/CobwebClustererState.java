package com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.cluster;

import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.base.BaseOnlineWekaState;
import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.utils.WekaUtils;
import weka.clusterers.Cobweb;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Collection;

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


/**
 * Example of a clustering state
 * <p/>
 * Look at abstract base class for method details
 * The base class gives the structure and the BinaryClassifierState classes implement them
 */

public class CobwebClustererState extends BaseOnlineWekaState {
    private final Object lock;
    private Cobweb clusterer;
    private int numClusters;

    public CobwebClustererState(int numClusters, int windowSize) {
        super(windowSize);
        // This is where you create your own classifier and set the necessary parameters
        this.lock = new Object();
        this.clusterer = new Cobweb();
        this.numClusters = numClusters;
    }

    private int getFeatureVectorLength() {
        // hack to obtain the feature set length
        final Collection<double[]> features = this.featureVectorsInCurrentWindow.values();
        for (double[] some : features) {
            return some.length;
        }
        return 0;
    }

    @Override
    public void train(Instances trainingInstances) throws Exception {
        while (trainingInstances.enumerateInstances().hasMoreElements()) {
            train((Instance) trainingInstances.enumerateInstances().nextElement());
        }
    }

    @Override
    protected void postUpdate() {
        this.clusterer.updateFinished();
    }

    @Override
    protected synchronized void preUpdate() throws Exception {
        // Our aim is to create a singleton dataset which will be reused by all trainingInstances
        if (dataset != null) {
            return;
        }

        lazyLoadWekaAttributes(getFeatureVectorLength());

        // we are now ready to create a training dataset metadata
        dataset = new Instances("training", this.wekaAttributes, 0);
        this.clusterer.buildClusterer(dataset.stringFreeStructure());
    }

    @Override
    public long getWindowSize() {
        return super.windowSize;
    }

    @Override
    public double predict(Instance testInstance) throws Exception {
        assert (testInstance != null);
        synchronized (lock) {
            return clusterer.clusterInstance(testInstance);
        }
    }

    @Override
    protected synchronized void lazyLoadWekaAttributes(final int featureCount) {
        if (this.wekaAttributes == null) {
            this.wekaAttributes = WekaUtils.makeFeatureVectorForOnlineClustering(numClusters, featureCount);
            this.wekaAttributes.trimToSize();
        }
    }

    @Override
    protected void train(Instance instance) throws Exception {
        // setting dataset for the instance is crucial, otherwise you'll hit NPE when weka tries to create a single instance dataset internally
        instance.setDataset(dataset);
        synchronized (lock) {
            this.clusterer.updateClusterer(instance);
        }
    }

    public int getNumClusters() {
        return numClusters;
    }
}
