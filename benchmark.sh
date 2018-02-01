#!/bin/bash
ant
for i in {1..3}; do
	./run-local.sh 2>&1 | grep milliseconds | cut -f3 -d" " | ( echo "seq ${i}" && cat) >> results
done
for i in 2 4 8 12 16; do
	for j in {1..3}; do
		POOLSIZE=$i ./run-das.sh 2>&1 | grep milliseconds | cut -f3 -d" " | ( echo "par ${i} ${j}" && cat)  >> results
	done
done
