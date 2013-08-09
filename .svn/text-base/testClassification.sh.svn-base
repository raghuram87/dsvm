#!/bin/sh
usage() {
	echo "Usage: $0 <dataFileName> <modelFileName> [lambda]"
	exit 1
}
#[[ $# -le 2 ]] && usage
if [[ $# -eq 2 ]]
then
	java -Xmx128m -classpath ./lib/*:classes -Djava.library.path=./lib peersim.gossip.TestClassifier --dataFile $1 --modelFile $2
elif [[ $# -eq 3 ]]
then
	java -Xmx128m -classpath ./lib/*:classes -Djava.library.path=./lib peersim.gossip.TestClassifier --dataFile $1 --modelFile $2 --lambda $3
else
	usage
fi
