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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.obidea.semantika.app.ApplicationFactory;
import com.obidea.semantika.app.ApplicationManager;
import com.obidea.semantika.materializer.IMaterializerEngine;
import com.obidea.semantika.materializer.MaterializationException;
import com.obidea.semantika.materializer.MaterializerEngineException;
import com.obidea.semantika.queryanswer.IQueryEngine;
import com.obidea.semantika.queryanswer.QueryEngineException;
import com.obidea.semantika.queryanswer.QueryEvaluationException;
import com.obidea.semantika.queryanswer.result.IQueryResult;
import com.obidea.semantika.util.StringUtils;

@SuppressWarnings("static-access")
public class Main
{
   private static final String VERSION_NUMBER = "1.0"; //$NON-NLS-1$
   private static final String SEMANTIKA_CORE_VERSION_NUMBER = "1.0"; //$NON-NLS-1$

   private static Options sOptions = new Options();
   static {
      sOptions.addOption(CliEnvironment.HELP, false, "print this message"); //$NON-NLS-1$
      sOptions.addOption(CliEnvironment.VERSION, false, "print the version information and exit"); //$NON-NLS-1$
      sOptions.addOption("v", CliEnvironment.VERBOSE, false, "be extra verbose"); //$NON-NLS-1$ //$NON-NLS-2$
      sOptions.addOption("q", CliEnvironment.QUIET, false, "be extra quiet"); //$NON-NLS-1$ //$NON-NLS-2$
      sOptions.addOption(
            OptionBuilder.withLongOpt(CliEnvironment.CONFIG)
            .withDescription("path to Semantika configuration file (default=./configuration.xml)") //$NON-NLS-1$
            .hasArg()
            .withArgName("PATH") //$NON-NLS-1$
            .create());
      sOptions.addOption(
            OptionBuilder.withLongOpt(CliEnvironment.QUERY)
            .withDescription("path to SPARQL query file") //$NON-NLS-1$
            .hasArg()
            .withArgName("PATH") //$NON-NLS-1$
            .create());
      sOptions.addOption(
            OptionBuilder.withLongOpt(CliEnvironment.OUTPUT)
            .withDescription("path to output file to flush the result") //$NON-NLS-1$
            .hasArg()
            .withArgName("PATH") //$NON-NLS-1$
            .create());
      sOptions.addOption(CliEnvironment.USE_NTRIPLES, false, "flush output in NTriples format"); //$NON-NLS-1$
      sOptions.addOption(CliEnvironment.USE_TURTLE, false, "flush output in Turtle format"); //$NON-NLS-1$
      sOptions.addOption(CliEnvironment.USE_RDFXML, false, "flush output in RDF/XML format"); //$NON-NLS-1$
      sOptions.addOption(CliEnvironment.USE_JSONLD, false, "flush output in JSON-LD format"); //$NON-NLS-1$
   }

   private static HelpFormatter mFormatter = new HelpFormatter();

   public static void main(String[] args)
   {
      @SuppressWarnings("unchecked")
      List<Logger> loggers = Collections.list(LogManager.getCurrentLoggers());
      loggers.add(LogManager.getRootLogger());
      normal(loggers);
      
      try {
         CommandLineParser parser = new GnuParser();
         CommandLine optionLine = parser.parse(sOptions, args);
         
         if (optionLine.hasOption(CliEnvironment.VERSION)) {
            printVersion();
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
      if (optionLine.hasOption(CliEnvironment.VERBOSE)) {
         verbose(loggers);
      }
      else if (optionLine.hasOption(CliEnvironment.QUIET)) {
         quiet(loggers);
      }
   }

   private static void executeOperation(String operation, CommandLine optionLine) throws Exception
   {
      File config = determineConfigurationFile(optionLine);
      ApplicationManager manager = new ApplicationFactory().configure(config).createApplicationManager();
      
      if (operation.equals(CliEnvironment.QUERYANSWER_OP)) {
         File fquery = determineQueryFile(optionLine);
         IQueryEngine engine = createQueryEngine(manager);
         queryanswer(engine, fquery);
      }
      else if (operation.equals(CliEnvironment.MATERIALIZE_OP)) {
         String format = determineOutputFormat(optionLine);
         File fout = determineOutputFile(optionLine);
         IMaterializerEngine engine = createMaterializerEngine(manager, format);
         materialize(engine, fout);
      }
      else {
         printUsage();
      }
   }

   private static void queryanswer(IQueryEngine engine, File fquery) throws QueryEngineException, QueryEvaluationException, IOException
   {
      engine.start();
      IQueryResult result = engine.evaluate(FileUtils.readFileToString(fquery, "UTF-8")); //$NON-NLS-1$
      flushResult(result);
      engine.stop();
   }

   private static void flushResult(IQueryResult result) throws IOException
   {
      while (result.next()) {
         System.out.println(result.getValueList().toString());
      }
   }

   private static void materialize(IMaterializerEngine engine, File fout) throws MaterializerEngineException, MaterializationException
   {
      engine.start();
      engine.materialize(fout);
      engine.stop();
   }

   private static IQueryEngine createQueryEngine(ApplicationManager manager)
   {
      return manager.createQueryEngine();
   }

   private static IMaterializerEngine createMaterializerEngine(ApplicationManager manager, String format)
   {
      if (format.equals("NTriples")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useNTriples();
      }
      else if (format.equals("Turtle")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useTurtle();
      }
      else if (format.equals("RDF/XML")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useRdfXml();
      }
      else if (format.equals("RDF/JSON")) { //$NON-NLS-1$
         return manager.createMaterializerEngine().useRdfJson();
      }
      return null; // should never goes here
   }

   private static File determineQueryFile(CommandLine optionLine)
   {
      String query = optionLine.getOptionValue(CliEnvironment.QUERY); //$NON-NLS-1$
      if (StringUtils.isEmpty(query)) {
         System.err.println("Query file is missing"); //$NON-NLS-1$
         System.exit(1);
      }
      return new File(query);
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
      String config = optionLine.getOptionValue(CliEnvironment.CONFIG); //$NON-NLS-1$
      if (StringUtils.isEmpty(config)) {
         return new File("configuration.xml"); //$NON-NLS-1$
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
      if (optionLine.hasOption(CliEnvironment.USE_NTRIPLES)) {
         return "NTriples"; //$NON-NLS-1$
      }
      else if (optionLine.hasOption(CliEnvironment.USE_TURTLE)) {
         return "Turtle"; //$NON-NLS-1$
      }
      else if (optionLine.hasOption(CliEnvironment.USE_RDFXML)) {
         return "RDF/XML"; //$NON-NLS-1$
      }
      else if (optionLine.hasOption(CliEnvironment.USE_JSONLD)) {
         return "RDF/JSON"; //$NON-NLS-1$
      }
      return "Turtle"; //$NON-NLS-1$ - by default
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
       System.err.println("Operation is missing"); //$NON-NLS-1$
       System.exit(1);
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
      String output = optionLine.getOptionValue(CliEnvironment.OUTPUT);
      if (!StringUtils.isEmpty(output)) {
         return new File(output);
      }
      System.err.println("Output file is missing"); //$NON-NLS-1$
      System.exit(1);
      return null;
   }

   private static void printVersion()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("semantika version ");
      sb.append("\"").append(VERSION_NUMBER).append("\"");
      sb.append("\n");
      sb.append("Semantika Core Runtime ");
      sb.append("(build ").append(SEMANTIKA_CORE_VERSION_NUMBER).append(")");
      sb.append("\n");
      System.out.println(sb.toString());
   }

   private static void normal(List<Logger> loggers)
   {
      for ( Logger logger : loggers ) {
         logger.setLevel(Level.INFO);
     }
   }

   private static void verbose(List<Logger> loggers)
   {
      for ( Logger logger : loggers ) {
          logger.setLevel(Level.DEBUG);
      }
   }

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
      String helpText = String.format("semantika [%s|%s] [OPTION]...", CliEnvironment.QUERYANSWER_OP, CliEnvironment.MATERIALIZE_OP); //$NON-NLS-1$
      mFormatter.printHelp(helpText, sOptions);
   }
}
