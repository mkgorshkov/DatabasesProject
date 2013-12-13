--Maxim Gorshkov (260397155)
--James McCorriston (260409387)
--Andrew Borodovski (260413685)

register s3n://mcgill-comp421-proj4-code/myudfs.jar

-- load the test file into Pig
raw = LOAD 's3n://mcgill-comp421-proj4/btc-2010-chunk-000' USING TextLoader as (line:chararray);

-- parse each line into ntriples
ntriples = foreach raw generate FLATTEN(myudfs.RDFSplit3(line)) as (subject:chararray,predicate:chararray,object:chararray);

-- group by subjects
nsubjects = group ntriples by subject;

-- count the number of elements
ncount = FOREACH nsubjects GENERATE($0), COUNT($1);

-- group by the number of elements
nnumber = group ncount by $1;

-- count frequency of numbers
nnumbercount = FOREACH nnumber GENERATE($0), COUNT($1);

-- store numbercount into a file
store nnumbercount into '/user/hadoop/problem-2' using PigStorage()
