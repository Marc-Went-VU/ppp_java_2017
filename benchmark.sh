#!/bin/bash
ant
echo seq >> results
for i in {1..3}; do
	./run-local.sh 2>&1 | grep milliseconds | cut -f4 -d" " >> results
done
for i in 1 2 4 8 12 16; do
	echo par $i >> results
	for j in {1..3}; do
		POOLSIZE=$i ./run-das.sh 2>&1 | grep milliseconds | cut -f4 -d" " >> results
	done
done
