# Freebase Experiments

## Setting up Freebase

Let's setup Freebase server first.

1. Install virtuso. See http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSUbuntuNotes
2. Download our Freebase version at https://www.dropbox.com/sh/zxv2mos2ujjyxnu/AAACCR4AJ1MMTCe8ElfBN39Ha?dl=0
3. In a terminal, cd to the folder which you just downloaded
4. Run "pwd"
5. Replace /dev/shm/vdb/ in virtuoso.ini with the output of Step 4.
6. Run "virtuoso-t -f"

## Multiligual WebQuestions and GraphQuestions Data

1. cd UDepLambda
2. git submodule update --init --recursive data/WebQuestions
3. git submodule update --init --recursive data/GraphQuestions

### Data Format

## Paper Results replication

TODO

## Running your own pipeline

### Entity Annotation

### Running UDepLambda

### Running Oracle
