#!/bin/bash

#args check
if [ $# -ne 3 ]
then
	echo "usage: $0 baseFilename inputExt outputExt"
	exit 1;
fi

#Make file readable
sudo chmod +r $1$2

#Create temporary directories
SILENCE_DIR=`mktemp -d`;
VAD_DIR=`mktemp -d`;
SOX_PATH=/usr/bin/sox;

#Split the input file into several new files at silence
echo "$1 $2 $3"

echo "removing silence..."
$SOX_PATH -V6 -t ffmpeg "$1$2" "$SILENCE_DIR/$1$3" silence -l 1 0.1 2% 1 0.2 2% : newfile : restart #&>> /tmp/output

#Remove stuff which isn't speech
echo "removing non-speech"
for FILENAME in `ls $SILENCE_DIR`; do
	$SOX_PATH -V6 "$SILENCE_DIR/$FILENAME" "$VAD_DIR/$FILENAME" norm vad reverse vad reverse #&>> /tmp/output
done

#Split remaining oversized files
echo "splitting remaing oversized files"
for FILENAME in `ls $VAD_DIR`; do
	$SOX_PATH -V6 "$VAD_DIR/$FILENAME" "$FILENAME" trim 0 13 : newfile : restart #&>> /tmp/output
done

#Output a list of the files created with their absolute path
find `pwd` -maxdepth 1 -name "$1*$3"; 

#remove files which are <= 114 bytes in size

#Delete temporary directories
#rm -r $SILENCE_DIR;
#rm -r $VAD_DIR;
