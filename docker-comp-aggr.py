#!/usr/bin/env python

'''Parse YAML file to aggregate resources'''

from __future__ import print_function
import yaml
import sys
import math

def mem_parser(mem):
   unit = mem[-1]
   mantissa = mem[0:-1]
   #Unit size in terms of MiB
   normalize = {'b': float(1)/1024**2,  'k': float(1)/1024, 'm': float(1)}

   try:
      return int(mantissa) * normalize[unit]
   except KeyError:
      return int(mantissa) * normalize['b']

with open("example.yaml", 'r') as stream:
  loaded_file = yaml.load(stream)

  ram = 0
  cpu = 0.0
  try:
     for k,v in loaded_file.iteritems():
        cpu = cpu + float(v['cpu_shares'])
        ram = ram + mem_parser(v['mem_limit'])
     print(str(cpu) + " " + str(math.ceil(ram)))
  except KeyError:
     print("CPU and Memory was not set for \'" + k + "\' container", file=sys.stderr)
