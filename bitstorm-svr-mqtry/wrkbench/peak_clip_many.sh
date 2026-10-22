#!/bin/bash

count=1
while [ $count -le 10 ]
do
    curl -w "\n cost %{time_total}s" -H "Trace-ID: niuge123" -H "Content-Type: application/json; charset=utf-8" -H "User-ID: 77"  http://localhost:8081/demo/peak_clipping  -d '{"num":1}' 
    count=$((count+1))
done
