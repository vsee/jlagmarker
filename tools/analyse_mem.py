#!/usr/bin/python

import sys

memlogfile = sys.argv[1]
runMode = int(sys.argv[2])

with open(memlogfile, 'r') as f:
	memlogcontent = f.readlines()

class AllocMem:
	addr = None
	size = None

malloccalls = dict()
# XXX: malloc(48) = 0x7f6714352bb0
allocMem = 0
allocMemList = list()

for line in memlogcontent:
	stripline = line.strip()
	
	if(runMode == 1):
		if(stripline.startswith("XXX: malloc")):
			am = AllocMem()
			
			chunks = stripline.split("=")
			am.addr = chunks[1].strip()
			am.size = long(chunks[0].split("(")[1].split(")")[0], 16)
			
			malloccalls[am.addr] = am
			
			allocMem += am.size
			allocMemList.append(allocMem)
		elif(stripline.startswith("XXX: free")):
			if(stripline == "XXX: free((nil))"): continue
			
			addr = stripline.split("(")[1].split(")")[0]
			if(addr in malloccalls):
				am = malloccalls.pop(addr)
				allocMem -= am.size
				allocMemList.append(allocMem)
			else:
				print("free for not found: " + addr)
	
		elif(stripline.startswith("XXX: calloc")):
			am = AllocMem()
			
			chunks = stripline.split("=")
			am.addr = chunks[1].strip()
			callargs = chunks[0].split("(")[1].split(")")[0]
			am.size = long(callargs.split(",")[0], 16) * long(callargs.split(",")[1], 16) 
			
			malloccalls[am.addr] = am
			
			allocMem += am.size
			allocMemList.append(allocMem)
		elif(stripline.startswith("XXX: realloc")):
			am = AllocMem()
			
			chunks = stripline.split("=")
			am.addr = chunks[1].strip()
			
			if("(nil)" in chunks[0]):
				am.size = long(chunks[0].split(",")[1].split(")")[0], 16)
			else:
				callargs = chunks[0].split("(")[1].split(")")[0]
				
				addrArg =  callargs.split(",")[0]
				if(addrArg in malloccalls):
					am = malloccalls.pop(addrArg)
					allocMem -= am.size
					allocMemList.append(allocMem)
				else:
					print("realloc free for not found: " + addrArg)
				
				am.size = long(callargs.split(",")[1], 16)
			
			malloccalls[am.addr] = am
			
			allocMem += am.size
			allocMemList.append(allocMem)
	elif(runMode == 2):
# 		DEBUG: free video frame 0x7f62c4f324e0 
# 		DEBUG: mallocing video frame 0x7f62c4f13bc0
		


print("Memory blocks allocated: " + str(len(malloccalls)))
totalMem = 0
for memblock in malloccalls.values():
	totalMem += memblock.size
print("Memory not freed: " + str(totalMem) + " Bytes")
print("Memory not freed: " + str(totalMem / 1024. / 1024.) + " MB")

with open("memprofile.csv", 'w') as outFile:
	outFile.write("mem Byte\n")
	for entry in allocMemList:
		outFile.write(str(entry) + "\n")
	

	