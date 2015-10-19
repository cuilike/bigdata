package com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.utils;

/**
 * Created by lbhat@DaMSl on 4/16/14.
 * <p/>
 * Copyright {2013} {Lakshmisha Bhat}
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public enum WekaOnlineClassificationAlgorithms {
    onlineDecisionTree, nearestNeighbors, locallyWeightedLearner, naiveBayes, stochasticGradientDescent
}
