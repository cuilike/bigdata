## Introduction
Set of predictive and exploratory data mining tools. Runs on Hadoop and Storm

## Philosophy
* Simple to use
* Input output in CSV format
* Metadata defined in simple JSON file
* Extremely configurable with tons of configuration knobs

## Solution
* Exploratry analytic including correlation, feature subset selection
* Naive Bayes
* Discrimininant analysis
* Nearest neighbor
* Decision tree
* Reinforcement learning


## Blogs
The following blogs of mine are good source of details of sifarish. These are the only source
of detail documentation
* http://pkghosh.wordpress.com/2014/03/12/using-mutual-information-to-find-critical-factors-in-hospital-readmission/
* http://pkghosh.wordpress.com/2014/01/09/boost-lead-generation-with-online-reinforcement-learning/
* http://pkghosh.wordpress.com/2013/11/06/retarget-campaign-for-abandoned-shopping-carts-with-decision-tree/
* http://pkghosh.wordpress.com/2013/10/06/predicting-customer-loyalty-trajectory/
* http://pkghosh.wordpress.com/2013/08/25/bandits-know-the-best-product-price/
* http://pkghosh.wordpress.com/2013/06/29/learning-but-greedy-gambler/
* http://pkghosh.wordpress.com/2013/04/15/smarter-email-marketing-with-markov-model/
* http://pkghosh.wordpress.com/2013/03/18/analytic-is-your-doctors-friend/
* http://pkghosh.wordpress.com/2013/02/19/stop-the-customer-separation-pain-bayesian-classifier/
* http://pkghosh.wordpress.com/2013/01/31/explore-with-cramer-index/
* https://pkghosh.wordpress.com/2015/07/06/customer-conversion-prediction-with-markov-chain-classifier/
* https://pkghosh.wordpress.com/2015/05/11/is-bigger-data-better-for-machine-learning/


## Getting started
Project's resource directory has various tutorial documents for the use cases described in
the blogs.

## Configuration 
All configuration parameters are described in the wiki page
https://github.com/pranab/avenir/wiki/Configuration

## Build
Please refer to resource/dependency.txt for build time and run time dependencies

For Hadoop 1
* mvn clean install

For Hadoop 2 (non yarn)
* git checkout nuovo
* mvn clean install

For Hadoop 2 (yarn)
* git checkout nuovo
* mvn clean install -P yarn

## Help
Please feel free to email me at pkghosh99@gmail.com

## Contribution
Contributors are welcome. Please email me at pkghosh99@gmail.com

