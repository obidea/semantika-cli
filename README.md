Semantika CLI
=============

A command line tool for Semantika engine. This tool is best use for testing your application-domain model.

```
usage: semantika queryanswer [OPTIONS...]
           (to execute query answer)
       semantika materialize [OPTIONS...]
           (to execute RDB2RDF export)
where OPTIONS include:
    --config <=path>             path to Semantika configuration file (default=./configuration.xml)
 -f,--format <N3|TTL|XML|JSON>   flush result in selected format
 -help                           print this message
 -l,--limit <size>               limit the number of returned query result
    --output <=path>             path to output file to flush the result
 -q,--quiet                      be extra quiet
    --query <=path>              path to SPARQL query file
 -sql                            show the generated SQL
 -v,--verbose                    be extra verbose
 -version                        print the version information and exit

Example:
  ./semantika queryanswer --config=configuration.xml --query=query.txt -l 100
  ./semantika materialize --config=configuration.xml --output=output.n3 -f N3
```

The tool serve two main functions as: (1) SPARQL query tool and (2) RDB2RDF export tool.

SPARQL Query Tool
-----------------

Here are some examples on querying data in database using SPARQL (rather than normal SQL).

* Simple data query

```
./semantika queryanswer --config=configuration.xml --query=query.txt --quiet
```

* Data query with max result number (LIMIT=100)

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

User's Guide
------------

1. Download and unzip the binaries: [semantika-cli-1.1.zip](https://github.com/obidea/semantika-cli/releases/download/v1.1/semantika-cli-1.1.zip)
2. Create your application-domain model. There are two input documents for declaring the specification, i.e.,
  * `ontology.owl`: specifies the names or labels used in the application.
  * `mapping.xml`: specifies the interconnection between labels and records in database (e.g., Label 'Employee' is connected to records in column 'EMP_ID' in table 'EMPLOYEE_TNT_2010')
3. Create the configuration file that points the location of the input domain documents and other application settings.
4. Put the JDBC driver according to your database product.
5. Run the command provided by the tool.

Need Help?
----------

Check [our Wikipage](https://github.com/obidea/semantika-api/wiki) for a brief introduction.
Need more help? Join [OBDA Semantika Forum](https://groups.google.com/forum/#!forum/obda-semantika).
