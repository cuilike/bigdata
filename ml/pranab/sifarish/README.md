## Introduction
Sifarish is a suite of solutions for recommendation personalization implementaed on 
Hadoop and Storm. Various  algorithms, including  feature similarity based recommendation 
and collaborative filtering based recommendation using social rating data are available

## Philosophy
* Providing complete business solutions, not just bunch of machine learning algorithms
* Simple to use
* Input output in CSV format
* Metadata defined in simple JSON file
* Extremely configurable with tons of configuration knobs

## Getting Started
Please read ../resource/GentleIntroductionToSifarish.docx for a high level introduction
and overview. The various tutorial documents in the resource directory are useful for
running different example use cases.

## Blogs
The following blogs of mine are good source of details of sifarish. These are the only source
of detail documentation

* http://pkghosh.wordpress.com/2011/10/26/similarity-based-recommendation-basics/
* http://pkghosh.wordpress.com/2011/11/28/similarity-based-recommendation-hadoop-way/
* http://pkghosh.wordpress.com/2011/12/15/similarity-based-recommendation-text-analytic/
* http://pkghosh.wordpress.com/2012/04/21/socially-accepted-recommendation/
* http://pkghosh.wordpress.com/2010/10/19/recommendation-engine-powered-by-hadoop-part-1/
* http://pkghosh.wordpress.com/2010/10/31/recommendation-engine-powered-by-hadoop-part-2/
* http://pkghosh.wordpress.com/2012/12/31/get-social-with-pearson-correlation/
* http://pkghosh.wordpress.com/2012/09/03/from-item-correlation-to-rating-prediction/
* http://pkghosh.wordpress.com/2014/02/10/from-explicit-user-engagement-to-implicit-product-rating/
* http://pkghosh.wordpress.com/2014/04/14/making-recommendations-in-real-time/
* http://pkghosh.wordpress.com/2014/05/26/popularity-shaken/
* http://pkghosh.wordpress.com/2014/06/23/novelty-in-personalization/
* http://pkghosh.wordpress.com/2014/09/10/realtime-trending-analysis-with-approximate-algorithms/
* http://pkghosh.wordpress.com/2014/12/22/positive-feedback-driven-recommendation-rank-reordering/
* https://pkghosh.wordpress.com/2015/01/20/diversity-in-personalization-with-attribute-diffusion/
* https://pkghosh.wordpress.com/2015/03/22/customer-service-and-recommendation-system/

## Content Similarity Based Recommendation
In the absence of social rating data, the only options is a feature similarity 
based recommendation. Similarity is calculated based on distance between entities 
in a multi dimensional feature space. Some examples are - recommending jobs based 
on user's resume - recommending products based on user profile. These
solutions are known as content based recommendation, because it's based innate 
features of some entity.

There are two different solutions as follows
1. Similarity between entities of different types (e.g. user profile and product)
2. Similarity between entities of same type (e.g. product)

Attribute meta data is defined in a json file. Both entities need not have the 
same set of attributes. Mapping between attributes values from one entity to 
the other can be defined in the config file.

The data type supported are numerical (integer), categorical, text, geo location, time. The 
distance algorithms  can be chosed to be euclidian, manhattan or minkowski. The default algorithm 
is euclidian. 

The distancs between different atrributes of different types are combined to find distance between 
two entity instances. Different weights can be assigned to the attributes to control the relative 
importance of different attributes.

The tutorial ../resource/product_similarity_tutorial.txt is a good starting point. The relevant
blogs are useful to understand the inner workings.


## Social Interaction Data Based Recommendation
These solutions are based user behavior data with respect to some product 
or service. these algorithms are also known as collaborative filtering.  

User behavior data is defined in terms of some explicit rating by user 
or it's derived from user  behavior in the site. The essential  input to all these algorithms 
is a matrix of user and items. The value for a cell could be the ratingas an integer. It could 
also be boolean,  if the user's interest in an item is expressed as a boolean

The tutorial ../resource/tutorial.txt is a good starting point. The relevant blogs are useful 
to understand the inner workings.


## Cold Starting Recommenders
These solutions are used when enough social data is not avaialable. 

1. If data contains text attributes, use TextAnalyzer MR to convert text to token stream 
   using lucene
2. Find similar items based on user profile. Use DiffTypeSimilarity MR
3. Use TopMatches MR to find top n matches for a profile


## Warm Starting Recommenders
When limited amount of user behavior data is available, these solutuions are appropriate

1. If data contains text attributes, use TextAnalyzer MR to convert text to token stream 
   using lucene
2. Find similar items by pairing items with one another using SameTypeSimilarity MR
3. Use TopMatches MR to find top n matches for a product


## Recommenders with Fully engaged Users
When significant of user behavior data is available, these soltions can be used. In 
the order of  complexity, the choices are as follows. They are all based on social data

There two phases for collaborative filetering based recommendation using social data
1. Find correlation between items 2. Predict rating based on items alreadyv rated and 
result of 1

The process involved running multiple map reduce jobs. Some of them are optional. Please refer to the 
tutorial document tutorial.txt in the resource directory


## Real Time Recommendation
Recommendations can be made real time based on user's current behavior in a pre defined time
window. The solution is based on Storm, although Hadoop gets used to compute item correlation
matrix from historical user behavior data.

## Text Attribute
For content based recommendation being able to find match between text field is an important
factor. Text attributes are stemmed or normalized with Apache Lucene. Various languages, in addition
to default of english are supported. They are german, french, italian, spanish, polish and brazilian
portuguese. Text matching algorithms supported are cosine, jaccard and semantic. For semantic matching,
RDF semantic graph is used 

## Complex Attributes
For content based recommendation, There is  support for structured fields e.g., Location, Time Window, 
Event, Categorized Item etc. Many of these  provide contextual dimensions to recommendation. They 
are particularly relevant for recommendation in the mobile space

## Novelty 
Novelty for an item can be computed at individual user level or the whole user community as a
whole. Novelty is blended into the final recommendation list by taking weighted average of
predicted rating and novelty  

## Diversilty 
Based on recent work in the academic world, I am working on implementing some  algorithms to introduce  
diversity in recommendation. Unlike novelty, diversity is group wise property. Diversity can be
defined either in terms item dissimilarity in a collaborative filtering sense or structural and content 
sense

## Facted Match
For content based recommendation, faceted match is supported as faceted search in Solr.
Faceted fields are specified through a configuration parameter

## Dithering
Dithering effectively handles the problem users usually not browsing the first few items
in a list. The dithering process shuffles the list little bit, every time recommended items 
are presented to the user.
 
## Getting started
Please use the tutorial.txt file in the resource directory for batch mode recommendation 
processing. For real time recommendation please use the tutorial document there is a separate
tutorial document realtime\_recommendation\_tutorial.txt

## Integration with other recommndation systems
If you use Apache mahout or some thing else for recommendation, you can
bring your basic recommendation output (userID, itemID, predictedRating) to
sifarish for additional postprocessing to improve the quality of the output. They are
listed in the next section.

## Post processing plugins
Just accuracy from the CF algorithm is not enough for a good recommender. There
are various post processing plugins are essential. They improve the quality of results. 
Here is the list. Sifarsh supports most them. Some are under development.

* Business goal injection 
* Adding novelty 
* Adding diversity 
* Rank reordering for explicit positive feedback 
* Rank reordering for implicit negative feedback 
* Dithering

## Configuration
Please refer to the wiki page for a detailed list of all configuration parameters
https://github.com/pranab/sifarish/wiki/Configuration. Going through the tutorial documents
in the resource directory, you can find sample configuration for various use cases.

## Build
Please read jar\_dependency.txt in the resource directory for build and run time dependency

For Hadoop 1
* mvn clean install

For Hadoop 2 (non yarn), use the branch nuovo
* git checkout nuovo
* mvn clean install

For Hadoop 2 (yarn), use the branch nuovo
* git checkout nuovo
* mvn clean install -P yarn

## Help
Please feel free to email me at pkghosh99@gmail.com

## Contribution
Contributors are welcome. Please email me at pkghosh99@gmail.com






