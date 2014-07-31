Semantika CLI
=============

A Unix-based command line utility for debugging, querying and exporting data.

Latest news: [1.3 (build 17.1) is available](https://github.com/obidea/semantika-cli/releases/tag/v1.3_17.1) (July 31, 2014)

```
usage: semantika queryanswer [OPTIONS...]
           (to execute query answer)
       semantika rdb2rdf [OPTIONS...]
           (to execute RDB2RDF export)
where OPTIONS include:
 -c,--config <path>     path to Semantika configuration file (default=./application.cfg.xml)
 -f,--format <format>   flush result in selected format (options: N3,TTL,XML,JSON)
 -help                  print this message
 -l,--limit <size>      limit the number of returned query result
 -o,--output <path>     path to output file to flush the result
 -q,--quiet             be extra quiet
 -sparql <arg>          input SPARQL query
 -sql                   show the generated SQL (for 'queryanswer' only)
 -v,--verbose           be extra verbose
 -version               print the version information and exit

Example:
  ./semantika queryanswer -c application.cfg.xml -l 100 -sparql 'SELECT ?x WHERE { ?x a :Person }'
  ./semantika rdb2rdf -c application.cfg.xml -o output.n3 -f N3
```

The tool serve two main functions as: (1) SPARQL query tool and (2) RDB2RDF export tool.

SPARQL Query Tool
-----------------

Here are some examples on querying data in database using SPARQL. Note that when the `-c` is omitted then the system will search the default configuration `application.cfg.xml` at the classpath.

* Simple data query

```
./semantika queryanswer -sparql 'SELECT ?x WHERE { ?x a :Person }' --quiet
```

* Data query with max result number (LIMIT=100)

```
./semantika queryanswer -sparql 'SELECT ?x WHERE { ?x a :Person }' -l 100 --quiet
```

* Show the generated SQL from the given input query.

```
./semantika queryanswer -sparql 'SELECT ?x WHERE { ?x a :Person }' -sql --quiet
```

RDB2RDF Export Tool
-------------------

Here are some examples to export RDB rows into RDF triples which can be useful for importing data to triplestore.

* Export data to NTriples format

```
./semantika rdb2rdf -o output.n3 -f N3
```

* Export data to JSON format in silent mode

```
./semantika rdb2rdf -o output.n3 -f N3 --quiet
```

User's Guide
------------

1. [Download and unzip the latest release](https://github.com/obidea/semantika-cli/releases).
2. Create the application-domain model. There are two types of modelling documents, i.e., an OWL ontology document and mapping document(s). The ontology document is optional.
3. Create the configuration file. It specifies the location of the model documents and the database connection parameters.
4. Place the proper JDBC driver inside the `jdbc/` folder
5. Run the command provided by the tool.

Need Help?
----------

Check [our Wikipage](https://github.com/obidea/semantika-api/wiki) for a brief introduction.
Need more help? Join [OBDA Semantika Forum](https://groups.google.com/forum/#!forum/obda-semantika).
