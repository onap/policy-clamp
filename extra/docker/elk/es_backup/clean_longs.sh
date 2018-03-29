#!/bin/bash
# these commands remove problematic longs in the default backup
# to avoid errors in kibana when restored.
sed -i 's/,"hits":0//g' default.json
sed -i 's/,"version":1//g' default.json
