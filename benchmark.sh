#!/bin/bash

#script used to check sanity of output for the Java assignment of the
#parallel programming practical. Please don't submit assignments which don't
#make it through this test. Passing this test does not mean you passed
#the practical :)

SERVER_ADDRESS=$1

if [ -z $SERVER_ADDRESS ];
then
	echo "Usage: bin/sanity-check IBIS_SERVER_ADDRESS"
	exit 1
fi


ARGUMENTS=""
IPL_ARGUMENTS="-Dibis.pool.name=test -Dibis.pool.size=2 -Dibis.server.address=$SERVER_ADDRESS"

echo Building application using ant
ant


echo Running sequential version...
if ! prun -v -1 -np 1 bin/java-run ida.sequential.Ida $ARGUMENTS >sequential.out 2>sequential.err ;
then
	echo "Running sequential version failed"
	exit 1
fi


echo Running ipl version...
for i in 2 4 8 12 16; do
	# for j in {1..3}; do
		prun -v -1 -np $i bin/java-run $IPL_ARGUMENTS ida.ipl.Ida $ARGUMENTS >ipl$i.out 2>ipl$i.err ;
	# done
done

# if ! prun -v -1 -np 2 bin/java-run $IPL_ARGUMENTS ida.ipl.Ida $ARGUMENTS >ipl.out 2>ipl.err ;
# then
	# echo "Running ipl version failed"
	# exit 1
# fi


echo Checking output...

for VERSION in sequential ipl2 ipl4 ipl8 ipl16;

do

	DIFF=`diff bin/reference.out $VERSION.out`

	if [ -z "$DIFF" ]; then
		echo "$VERSION: Output ok"
	else
		echo "$VERSION: Invalid output"
		exit 1
	fi

	if grep "ida took" $VERSION.err --quiet ; then
		echo "$VERSION: Time print found"
	else
		echo "$VERSION: No time print found"
		exit 1
	fi

done
