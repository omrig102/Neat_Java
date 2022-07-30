import json
import sys
from graphviz import Digraph

generation = sys.argv[1]
type = sys.argv[2]

with open('genomes/' + generation + '/' + type + 'GraphData.json') as json_file:
	data = json.load(json_file)
	nodes = data['nodes']
	connections = data['connections']
	f = Digraph(type, filename=type + '.png')
	f.attr(rankdir='LR', size='8,5')

	f.attr('node', shape='circle')
	for node in nodes :
		nodeStr = str(node)
		if(nodeStr.startswith('input')) :
			f.node(nodeStr,nodeStr,color='red',fillColor='red')	
		elif(nodeStr.startswith('output')):
			f.node(nodeStr,nodeStr,color='blue')
		else :
			f.node(nodeStr,nodeStr)		
		
	for con in connections :
		f.edge(str(con['from']), str(con['to']), label=str(con['innov']))

	f.view()