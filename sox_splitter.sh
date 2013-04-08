#!/bin/bash

#args check
if [ $# -ne 4 ]
then
	echo "usage: $0 baseDir baseFilename inputExt outputExt"
	exit 1;
fi

cd $1

#Make file readable
sudo chmod +r $2

#Create temporary directories
SILENCE_DIR=`mktemp -d`;
VAD_DIR=`mktemp -d`;
SOX_PATH=/usr/bin/sox;

#Split the input file into several new files at silence
$SOX_PATH -t ffmpeg "$2$3" "$2$4" trim 0 10 : newfile : restart 1>&2

#Split the input file into several new files at silence
#$SOX_PATH -t ffmpeg "$2$3" "$SILENCE_DIR/$2$4" silence -l 1 0.1 2% 1 0.2 2% : newfile : restart 1>&2 

#Remove stuff which isn't speech
#for FILENAME in `ls $SILENCE_DIR`; do
#	$SOX_PATH "$SILENCE_DIR/$FILENAME" "$VAD_DIR/$FILENAME" norm vad reverse vad reverse 1>&2 
#done

#Split remaining oversized files
#for FILENAME in `ls $VAD_DIR`; do
#	$SOX_PATH "$VAD_DIR/$FILENAME" "$FILENAME" trim 0 13 : newfile : restart 1>&2 
#done

#Output a list of the files created with their absolute path
find `pwd` -maxdepth 1 -name "$2*$4";

#remove files which are <= 114 bytes in size

#Delete temporary directories
rm -r $SILENCE_DIR;
rm -r $VAD_DIR;
