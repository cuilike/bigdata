package com.github.lbhat1.mlstorm.streaming.drpc;

import backtype.storm.generated.DRPCExecutionException;
import backtype.storm.utils.DRPCClient;
import com.github.lbhat1.mlstorm.streaming.bolt.ml.state.weka.cluster.query.MlStormClustererQuery;
import com.github.lbhat1.mlstorm.streaming.utils.MlStormFeatureVectorUtils;
import com.github.lbhat1.mlstorm.streaming.utils.SpoutUtils;
import com.google.gson.Gson;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.thrift7.TException;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
/*
* Run drpc query as
* java -cp .:`storm classpath`:$REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar BptiEnsembleQuery qp-hd3 EnsembleClusterer /damsl/projects/mddb/bpti_db/features
* */
public class KMeansDrpcQuery {

    private static double computeRootMeanSquare(double[] v) {
        double distance = 0;
        for (double aV : v) {
            distance += Math.pow((aV), 2);
        }
        return Math.sqrt(distance / v.length);
    }

    private static double computeRootMeanSquareDeviation(double[] v, double[] w) {
        double distance = 0;
        for (int i = 0; i < v.length && i < w.length; i++) {
            distance += Math.pow((v[i] - w[i]), 2);
        }
        return Math.sqrt(distance / (Math.min(v.length, w.length)));
    }

    public static void main(final String[] args) throws IOException, TException, DRPCExecutionException, DecoderException, ClassNotFoundException {
        if (args.length < 3) {
            System.err.println("Where are the arguments? args -- DrpcServer DrpcFunctionName folder");
            return;
        }

        final DRPCClient client = new DRPCClient(args[0], 3772, 1000000 /*timeout*/);
        final Queue<String> featureFiles = new ArrayDeque<String>();
        SpoutUtils.listFilesForFolder(new File(args[2]), featureFiles);

        Scanner scanner = new Scanner(featureFiles.peek());
        int i = 0;
        while (scanner.hasNextLine() && i++ < 10) {
            List<Map<String, List<Double>>> dict = SpoutUtils.pythonDictToJava(scanner.nextLine());
            for (Map<String, List<Double>> map : dict) {
                i++;

                Double[] features = map.get("chi2").toArray(new Double[0]);
                Double[] moreFeatures = map.get("chi1").toArray(new Double[0]);
                Double[] rmsd = map.get("rmsd").toArray(new Double[0]);
                Double[] both = (Double[]) ArrayUtils.addAll(features, moreFeatures);
                String parameters = MlStormFeatureVectorUtils.serializeFeatureVector(ArrayUtils.toPrimitive(both));

                String centroidsSerialized = runQuery(args[1], parameters, client);

                Gson gson = new Gson();
                Object[] deserialized = gson.fromJson(centroidsSerialized, Object[].class);

                for (Object obj : deserialized) {
                    // result we get is of the form List<result>
                    List l = ((List) obj);
                    centroidsSerialized = (String) l.get(0);

                    String[] centroidSerializedArrays = centroidsSerialized.split(MlStormClustererQuery.KmeansClustererQuery.CENTROID_DELIM);
                    List<double[]> centroids = new ArrayList<double[]>();
                    for (String centroid : centroidSerializedArrays) {
                        centroids.add(MlStormFeatureVectorUtils.deserializeToFeatureVector(centroid));
                    }

                    double[] rmsdPrimitive = ArrayUtils.toPrimitive(both);
                    double[] rmsdKmeans = new double[centroids.size()];

                    for (int k = 0; k < centroids.size(); k++) {
                        System.out.println("centroid        -- " + Arrays.toString(centroids.get(k)));
                        double[] centroid = centroids.get(k);
                        rmsdKmeans[k] = computeRootMeanSquare(centroid);
                    }

                    System.out.println("1 rmsd original -- " + Arrays.toString(rmsd));
                    System.out.println("2 rmsd k- Means -- " + Arrays.toString(rmsdKmeans));
                    System.out.println();
                }

            }
        }
        client.close();
    }

    private static String runQuery(final String topologyAndDrpcServiceName, final String args, final DRPCClient client) throws
            TException,
            DRPCExecutionException {
        return client.execute(topologyAndDrpcServiceName, args);
    }
}
