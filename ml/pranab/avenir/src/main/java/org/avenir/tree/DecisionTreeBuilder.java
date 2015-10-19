/*
 * avenir: Predictive analytic based on Hadoop Map Reduce
 * Author: Pranab Ghosh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */


package org.avenir.tree;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.avenir.explore.ClassPartitionGenerator.PartitionGeneratorReducer;
import org.avenir.tree.SplitManager.AttributePredicate;
import org.avenir.util.AttributeSplitStat;
import org.avenir.util.InfoContentStat;
import org.chombo.mr.FeatureField;
import org.chombo.mr.FeatureSchema;
import org.chombo.util.Tuple;
import org.chombo.util.Utility;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author pranab
 *
 */
public class DecisionTreeBuilder   extends Configured implements Tool {

	@Override
	public int run(String[] args) throws Exception {
        Job job = new Job(getConf());
        String jobName = "Decision tree  builder";
        job.setJobName(jobName);
        job.setJarByClass(DecisionTreeBuilder.class);
        Utility.setConfiguration(job.getConfiguration(), "avenir");

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
       
        job.setMapperClass(DecisionTreeBuilder.BuilderMapper.class);
        job.setReducerClass(DecisionTreeBuilder.BuilderReducer.class);
        
        job.setMapOutputKeyClass(Tuple.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        int numReducer = job.getConfiguration().getInt("dtb.num.reducer", -1);
        numReducer = -1 == numReducer ? job.getConfiguration().getInt("num.reducer", 1) : numReducer;
        job.setNumReduceTasks(numReducer);

        int status =  job.waitForCompletion(true) ? 0 : 1;
        return status;
	}

	/**
	 * Decision tree or random forest. For random forest, data is sampled in the first iteration
	 * @author pranab
	 *
	 */
	public static class BuilderMapper extends Mapper<LongWritable, Text, Tuple, Text> {
		private String fieldDelimRegex;
		private String[] items;
        private Tuple outKey = new Tuple();
		private Text outVal  = new Text();
        private FeatureSchema schema;
        private List<Integer> splitAttrs;
        private FeatureField classField;
        private int maxCatAttrSplitGroups;
        private SplitManager splitManager;
        private String attrSelectStrategy;
        private int randomSplitSetSize;
        private String classVal;
        private String currenttDecPath;
        private String decPathDelim;
        private DecisionPathList decPathList;
        private Map<String, Boolean> validDecPaths = new HashMap<String, Boolean>();
        private String subSamlingStrategy;
        private static final String SUB_SAMPLING_WITH_REPLACE = "withReplace";
        private static final String SUB_SAMPLING_WITHOUT_REPLACE = "withoutReplace";
        private static final String SUB_SAMPLING_WITHOUT_NONE = "none";
        private boolean treeAvailable;
        private int samplingRate;
        private int samplingBufferSize;
        private String[]  samplingBuffer;
        private int count;
        private static final String ATTR_SEL_ALL = "all";
        private static final String ATTR_SEL_NOT_USED_YET = "notUsedYet";
        private static final String ATTR_SEL_RANDOM_ALL = "randomAll";
        private static final String ATTR_SEL_RANDOM_NOT_USED_YET = "randomNotUsedYet";
        
        private static final Logger LOG = Logger.getLogger(BuilderMapper.class);

        /* (non-Javadoc)
         * @see org.apache.hadoop.mapreduce.Mapper#setup(org.apache.hadoop.mapreduce.Mapper.Context)
         */
        protected void setup(Context context) throws IOException, InterruptedException {
        	Configuration conf = context.getConfiguration();
            if (conf.getBoolean("debug.on", false)) {
            	LOG.setLevel(Level.DEBUG);
            }
        	fieldDelimRegex = conf.get("field.delim.regex", ",");
        	maxCatAttrSplitGroups = conf.getInt("max.cat.attr.split.groups", 3);
        	
        	//schema
        	InputStream fs = Utility.getFileStream(context.getConfiguration(), "feature.schema.file.path");
            ObjectMapper mapper = new ObjectMapper();
            schema = mapper.readValue(fs, FeatureSchema.class);
            
            //decision path list  file
        	fs = Utility.getFileStream(context.getConfiguration(), "decision.file.path");
            mapper = new ObjectMapper();
            decPathList = mapper.readValue(fs, DecisionPathList.class);
           
            //split manager
            decPathDelim = conf.get("dec.path.delim", ";");
            splitManager = new SplitManager(conf, "dec.path.file.path",  decPathDelim , schema); 
            treeAvailable = splitManager.isTreeAvailable();
            
            //attribute selection strategy
            attrSelectStrategy = conf.get("split.attribute.selection.strategy", "notUsedYet");
 
           	randomSplitSetSize = conf.getInt("random.split.set.size", 3);
            
            //class attribute
            classField = schema.findClassAttrField();
            
            validDecPaths.clear();
            
            //sub sampling
            subSamlingStrategy = conf.get("sub.samling.strategy", "withReplace");
            if (subSamlingStrategy.equals(SUB_SAMPLING_WITHOUT_REPLACE)) {
            	samplingRate = Utility.assertIntConfigParam(conf, "sampling.rate", 
            			"samling rate should be provided for sampling without replacement");
            } else if (subSamlingStrategy.equals(SUB_SAMPLING_WITH_REPLACE)) {
            	int samplingBufferSize = conf.getInt("sampling.buffer.size",  10000);
            	samplingBuffer = new String[samplingBufferSize];
            }
        }
        
		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			if (!treeAvailable && subSamlingStrategy.equals(SUB_SAMPLING_WITH_REPLACE)) {
				for (int i = 0; i < count; ++i) {
					int sel = (int)(Math.random() * count);
					sel = sel == count ? count -1 : sel;
					mapHelper(samplingBuffer[sel], context);
				}
			}
			super.cleanup(context);
		}
        
        @Override
        protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        	//sampling
        	if (!treeAvailable) {
        		//first iteration
        		 if (subSamlingStrategy.equals(SUB_SAMPLING_WITH_REPLACE)) {
        			 	//sampling with replace
		        		if (count <  samplingBufferSize)  {
		        			samplingBuffer[count++] = value.toString();
		        		} else {
		        			//sample all
		        			for (int i = 0; i < samplingBufferSize; ++i) {
		        				int sel = (int)(Math.random() * samplingBufferSize);
		        				sel = sel == samplingBufferSize ? samplingBufferSize -1 : sel;
		                		mapHelper(samplingBuffer[sel], context);
		        			}
		        			
		        			//start refilling buffer
		        			count = 0;
		        			samplingBuffer[count++] = value.toString();
		        		}
        		 } else if (subSamlingStrategy.equals(SUB_SAMPLING_WITHOUT_REPLACE)) {
        			 	//sampling without replace
     					int sel = (int)(Math.random() * 100);
     					if (sel < samplingRate) {
     		        		mapHelper(value.toString(),  context);
     					}
        		 } else {
        			 //no sampling
             		mapHelper(value.toString(),  context);
        		 }
        	} else {
        		//intermediate iteration
        		mapHelper(value.toString(),  context);
        	}
  		}

        /**
         * @param record
         * @param context
         * @throws IOException
         * @throws InterruptedException
         */
        private void mapHelper(String record, Context context)
                throws IOException, InterruptedException {
       		items  =  record.split(fieldDelimRegex);
       	    classVal = items[classField.getOrdinal()];
            	
             currenttDecPath = null;
              if (treeAvailable) {
                	currenttDecPath = items[0];
                	
                	//find decision path status
                	Boolean status = validDecPaths.get(currenttDecPath);
                	if (null == status) {
                		//from decision path list object
                		DecisionPathList.DecisionPath decPathObj = decPathList.findDecisionPath(currenttDecPath.split(decPathDelim)) ;
                		status = null == decPathObj ? false : true;
                		
                		//cache it
                		validDecPaths.put(currenttDecPath, status);
                	} 
                	
                	//rejected decision path from earlier iteration rejected splits
                	if (!status) {
                		return;
                	}
                }
                
              //get split attributes
             getSplitAttributes();
                
              //all attributes
              for (int attr :  splitAttrs) {
                	FeatureField field = schema. findFieldByOrdinal(attr);
                	Object attrValue = null;
                	//all splits
                	List<List<AttributePredicate>> allSplitPredicates = null;
                	if (field.isInteger()) {
                		allSplitPredicates = splitManager.createIntAttrSplitPredicates(attr);
                		Integer iValue = Integer.parseInt(items[attr]);
                		attrValue = iValue;
                	} else if (field.isDouble()) {
                		allSplitPredicates = splitManager.createDoubleAttrSplitPredicates(attr);
                		Double dValue = Double.parseDouble(items[attr]);
                		attrValue = dValue;
                	} else if (field.isCategorical()) {
                		allSplitPredicates = splitManager.createCategoricalAttrSplitPredicates(attr);
                		attrValue = items[attr];
                	}
                    
                	//evaluate split predicates
                    for (List<AttributePredicate> predicates : allSplitPredicates) {
                    	for (AttributePredicate predicate : predicates) {
                    		if (predicate.evaluate(attrValue)) {
                    			//data belongs to this split segment
                    			outKey.initialize();
                    			if (null == currenttDecPath) {
                    				outKey.add(predicate.toString());
                        			outVal.set(record);
                    			} else {
                    				//existing predicates
                    				String[] curDecPathItems = items[0].split(decPathDelim);
                    				for (String curDecPathItem : curDecPathItems) {
                        				outKey.add(curDecPathItem);
                    				}
                    				
                    				//new predicate
                     				outKey.add(predicate.toString());
                     				int pos = record.indexOf(fieldDelimRegex);
                     				
                     				//exclude predicate
                        			outVal.set(record.substring(pos + fieldDelimRegex.length()));
                    			}               		
                				context.write(outKey, outVal);
                    		}	
                    	}
                    }
              }
      	}
       
        /**
         * @param attrSelectStrategy
         * @param conf
         */
        private void getSplitAttributes() {
            if (attrSelectStrategy.equals(ATTR_SEL_ALL)) {
            	//all attributes
            	splitAttrs = splitManager.getAllAttributes();
            } else if (attrSelectStrategy.equals(ATTR_SEL_NOT_USED_YET)) {
            	//attributes that have not been used yet
            	splitAttrs = splitManager.getRemainingAttributes(currenttDecPath);
            } else if (attrSelectStrategy.equals(ATTR_SEL_RANDOM_ALL)) {
            	//randomly selected k attributes from all
            	splitManager.getRandomAllAttributes(randomSplitSetSize);
            } else if (attrSelectStrategy.equals(ATTR_SEL_RANDOM_NOT_USED_YET)) {
            	//randomly selected k attributes from attributes not used yet
            	splitManager.getRandomRemainingAttributes(currenttDecPath, randomSplitSetSize);
            } else {
            	throw new IllegalArgumentException("invalid splitting attribute selection strategy");
            }
        }	
	}

	/**
	 * @author pranab
	 *
	 */
	public static class BuilderReducer extends Reducer<Tuple, Text, NullWritable, Text> {
 		private FeatureSchema schema;
		private String fieldDelim;
		private Text outVal  = new Text();
		private String  infoAlgorithm;
        private boolean outputSplitProb;
        private Map<String, Map<String, InfoContentStat>> decPaths = new HashMap<String, Map<String, InfoContentStat>>();
        private int classAttrOrdinal;
        private String classAttrValue;
        private String parentDecPath;
        private  String decPath;
        private String childPath;
        private String decPathDelim;
        private DecisionPathStoppingStrategy pathStoppingStrategy;
        private DecisionPathList decPathList;
        private static final Logger LOG = Logger.getLogger(PartitionGeneratorReducer.class);

	   	@Override
	   	protected void setup(Context context) throws IOException, InterruptedException {
        	Configuration conf = context.getConfiguration();
            if (conf.getBoolean("debug.on", false)) {
            	LOG.setLevel(Level.DEBUG);
            	AttributeSplitStat.enableLog();
            }
            
            //schema
        	InputStream fs = Utility.getFileStream(context.getConfiguration(), "feature.schema.file.path");
            ObjectMapper mapper = new ObjectMapper();
            schema = mapper.readValue(fs, FeatureSchema.class);
            
            //decision path list  file
        	fs = Utility.getFileStream(context.getConfiguration(), "decision.file.path");
            mapper = new ObjectMapper();
            decPathList = mapper.readValue(fs, DecisionPathList.class);
            
        	fieldDelim = conf.get("field.delim.out", ",");
        	infoAlgorithm = conf.get("split.algorithm", "giniIndex");
        	outputSplitProb = conf.getBoolean("output.split.prob", false);
        	classAttrOrdinal = Utility.assertIntConfigParam(conf, "class.attr.ordinal", "missing class attribute ordinal");
            decPathDelim = conf.get("dec.path.delim", ";");

        	//stopping strategy
        	String stoppingStrategy =  conf.get("stopping.strategy", DecisionPathStoppingStrategy.STOP_MIN_INFO_GAIN);
        	int maxDepthLimit = -1;
        	double minInfoGainLimit = -1;
        	int minPopulationLimit = -1;
        	if (stoppingStrategy.equals(DecisionPathStoppingStrategy.STOP_MAX_DEPTH)) {
        		maxDepthLimit = Utility.assertIntConfigParam(conf, "max.depth.limit", "missing max depth limit for tree");
        	} else if (stoppingStrategy.equals(DecisionPathStoppingStrategy.STOP_MIN_INFO_GAIN)) {
            	minInfoGainLimit =      Utility.assertDoubleConfigParam(conf, "min.info.gain.limit", "missing min info gain limit");     
        	} else if (stoppingStrategy.equals(DecisionPathStoppingStrategy.STOP_MIN_POPULATION)) {
            	minPopulationLimit =  Utility.assertIntConfigParam(conf, "min.population.limit", "missing min population limit");                 
        	} else {
        		throw new IllegalArgumentException("invalid stopping strategy " + stoppingStrategy);
        	}
        	pathStoppingStrategy = new DecisionPathStoppingStrategy(stoppingStrategy, maxDepthLimit, 
        			minInfoGainLimit,minPopulationLimit);
        	
	   	}   

	   	@Override
	   	protected void cleanup(Context context)  throws IOException, InterruptedException {
	   		DecisionPathList newDecPathList = new DecisionPathList();
	   		boolean isAlgoEntropy = infoAlgorithm.equals("entropy");
	   		double parentStat = 0;
   			Map<Integer, List< InfoContentStat>> attrInfoContent = new HashMap<Integer, List<InfoContentStat>>();
	   		
   			//each parent path
	   		for (String parentPath :  decPaths.keySet() ) {		   		
	   			//info content stat for different attributes
	   			attrInfoContent.clear();
	   			
	   			Map<String, InfoContentStat> childStats = decPaths.get(parentPath);
	   			int selectedSplitAttr = 0;
	   			double minInfoContent = 1000;
	   			
	   			//parent decision path in existing tree
	   			DecisionPathList.DecisionPath parentDecPath = findParentDecisionPath(parentPath);
	   			if (null == parentDecPath) {
	   				throw new IllegalStateException("missing parent decision path in existing tree");
	   			}
	   			parentStat = parentDecPath.getInfoContent();
	   			
	   			//all child stats for current parent decision path
	   			for (String childPath :  childStats.keySet()) {
	   				//each child path
	   				InfoContentStat stat = childStats.get(childPath);
	   				//stat.processStat(infoAlgorithm.equals("entropy"));
	   				int attr = Integer.parseInt(childPath.split("\\s+")[0]);
	   				
	   				//group  by attribute
	   				List< InfoContentStat> statList = attrInfoContent.get(attr);
	   				if (null == statList) {
	   					statList  = new ArrayList< InfoContentStat>();
	   					attrInfoContent.put(attr, statList);
	   				}
	   				stat.setPredicate(childPath);
	   				statList.add(stat);
	   			}
	   			
	   			//select splitting attributes iterating through each attribute
	   			for (int attr :  attrInfoContent.keySet()) {
	   				double weightedInfoContent = 0;
	   				int totalCount = 0;
	   				
	   				//info content of each split
	   				for (InfoContentStat stat : attrInfoContent.get(attr)) {
	   					weightedInfoContent += stat.processStat(isAlgoEntropy) * stat.getTotalCount();
	   					totalCount += stat.getTotalCount();
	   				}
	   				
	   				//average info conten across splits
	   				double  avInfoContent = weightedInfoContent / totalCount;
	   				
	   				//pick minimum
	   				if (avInfoContent  < minInfoContent) {
	   					minInfoContent = avInfoContent;
	   					selectedSplitAttr = attr;
	   				}
	   			}
	   			
	   			//parent predicates
	   			List<DecisionPathList.DecisionPathPredicate> parentPredicates = 
	   					DecisionPathList.DecisionPathPredicate.createPredicates(parentPath, schema);
	   			
	   			//generate new path based on selected split attribute
				FeatureField field = schema.findFieldByOrdinal(selectedSplitAttr);
			    List< DecisionPathList.DecisionPathPredicate> predicates = new ArrayList< DecisionPathList.DecisionPathPredicate>();
   				for (InfoContentStat stat : attrInfoContent.get(selectedSplitAttr)) {
   					//all predicate for selected attribute
   					String predicateStr = stat.getPredicate();
   					DecisionPathList.DecisionPathPredicate predicate = null;
   					if (field.isInteger()) {
   						predicate = DecisionPathList.DecisionPathPredicate.createIntPredicate(predicateStr);
   					} else if (field.isDouble()) {
   						predicate = DecisionPathList.DecisionPathPredicate.createDoublePredicate(predicateStr);
   					} else if (field.isCategorical()) {
   						predicate = DecisionPathList.DecisionPathPredicate.createCategoricalPredicate(predicateStr);
   					} 
   					
   					//append new predicate to parent predicate list
   					predicates.clear();
   					predicates.addAll(parentPredicates);
   					predicates.add(predicate);
   					
   					//create new decision path
   					boolean toBeStopped = pathStoppingStrategy.shouldStop(stat, parentStat, parentPredicates.size() + 1);
   					DecisionPathList.DecisionPath decPath = new DecisionPathList.DecisionPath(predicates, stat.getTotalCount(),
   							stat.getStat(),  toBeStopped);
   					newDecPathList.addDecisionPath(decPath);
   				}	   			
	   		}
	   		
	   		//save new decision path list
	   		writeDecisioList(newDecPathList, "decision.file.path",  context.getConfiguration() );
	   	}
	   	
	   	/**
	   	 * finds decision path from current tree
	   	 * @param parentPath
	   	 * @return
	   	 */
	   	private DecisionPathList.DecisionPath findParentDecisionPath(String parentPath) {
	   		String[] parentPathItems = parentPath.split(decPathDelim);
	   		DecisionPathList.DecisionPath decPath = decPathList.findDecisionPath(parentPathItems);
	   		return decPath;
	   	}
	   	
	   	/**
	   	 * @param newDecPathList
	   	 * @param outFilePathParam
	   	 * @param conf
	   	 * @throws IOException
	   	 */
	   	private void writeDecisioList(DecisionPathList newDecPathList, String outFilePathParam, Configuration conf ) 
	   			throws IOException {
	   		FSDataOutputStream ouStrm = FileSystem.get(conf).create(new Path(conf.get(outFilePathParam)));	
	   		ObjectMapper mapper = new ObjectMapper();
	   		mapper.writeValue(ouStrm, newDecPathList);
	   		ouStrm.flush();
	   	}
	   	
        /* (non-Javadoc)
         * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN, java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
         */
        protected void reduce(Tuple  key, Iterable<Text> values, Context context)
        		throws IOException, InterruptedException {
        	int keySize = key.getSize();
        	key.setDelim(";");
        	decPath = key.toString();
        	parentDecPath =  key.toString(0, keySize-1);
        	childPath = key.getString(keySize-1);
        	
        	//all child class stats
        	Map<String, InfoContentStat> candidateChildrenPath =  decPaths.get(parentDecPath);
        	if (null == candidateChildrenPath) {
        		candidateChildrenPath = new HashMap<String, InfoContentStat>();
        		decPaths.put(parentDecPath,  candidateChildrenPath);
        	}
        	
        	//class stats
        	InfoContentStat classStats = candidateChildrenPath.get(childPath);
        	if (null == classStats) {
        		classStats = new InfoContentStat();
        		candidateChildrenPath.put(childPath, classStats);
        	}
        	
        	
        	for (Text value : values) {
        		classAttrValue = values.toString().split(fieldDelim)[classAttrOrdinal];
        		classStats.incrClassValCount(classAttrValue);
            	outVal.set(decPath + fieldDelim + value.toString());
            	context.write(NullWritable.get(), outVal);
        	}
        }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new DecisionTreeBuilder(), args);
        System.exit(exitCode);
	}
	
}
