#!/bin/bash

HOST_IP=${REDIS_HOST}

yes "yes" | redis-cli -h $HOST_IP -p 7211 \
    --cluster create \
    $HOST_IP:7211 \
    $HOST_IP:7221 \
    $HOST_IP:7231

redis-cli -h $HOST_IP -p 7211 \
 --cluster add-node \
 --cluster-slave $HOST_IP:7212 $HOST_IP:7211

redis-cli -h $HOST_IP -p 7211 \
 --cluster add-node \
 --cluster-slave $HOST_IP:7222 $HOST_IP:7221

redis-cli -h $HOST_IP -p 7211 \
 --cluster add-node \
 --cluster-slave $HOST_IP:7232 $HOST_IP:7231

