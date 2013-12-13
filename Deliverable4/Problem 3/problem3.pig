--Used 15 nodes
--Time taken: 7min 56sec
--Maxim Gorshkov (260397155)
--James McCorriston (260409387)
--Andrew Borodovski (260413685)

register s3n://mcgill-comp421-proj4-code/myudfs.jar

-- load the test file into Pig
--raw = LOAD 's3n://mcgill-comp421-proj4/comp421-test-file' USING TextLoader as (line:chararray);

raw = LOAD 's3n://mcgill-comp421-proj4/btc-2010-chunk-000' USING TextLoader as (line:chararray);

-- parse each line into ntriples
ntriples = foreach raw generate FLATTEN(myudfs.RDFSplit3(line)) as (subject:chararray,predicate:chararray,object:chararray);

-- filter to only have touples with '.*rdfabout\\.com.*'
-- for test subject match '.*business.*'
matchSubject = FILTER ntriples BY (subject matches '.*rdfabout\\.com.*');

-- second copy of filtered collection
itermediateSubject2 = FILTER ntriples BY (subject matches '.*rdfabout\\.com.*');;

matchSubject2 = FOREACH itermediateSubject2 GENERATE subject as subject2, predicate as predicate2, object as object2;

-- joins the two filtered collections
-- for test we use subject and subject2
njoined = JOIN matchSubject BY object, matchSubject2 by subject2;

-- removed duplicated touples
ndistinct = distinct njoined;

-- order results by predicate from first copy
nordered = order ndistinct by predicate ASC;

-- store numbercount into a file
store nordered into '/user/hadoop/problem-3-full' using PigStorage();
