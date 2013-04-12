#!/bin/bash

#args check
if [ $# -ne 4 ]; then
	echo "usage: $0 baseDir baseFilename inputExt outputExt"
	exit 1;
fi

cd $1

#Create temp directories
readonly MAX_LENGTH=13;
#SILENCE_DIR=`mktemp -d`;
#VAD_DIR=`mktemp -d`;
TRIM_DIR=`mktemp -d`;
TEMP_TRANSCODE=temp_$2$4;
INPUT_FILE=$2$3

#Transcode
ffmpeg -i "$INPUT_FILE" "$TEMP_TRANSCODE"

sox "$TEMP_TRANSCODE" "$TRIM_DIR/$2$4" trim 0 $MAX_LENGTH : newfile : restart 1>&2 
#Remove stuff which isn't speech
for FILENAME in `ls $TRIM_DIR`; do
		sox "$TRIM_DIR/$FILENAME" "$FILENAME" norm vad reverse vad reverse 1>&2
done

#Split the input file into several new files at silence
#sox "$TEMP_TRANSCODE" "$SILENCE_DIR/$2$4" silence -l 1 0.1 2% 1 0.2 2% : newfile : restart 1>&2 

#Remove stuff which isn't speech
#for FILENAME in `ls $SILENCE_DIR`; do
#		sox "$SILENCE_DIR/$FILENAME" "$VAD_DIR/$FILENAME" norm vad reverse vad reverse 1>&2
#done

#Split remaining oversized files
#for FILENAME in `ls $VAD_DIR`; do
#for FILENAME in `ls $SILENCE_DIR`; do
#	sox "$SILENCE_DIR/$FILENAME" "$TRIM_DIR/$FILENAME" trim 0 $MAX_LENGTH : newfile : restart 1>&2 
#done

#Merge consecutive files up to a length limit 13s
#CANDIDATE_MERGE_FILES="";
#MERGE_FILES="";
#FILE_INDEX=0;

#for FILENAME in `ls $TRIM_DIR`; do
#	FILEPATH="$TRIM_DIR/$FILENAME";
#	FILE_LENGTH=`soxi -D $FILEPATH`;
#	if [ `echo "$FILE_LENGTH > 0" | bc` -ne 0 ]; then
#		CANDIDATE_MERGE_FILES="$MERGE_FILES $FILEPATH";	#Add another file to the candidate merge
#		CANDIDATE_MERGE_FILE_LENGTH=`soxi -TD $CANDIDATE_MERGE_FILES`;
#
#		#If the candidate merge pushes length over the limit then merge the files of previous iterations
#		if [ `echo "$CANDIDATE_MERGE_FILE_LENGTH > $MAX_LENGTH" | bc` -eq 1 ]; then
#			sox $MERGE_FILES $2_$((FILE_INDEX++))$4 1>&2;
#			CANDIDATE_MERGE_FILES=$FILEPATH;
#		fi
#
#		MERGE_FILES=$CANDIDATE_MERGE_FILES
#	fi
#done

#For the unmerged remainder when MERGE_FILES is not empty
#if [ -n "$MERGE_FILES" ]; then
#	sox $MERGE_FILES $2_$((FILE_INDEX++))$4 1>&2;
#fi

#Output a list of the files created with their absolute path
find `pwd` -maxdepth 1 -name "$2*$4";

#Delete temp directories
#rm -r $SILENCE_DIR;
#rm -r $VAD_DIR;
#rm -r $TRIM_DIR;
rm $TEMP_TRANSCODE;
