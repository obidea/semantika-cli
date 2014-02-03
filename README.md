Semantika CLI
=============

A command line tool for Semantika engine. Best use for testing your domain and application model.

```
usage: semantika [queryanswer|materialize] [OPTION]...
    --config <PATH>     path to Semantika configuration file (default=./configuration.xml)
 -f,--format <FORMAT>   flush result in selected format (FORMAT=N3|Turtle|XML|JSON)
 -help                  print this message
 -l,--limit <SIZE>      limit the number of returned query result
    --output <PATH>     path to output file to flush the result
 -q,--quiet             be extra quiet
    --query <PATH>      path to SPARQL query file
 -sql                   show the generated SQL
 -v,--verbose           be extra verbose
 -version               print the version information and exit
```

The tool serve two main functions as: (1) SPARQL query tool and (2) RDB2RDF export tool.

SPARQL Query Tool
-----------------

Here are some examples on querying data in database using SPARQL (rather than normal SQL).

* Simple data query

```
./semantika queryanswer --config=configuration.xml --query=query.txt --quiet
```

* Data query with max result number

```
./semantika queryanswer --config=configuration.xml --query=query.txt -l 100 --quiet
```

* Show the generated SQL from the given input query.

```
./semantika queryanswer --config=configuration.xml --query=query.txt -sql --quiet
```

RDB2RDF Export Tool
-------------------

Here are some examples to export RDB rows into RDF triples which can be useful for importing data to triplestore.

* Export data to NTriples format

```
./semantika materialize --config=configuration.xml --output=output.n3 -f N3
```

* Export data to JSON format in silent mode

```
./semantika materialize --config=configuration.xml --output=output.n3 -f N3 --quiet
```