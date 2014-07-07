/*
 * Copyright (c) 2013-2014 Obidea
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.obidea.semantika.cli;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.obidea.semantika.app.ApplicationFactory;
import com.obidea.semantika.app.ApplicationManager;
import com.obidea.semantika.exception.SemantikaException;
import com.obidea.semantika.materializer.IMaterializerEngine;
import com.obidea.semantika.materializer.MaterializationException;
import com.obidea.semantika.materializer.MaterializerEngineException;
import com.obidea.semantika.queryanswer.IQueryEngine;
import com.obidea.semantika.queryanswer.IQueryEngineExt;
import com.obidea.semantika.queryanswer.QueryEngineException;
import com.obidea.semantika.queryanswer.result.IQueryResult;
import com.obidea.semantika.util.StringUtils;

@SuppressWarnings("static-access")
public class Main
{
   private static final String VERSION_NUMBER = "1.3"; //$NON-NLS-1$
   private static final String SEMANTIKA_CORE_VERSION_NUMBER = "1.7"; //$NON-NLS-1$

   private static Options sOptions = new Options();
   static {
      sOptions.addOption(Environment.HELP, false, "print this message"); //$NON-NLS-1$
      sOptions.addOption(Environment.VERSION, false, "print the version information and exit"); //$NON-NLS-1$
      sOptions.addOption(Environment.SHOW_SQL, false, "show the generated SQL (for 'queryanswer' only)"); //$NON-NLS-1$
      sOptions.addOption(Environment.VERBOSE_SHORTCUT, Environment.VERBOSE, false, "be extra verbose"); //$NON-NLS-1$
      sOptions.addOption(Environment.QUIET_SHORTCUT, Environment.QUIET, false, "be extra quiet"); //$NON-NLS-1$
      sOptions.addOption(Environment.QUERY, true, "input SPARQL query"); //$NON-NLS-1$
      sOptions.addOption(
            OptionBuilder.withLongOpt(Environment.CONFIG)
            .withDescription("path to Semantika configuration file (default=./application.cfg.xml)") //$NON-NLS-1$
            .hasArg()
            .withArgName("path") //$NON-NLS-1$
            .create(Environment.CONFIG_SHORTCUT));
      sOptions.addOption(
            OptionBuilder.withLongOpt(Environment.OUTPUT)
            .withDescription("path to output file to flush the result") //$NON-NLS-1$
            .hasArg()
            .withArgName("path") //$NON-NLS-1$
            .create(Environment.OUTPUT_SHORTCUT));
      sOptions.addOption(
            OptionBuilder.withLongOpt(Environment.FORMAT)
            .withDescription("flush result in selected format (options: N3,TTL,XML,JSON)") //$NON-NLS-1$
            .hasArg()
            .withArgName("format") //$NON-NLS-1$
            .create(Environment.FORMAT_SHORTCUT));
      sOptions.addOption(
            OptionBuilder.withLongOpt(Environment.LIMIT)
            .withDescription("limit the number of returned query result") //$NON-NLS-1$
            .hasArg()
            .withArgName("size") //$NON-NLS-1$
            .create(Environment.LIMIT_SHORTCUT));
   }

   private static CustomHelpFormatter mFormatter = new CustomHelpFormatter();

   public static void main(String[] args)
   {
      @SuppressWarnings("unchecked")
      List<Logger> loggers = Collections.list(LogManager.getCurrentLoggers());
      loggers.add(LogManager.getRootLogger());
      normal(loggers);
      
      try {
         CommandLineParser parser = new GnuParser();
         CommandLine optionLine = parser.parse(sOptions, args);
         
         if (optionLine.hasOption(Environment.VERSION)) {
            printVersion();
            System.exit(0);
         }
         if (optionLine.hasOption(Environment.HELP)) {
            printUsage();
            System.exit(0);
         }
         
         String operation = determineOperation(args);
         if (StringUtils.isEmpty(operation)) {
             printUsage();
         }
         else {
            setupLoggers(optionLine, loggers);
            executeOperation(operation, optionLine);
         }
      }
      catch (Exception e) {
         System.err.println("Unexpected exception:" + e.getMessage()); //$NON-NLS-1$
         System.exit(1);
      }
   }

   private static void setupLoggers(CommandLine optionLine, List<Logger> loggers)
   {
      if (optionLine.hasOption(Environment.VERBOSE)) {
         verbose(loggers);
      }
      else if (optionLine.hasOption(Environment.QUIET)) {
         quiet(loggers);
      }
   }

   private static void executeOperation(String operation, CommandLine optionLine) throws Exception
   {
      if (operation.equals(Environment.QUERYANSWER_OP)) {
         File config = determineConfigurationFile(optionLine);
         ApplicationManager manager = new ApplicationFactory().configure(config).createApplicationManager();
         
         String sparql = determineInputSparql(optionLine);
         int limit = determineResultLimit(optionLine);
         IQueryEngine engine = createQueryEngine(manager);
         
         boolean showSql = determineShowSql(optionLine);
         queryanswer(engine, sparql, limit, showSql);
      }
      else if (operation.equals(Environment.MATERIALIZE_OP)) {
         File config = determineConfigurationFile(optionLine);
         ApplicationManager manager = new ApplicationFactory().configure(config).createApplicationManager();
         
         String format = determineOutputFormat(optionLine);
         File fout = determineOutputFile(optionLine);
         IMaterializerEngine engine = createMaterializerEngine(manager, format);
         materialize(engine, fout);
      }
      else {
         System.err.println("Invalid command"); //$NON-NLS-1$
         printUsage();
      }
   }

   private static void queryanswer(IQueryEngine engine, String sparql, int limit, boolean showSql) throws QueryEngineException, SemantikaException, IOException
   {
      engine.start();
      if (showSql) {
         printSql(sparql, engine);
      }
      else {
         flushResult(evaluateQuery(sparql, engine, limit));
      }
      engine.stop();
   }

   private static void printSql(String sparql, IQueryEngine engine) throws SemantikaException
   {
      String sql = engine.translate(sparql);
      System.out.println(sql);
   }

   private static IQueryResult evaluateQuery(String sparql, IQueryEngine engine, int limit) throws SemantikaException
   {
      if (engine instanceof IQueryEngineExt) {
         return ((IQueryEngineExt) engine).createQuery(sparql).setMaxResults(limit).evaluate();
      }
      return engine.evaluate(sparql);
   }

   private static void materialize(IMaterializerEngine engine, File fout) throws MaterializerEngineException, MaterializationException
   {
      engine.start();
      engine.materialize(fout, new ConsoleProgressBar());
      engine.stop();
   }

   private static IQueryEngine createQueryEngine(ApplicationManager manager)
   {
      return manager.createQueryEngine();
   }

   private static IMaterializerEngine createMaterializerEngine(ApplicationManager manager, String format)
   {
      if (format.equals("N3")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useNTriples();
      }
      else if (format.equals("TTL")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useTurtle();
      }
      else if (format.equals("XML")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useRdfXml();
      }
      else if (format.equals("JSON")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useRdfJson();
      }
      return null; // should never goes here
   }

   /**
    * Flush the query result to stdout.
    */
   private static void flushResult(IQueryResult result) throws IOException
   {
      while (result.next()) {
         System.out.println(result.getValueList().toString());
      }
   }

   /**
    * Determines the input SPARQL string.
    * 
    * @param optionLine
    *           The command-line arguments passed in.
    * @return The query string.
    */
   private static String determineInputSparql(CommandLine optionLine)
   {
      String query = optionLine.getOptionValue(Environment.QUERY).trim(); //$NON-NLS-1$
      if (StringUtils.isEmpty(query)) {
         System.err.println("Input query is missing"); //$NON-NLS-1$
         System.exit(1);
      }
      return query;
   }

   /**
    * Determines the file to use for loading the configuration.
    * 
    * @param optionLine
    *           The command-line arguments passed in.
    * @return The path of the configuration file on disk.
    */
   private static File determineConfigurationFile(CommandLine optionLine)
   {
      String config = optionLine.getOptionValue(Environment.CONFIG); //$NON-NLS-1$
      if (StringUtils.isEmpty(config)) {
         return new File(ApplicationFactory.DEFAULT_CONFIGURATION_FILENAME); //$NON-NLS-1$
      }
      return new File(config);
   }

   /**
    * Determines the writer format for flushing the output.
    * 
    * @param args
    *           The command-line arguments passed in.
    * @return The selected writer format.
    */
   private static String determineOutputFormat(CommandLine optionLine)
   {
      String format = optionLine.getOptionValue(Environment.FORMAT);
      if (StringUtils.isEmpty(format)) {
         format = "TTL"; //$NON-NLS-1$ - by default
      }
      return format;
   }

   /**
    * Determines the limit of the query result to be fetched.
    * 
    * @param optionLine
    *           The command-line arguments passed in.
    * @return The limit amount.
    */
   private static int determineResultLimit(CommandLine optionLine)
   {
      String limit = optionLine.getOptionValue(Environment.LIMIT);
      if (StringUtils.isEmpty(limit)) {
         return -1;
      }
      return Integer.parseInt(limit);
   }

   /**
    * Determines if the generated SQL should be printed instead of executing it.
    * 
    * @param optionLine
    *           The command-line arguments passed in.
    */
   private static boolean determineShowSql(CommandLine optionLine)
   {
      return optionLine.hasOption(Environment.SHOW_SQL);
   }

   /**
    * Determines the command operation Semantika CLI should execute.
    * 
    * @param args
    *           The command-line arguments passed in.
    * @return The operation string or <code>null</code> if it could not be
    *         determined.
    */
   private static String determineOperation(String[] args)
   {
       for (String arg : args) {
           if (!arg.startsWith("-")) { //$NON-NLS-1$
               return arg;
           }
       }
       return null;
   }

   /**
    * Determines the file to use to flush out the result.
    * 
    * @param args
    *           The command-line arguments passed in.
    * @return The path of the output file on disk.
    */
   private static File determineOutputFile(CommandLine optionLine)
   {
      String output = optionLine.getOptionValue(Environment.OUTPUT);
      if (!StringUtils.isEmpty(output)) {
         return new File(output);
      }
      System.err.println("Output file is missing"); //$NON-NLS-1$
      System.exit(1);
      return null;
   }

   /**
    * Print tool version.
    */
   private static void printVersion()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("semantika version "); //$NON-NLS-1$
      sb.append("\"").append(VERSION_NUMBER).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("\n"); //$NON-NLS-1$
      sb.append("Semantika Core Runtime "); //$NON-NLS-1$
      sb.append("(build ").append(SEMANTIKA_CORE_VERSION_NUMBER).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
      System.out.println(sb.toString());
   }

   /**
    * Set all loggers in INFO level.
    */
   private static void normal(List<Logger> loggers)
   {
      for ( Logger logger : loggers ) {
         logger.setLevel(Level.INFO);
     }
   }

   /**
    * Set all loggers in DEBUG level.
    */
   private static void verbose(List<Logger> loggers)
   {
      for ( Logger logger : loggers ) {
          logger.setLevel(Level.DEBUG);
      }
   }

   /**
    * Disable all loggers.
    */
   private static void quiet(List<Logger> loggers)
   {
      for ( Logger logger : loggers ) {
          logger.setLevel(Level.OFF);
      }
   }

   /**
    * Prints the usage instructions on the console.
    */
   private static void printUsage()
   {
      StringBuilder usage = new StringBuilder();
      usage.append(String.format("semantika %s [OPTIONS...]\n", Environment.QUERYANSWER_OP));
      usage.append("           (to execute query answer)\n");
      usage.append(String.format("       semantika %s [OPTIONS...]\n", Environment.MATERIALIZE_OP));
      usage.append("           (to execute RDB2RDF export)");
      String header = "where OPTIONS include:"; //$NON-NLS-1$
      String footer =
            "\nExample:\n" + //$NON-NLS-1$
            "  ./semantika queryanswer --config=application.cfg.xml --query=query.txt -l 100\n" + //$NON-NLS-1$
            "  ./semantika rdb2rdf --config=application.cfg.xml --output=output.n3 -f N3"; //$NON-NLS-1$
      mFormatter.setOptionComparator(null);
      mFormatter.printHelp(400, usage.toString(), header, sOptions, footer);
   }
}
