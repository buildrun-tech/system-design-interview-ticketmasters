#!/usr/bin/env bash

#JMETER_HOME=../apache-jmeter-5.6.3

$JMETER_HOME/bin/jmeter.sh -n \
  -t TicketMaster.jmx \
  -l results/load-test-$(date +%Y%m%d-%H%M%S).jtl \
  -e \
  -o reports/load-test-$(date +%Y%m%d-%H%M%S) \
  -Jjmeter.reportgenerator.overall_granularity=1000