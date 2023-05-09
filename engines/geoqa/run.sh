#!/bin/bash

java -jar ./qanary_pipeline-template/target/qa.pipeline-2.4.0.jar &
declare -i PID1=$!
sleep 2
java -jar ./qanary_component-TagMeDisambiguateYago/target/qanary_component-TagMeDisambiguate-0.1.0.jar &
declare -i PID2=$!
java -jar ./qanary_component-ConceptIdentifierYago/target/qanary_component-ConceptIdentifier-0.1.0.jar &
declare -i PID3=$!
java -jar ./qanary_component-PropertyIdentifierYago/target/qanary_component-PropertyIdentifier-0.1.0.jar &
declare -i PID4=$!
java -jar ./qanary_component-RelationDetection/target/qanary_component-RelationDetection-0.1.0.jar &
declare -i PID5=$!
java -jar ./qanary_component-GeoSparqlGeneratorYago/target/qanary_component-GeoSparqlGenerator-0.1.0.jar &
declare -i PID6=$!

# input `exit` to close all processes opened
while true
do
  read input
  if [ "$input" == "exit" ]; then
    break
  fi
done

kill $PID1
kill $PID2
kill $PID3
kill $PID4
kill $PID5
kill $PID6
