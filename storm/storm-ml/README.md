# mlstorm - Machine Learning in Storm Ecosystem

Experimenting with online ensemble methods for collaborative learning and parallel streaming principal components analysis.

## Introduction

a. A basic description of Storm and its capabilities is available at [storm home page] (http://storm.incubator.apache.org/)

b. A detailed tutorial on how to install and run a multi-node storm cluster is available [here] ( http://www.michael-noll.com/tutorials/running-multi-node-storm-cluster/)

c. This [wiki page] (https://github.com/nathanmarz/storm/wiki/Lifecycle-of-a-topology) helps you understand the lifecycle of a storm topology

d. This [wiki page] (https://github.com/nathanmarz/storm/wiki/Trident-API-Overview) provides a detailed description of the API's used in this project.

e. This [wiki page] (https://github.com/nathanmarz/storm/wiki/Understanding-the-parallelism-of-a-Storm-topology) should be useful to understand the parallelism of a storm topology.

f. The common configurations and details of running Storm on a production cluster can be found in this [wiki page] (https://github.com/nathanmarz/storm/wiki/Running-topologies-on-a-production-cluster)

g. We have implemented spouts to stream BPTI features, sensor data etc. `MddbFeatureExtractorSpout`, `SensorStreamingSpout` and `AustralianElectricityPricingSpout` are all NonTransactional spouts. A detailed description of Transactional, Non-Transactional and Opaque-Transactional spouts is available [here] (https://github.com/nathanmarz/storm/wiki/Trident-spouts)

h. The entire storm documentattion resides [here] (https://github.com/nathanmarz/storm/wiki/Documentation)

## A General Framework for Online Machine Learning In Storm

Our framework integrates the Storm stream processing system with the ability to perform exploratory and confirmatory data analysis tasks through the WEKA toolkit. WEKA is a popular library for a range of data mining and machine learning algorithms implemented in the Java programming language. As such, it is straightforward to incorporate WEKA’s algorithms directly into Storm’s bolts (where bolts are the basic unit of processing in the Storm system). 

WEKA is designed as a general-purpose data mining and machine learning library, with its typical use-case in offline modes, where users have already collected and curated their dataset, and are now seeking to determine relationships in the dataset through analysis. To enable WEKA’s algorithms to be used in an online fashion, we decouple the continuous arrival of stream events from the execution of WEKA algorithms through window-based approaches. Our framework supplies window capturing facilities through Storm’s provision of stateful topology components, for example its State classes, persistentAggregate, and stateQuery topology construction methods to access and manipulate state. Our windows define the scope of the data on which we can evaluate WEKA algorithms. Furthermore, while we have implemented basic window sliding capabilities, one can arbitrarily manage the set of open windows in the system through Storm’s state operations. This allows us to dynamically select which windows we should consider for processing, beyond standard sliding mechanisms, for example disjoint windows or predicate-based windows that are defined by other events in the system.

In our framework, each window is supplied for training to a WEKA analysis algorithm. Once training completes, the resulting model can be used to predict or classify future stream events arriving to the system. Below, we present our framework as used with the k-means clustering and principal components analysis algorithms. Our current focus is to support the scalable exploration, training and validation performed by multiple algorithms simultaneously in the Storm system, as is commonly needed when the class of algorithms that ideally models a particular dataset is not known up front, and must be determined through experimentation. For example, with our framework, we can run multiple k-means clustering algorithms in Storm, each configured with a different parameter value for k. Thus in many application scenarios where k is not known a priori, we can discover k through experimentation. Our framework leverages Storm’s abilities to distribute its program topology across multiple machines for scalable execution of analysis algorithms, as well as its fault-tolerance features. 

---------------------------
### Implementation Details 
---------------------------

1. All the learning algorithms are implemented in `Trident` and use external `EJML (Efficient Java Matrix Library)` and `Weka (Weka Machine Learning Toolkit)` libraries. All these libraries reside in the /lib directory in the project home directory. Look at `m2-pom.xml` to get an idea about the project dependencies.

2. The consensus clustering algorithm (`topology.weka.EnsembleClusteringTopology`) uses 2-level (shallow!) deep-learning technique. We experimented with relabelling and majority voting based schemes. However, since the clustering algorithm is quadratic and the hungarian algorithm (used for relabeling) is cubic in the size of the window, we were unable to keep up with a BPTI protein data stream (thus ran into storm timeouts). Interested readers are encouraged to look at `Vega-Pons & Ruiz-Shulcloper, 2011 (A Survey of Clustering Ensemble Algorithms) and Wei-Hao Lin, Alexander Hauptmann, 2003 (Meta-classification: Combining Multimodal Classifiers)` for detailed explanation of the techniques.

----------------
### Experiments
----------------

2. Our implementation of consensus clustering uses the MddbFeatureExtractorSpout to inject feature vectors. All the base clusterers we use implement [weka.clustereres interface] (http://weka.sourceforge.net/doc.dev/weka/clusterers/Clusterer.html). The ensemble consists of `SimpleKMeans, DensityBasedClusterer, FarthestFirst, HierarchicalClusterer, EM and FilteredClusterer (with underlying algorithm being SimpleKMeans)`. The meta-clustering algorithm is density based. If you want modify/replace these algorithms, you may do so by updating the Enum class - WekaClusterers and the factory methods in WekaUtils. We use default parameters for individual clusterers but you may inject the appropriate options that can be handled by weka OptionHandler. For example, you can find all the available options for [SimpleKMeans] ( http://weka.sourceforge.net/doc.dev/weka/clusterers/SimpleKMeans.html), which could be specified as a Java String[]. `If the options are incorrect, we throw a RuntimeException wrapping the actual exception thrown by Weka.`

  a. To submit the topology one can fire away the following command.
  
      storm jar $REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar topology.weka.EnsembleClustererTopology /damsl/projects/bpti_db/features 4 1000 5 1

    The arguments to the topology are described below
  
     1. `directory - The folder containing containing feature vectors` (We use BPTI dataset for our experiments. The spout  implementation - MddbFeatureExtractorSpout - is responsible to feed the topology with the feature vectors. Look at `res/features_o.txt` for an example dataset. If you want access to the bpti dataset, contact lbhat1@jhu.edu or yanif@jhu.edu)
     2. `no of workers - total no. of nodes to run the topology on.`
     3. `window-size - The total number of training examples in the sliding window`
     4. `k - the number of clusters`. This `k` is consistent across all the partitions including the meta-classifier/clusterer. This is a critical requirement for consensus clustering to be successful.
     5. `parallelism - the number of threads per bolt `
  

  b. To predict the most likely cluster for a test sample, one can invoke a drpc query to query the meta learner state.
  
      java -cp .:`storm classpath`:$REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar BptiEnsembleQuery        drpc-server-host ClustererEnsemble
      

  #### Important Notes 
  --------------------
  - Note that the second argument to the DRPC query specifies the drpc function to invoke. These names are hard-coded in our Java files (`topology.weka.EnsembleClassifierTopology.java` and so on.)
  - All the configuration parameters including the `list of drpc servers`, `number of workers`, `max spout pending (described below)` are hard-coded in the Topology source files. Any change to these will require a code recompile.
 
  #### How to compile?
  --------------------
  - Go to the project home directory
  - fire away the command `mvn -f mlstorm.pom package` (Assuming that you have maven installed!)
  - this builds the `mlstorm-00.01-jar-with-dependencies.jar` in the `$REPO/mlstorm/target` directory.


3. There is also an implementation of an ensemble of binary classifiers (`topology.weka.EnsembleBinaryClassifierTopology`) and it's online counterpart (`topology.weka.OnlineEnsembleBinaryClassifierTopology`). All the base/weak learning algorithms are run in parallel with their continuous predictions reduced into (a ReducerAggregator aggregating a grouped stream) a meta-classifier training sample labelled using the original dataset. We use `linear SVM` as our meta classifier and a bunch of weak learners including `pruned decision trees, perceptron, svm, decision stubs` etc.

    You may submit the topologies for execution using the following command

      `storm jar $REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar topology.weka.EnsembleClassifierTopology $REPO/mlstorm/res/elecNormNew.arff 1 1000 5 1`
      
      `storm jar $REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar topology.weka.OnlineEnsembleClassifierTopology $REPO/mlstorm/res/elecNormNew.arff 1 1000 5 1 `
      
    
    To classify a test example, one can invoke a drpc query to query the meta learner (`SVM, by default`) state.
     
      `java -cp .:`storm classpath`:$REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar AustralianElectricityPricingTest drpc-server-host-name ClassifierEnsemble`
      
      `java -cp .:`storm classpath`:$REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar AustralianElectricityPricingTest drpc-server-host-name OnlineClassifierEnsemble`
      
      
4. Our implementation of `Kmeans clustering` allows querying different partitions (`each partition runs a separate k-means instance`). The result of such a query is a partitionId and the query result (for ex. the centroids of all the clusters or the distribution depicting the association of a test sample (feature vector) to the different clusters). Using the partion id returned and the usefulness of the results a human/machine can update the parameters of the model on the fly. The following is an example.

  a.  Submit/Start the topology as usual.

      storm jar $REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar topology.weka.KmeansClusteringTopology /damsl/projects/bpti_db/features 4 10000 10 10

         
   The arguments to the topology are described below:
   1. `directory -- The folder containing containing feature vectors`
   2. `no of workers -- total no. of nodes to run the topology on.`
   3. `k -- the number of clusters`.
   4. `parallelism -- the number of threads per bolt`


  b. A distributed query (`querying for parameters/model statistics`) on the model can be executed as below. This query returns the centroids of all the clusters for a given instance of clustering algorithm.

      java -cp .: `storm classpath` : $REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar drpc.DrpcQueryRunner qp-hd3 kmeans "no args" 

  c. A parameter update (`k for k-means`) can be made using the following DRPC query. Here we are updating the parameters of the said algorithm (K-means clustering) on the fly using DRPC. We started the topology with 10 partitions and (c) is updating the Clusterer at partition '0' to [`k=45`]. 

      java -cp .:`storm classpath`:$REPO/mlstorm/target/mlstorm-00.01-jar-with-dependencies.jar  drpc.DrpcQueryRunner qp-hd3 kUpdate 0,45 


    The result of the above query looks like the following:

      <[["0,35","k update request (30->35) received at [0]; average trainingtime for k = [30] = [334,809]ms"]]>
       
       
5. The PCA and Clustering algorithm implementations are window based. There's also an online/incremental clustering (`Cobweb`) and PCA implementation available for experimentation.

--------------------------------------------------------------------------------------------------------------------------
### Storm Experience Report
--------------------------------------------------------------------------------------------------------------------------

Storm is an open-source system that was initially developed by BackType before its acquisition by Twitter, Inc. The documentation and support for Storm primarily arises from the open-source user community that continues to develop its capabilities through the github project repository. Storm has been incubated as an Apache project as of September 2013. In this section we provide an initial report on our experiences in developing with the Storm framework, particularly as we deploy it onto a cluster environment and develop online learning and event detection algorithms.

##### Notes useful for Debugging
--------------------------------
##### Storm Components
--------------------------------

###### supervisor
  - JVM process launched on each storm worker machine. This does not execute your code — just supervises it.
  - number of workers is set by number of `supervisor.slots.ports` on each machine.
  
###### worker
  - jvm process launched by the supervisor
  - intra-worker transport is more efficient, so we run one worker per topology per machine
  - if worker dies, supervisor will restart. But worker dies for any minor failure (`fail fast`)
  
###### Coordinator 
  - generates new transaction ID
  - sends tuple, which influences spout to dispatch a new batch
  - each transaction ID corresponds identically to single trident batch and vice-versa
  - Transaction IDs for a given `topo_launch` are serially incremented globally.
  - knows about Zookeeper/transactional; so it recovers the transaction ID.
  
###### Executor
  - Each executor is responsible for one bolt or spout.
  - therefore with 3 sensor/MDDB spouts on a worker, there are three executors spouting.

###### Hard to Debug Mistakes
  - A spout must never block when emitting — if it blocks, critical bookkeeping tuples will get trapped, and the topology hangs. So its emitter keeps an `overflow buffer`, and publishes as follows:
    - if there are tuples in the overflow buffer add the tuple to it — the queue is certainly full.
    - otherwise, publish the tuple to the flow with the non-blocking call. That call will either succeed immediately
      or fail with an `InsufficientCapacityException`, in which case add the tuple to the overflow buffer.
    - The spout’s async-loop won’t call nextTuple if overflow is present, so the overflow buffer only has to accommodate the maximum number of tuples emitted in a single nextTuple call.

-------------
##### Acking
-------------
  - Acker is just a regular bolt — all the interesting action takes place in its execute method.
  - set number of ackers equal to number of workers. (`default is 1 per topology`)
  - The Acker holds a single `O(1)` lookup table
  - it is actually a collection of lookup tables: current, old and dead. new tuple trees are added to the current bucket; after every timeout number of seconds, current becomes old, and old becomes dead — they are declared failed and their records retried.
  - it knows `id == tuple[0]` the tuple’s stream-id
  - there is a time-expiring data structure, the RotatingHashMap
  - when you go to update or add to it, it performs the operation on the right component of HashMap.
  - periodically (`when you receive a tick tuple in Storm8.2+`), it will pull off oldest component HashMap, mark it as dead; invoke the expire callback for each element in that HashMap.

-----------------
##### Throttling
-----------------
  - Max spout pending (`TOPOLOGY_MAX_SPOUT_PENDING`) sets the number of tuple trees live in the system at any point in time.
  - Trident batch emit interval (topology.trident.batch.emit.interval.millis) sets the maximum pace at which the trident master batch co-ordinator issues new seed tuples. If batch delay is 500ms and the most recent batch was released 486ms, the spout coordinator will wait 14ms before dispensing a new seed tuple. If the next pending entry isn’t cleared for 523ms, it will be dispensed immediately.
  - Trident batch emit interval  is extremely useful to prevent congestion, especially around startup/rebalance. 
  - As opposed to a traditional Storm spout, a Trident spout will likely dispatch hundreds of records with each batch. If max-pending is 20, and the spout releases `500 records per batch`, the spout will try to cram `10,000` records into its send queue.

------------------
##### Batch Size
------------------
  - Set the batch size to `optimize the throughput of the most expensive batch operation` — a bulk database operation, network request, or large aggregation.
  - When the batch size is too small, bookkeeping dominates response time i.e `response time is constant`
  - Execution times increase slowly and we get better and better records-per-second throughput with increase in batch size.
  - at some point, we start overwhelming some resource and execution time increases sharply (usually due to network failures and replays in our case)
  


-------------------
## Consensus Clustering
-------------------

Consensus clustering, also known as cluster ensemble, aims to find a single partitioning of data from multiple existing basic partitionings `[Monti et al., 2003; Zhang et al., 2010]`. It has been recognized that consensus clustering helps to generate robust partitionings, find bizarre clusters, handle noise and outliers, and integrate solutions from multiple distributed sources. The main motivation for Consensus clustering is the need to assess the “stability” of the discovered clusters, that is, the robustness of the putative clusters to sampling variability. The basic assumption of this method is intuitively simple: if the data represent a sample of items drawn from distinct sub-populations, and if we were to observe a different samples drawn from the same sub-populations, the induced cluster composition and number should not be radically different. Therefore, the more the attained clusters are robust to sampling variability, the more we can be conﬁdent that these clusters represent real structure. 

However, theoretically, Consensus clustering is NP-complete `[Filkov and Steven, 2004a; Topchy et al., 2005]`. In the literature, many algorithms have been proposed to address the computational challenges, among which a K-means-based method proposed in `[Topchy et al., 2003]` attracts great interests. 

----------------
#### Background
----------------
Consensus clustering (CC) is essentially a combinatorial optimization problem. The existing literature can be roughly divided into two categories: CC with implicit objectives (CCIO) and CC with explicit objectives (CCEO).

Methods in CCIO do not set global objective functions. Rather, they directly adopt some heuristics to find approximate solutions. The representative methods include the graph-based algorithms `[Strehl and Ghosh, 2002; Fern and Brodley, 2004]`, the co-association matrix based methods `[Fred and Jain, 2005; Wang et al., 2009]` and Relabeling & Voting based methods `[Fischer and Buhmann, 2003; Ayad and Kamel, 2008]`.

Methods in CCEO have explicit global objective functions for consensus clustering. The Median Partition problem based on Mirkin distance is among the oldest ones `[Filkov and Steven, 2004b; Gionis et al., 2007]`. In the inspiring work, `[Topchy et al., 2003]` proposed a Quadratic Mutual Information based objective function and used K-means clustering to find the solution. This elegant idea could be traced back to the work by Mirkin on the Category Utility Function `[Mirkin, 2001]`. Other solutions for different objective functions include EM algorithm [Topchy et al., 2004], non - negative matrix factorization `[Li et al., 2007]`, kernel-based methods `[Vega-Pons et al., 2010]`, simulated annealing `[Lu et al., 2008]`, and among others.

In general, compared with CCIO methods, CCEO methods might offer better interpretability and higher robustness to clustering results, via the guidance of objective functions. However, they often bear high computational costs. Moreover, one CCEO method typically works for one objective function, which seriously limits its applicative scope.

We attempt to build a general framework using meta-clustering for efficient density based consensus clustering using multiple base utility functions. The base functions use k-means as the underlying algorithm and apply various density manipulation and attribute filtering techniques to them. Thus each base function is parameterized differently.

-----------------------------------------------------
#### The Hungarian Algorithm and Cluster Relabeling
-----------------------------------------------------

The Hungarian algorithm is used to solve the assignment problem. An instance of the assignment problem consists of a number of workers along with a number of jobs and a cost matrix which gives the cost of assigning the i'th worker to the j'th job at position (i, j). The goal is to find an assignment of workers to jobs so that no job is assigned more than one worker and so that no worker is assigned to more than one job in such a manner so as to minimize the total cost of completing the jobs. An assignment for a cost matrix that has more workers than jobs will necessarily include unassigned workers, indicated by an assignment value of -1; in no other circumstance will there be unassigned workers. Similarly, an assignment for a cost matrix that has more jobs than workers will necessarily include unassigned jobs; in no other circumstance will there be unassigned jobs. The Hungarian algorithm runs in time O(n^3), where n is the maximum among the number of workers and the number of jobs.

The voting approach to consensus clustering attempts to solve the cluster correspondence problem. A simple voting produce can be used to assign objects in clusters to determine the final consensus partition. However, label correspondence is exactly what makes unsupervised combination difficult. The main idea behind this scheme is to permute the cluster labels such that best agreement between the labels of two partitions is obtained. All the partitions from the ensemble must be relabeled according to a fixed `reference partition`. The reference partition can be taken as one from the ensemble, or from a new clustering of the dataset. Also, a meaningful voting procedure assumes that the `number of clusters` in every given partition is the same as in the `target partition`. This requires that the number of clusters in the target consensus partition is known. `The complexity of this process is k! , which can be reduced to O(k^3) if the Hungarian method is employed` for the minimal weight bipartite matching problem.


