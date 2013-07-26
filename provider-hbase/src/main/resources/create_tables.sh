#!/bin/bash

  # Small script to setup the HBase tables used by OpenTSDB.
  HBASE_HOME=/usr/local/sw/hbase

  test -n "$HBASE_HOME" || {
    echo >&2 'The environment variable HBASE_HOME must be set'
    exit 1
  }
  test -d "$HBASE_HOME" || {
    echo >&2 "No such directory: HBASE_HOME=$HBASE_HOME"
    exit 1
  }

  TSDB_TABLE=${TSDB_TABLE-'tsdb'}
  UID_TABLE=${UID_TABLE-'tsdb-uid'}

  # HBase scripts also use a variable named `HBASE_HOME', and having this
  # variable in the environment with a value somewhat different from what
  # they expect can confuse them in some cases.  So rename the variable.
  hbh=$HBASE_HOME
  unset HBASE_HOME
  exec "$hbh/bin/hbase" shell <<EOF

  disable 'bean'
  drop 'bean'

  create 'bean',
    {NAME => 'd'},
    {NAME => 'p'},
    {NAME => 'r'},
    {NAME => 's'},
    {NAME => 'pr'}

  disable 'iid'
  drop 'iid'
  create 'iid',
    {NAME => 'i'},
    {NAME => 'n'}

  disable 'sid'
  drop 'sid'
  create 'sid',
    {NAME => 'i'},
    {NAME => 'n'}

EOF

