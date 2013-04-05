#!/bin/bash

#args check
if [ $# -ne 3 ]
then
	echo "usage: $0 baseFilename inputExt outputExt"
	exit 1;
fi

#Create temporary directories
SILENCE_DIR=`mktemp -d`;
VAD_DIR=`mktemp -d`;

#Convert to flac
ffmpeg -i "$1$2" -ar 8000 -sample_fmt s16 "$1$3";
					
#Split the input file into several new files at silence
sox "$1$3" "$SILENCE_DIR/$2$3" silence -l 1 0.1 2% 1 0.2 2% : newfile : restart;

#We got the parts we need so remove the full converted file
rm "$1$3"

#Remove stuff which isn't speech
for FILENAME in `ls $SILENCE_DIR`; do
	sox "$SILENCE_DIR/$FILENAME" "$VAD_DIR/$FILENAME" norm vad reverse vad reverse;
done

#Split remaining oversized files
for FILENAME in `ls $VAD_DIR`; do
	sox "$VAD_DIR/$FILENAME" "$FILENAME" trim 0 13 : newfile : restart;
done

#Output a list of the files created with their absolute path
find `pwd` -name "$1*$3";

#remove files which are <= 114 bytes in size

#Delete temporary directories
#rm -r $SILENCE_DIR
#rm -r $VAD_DIR
