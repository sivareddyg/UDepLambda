# Compiles protos and creates source code.
compile_protos:
	protoc -I=protos --java_out=src protos/transformation-rules.proto
	
