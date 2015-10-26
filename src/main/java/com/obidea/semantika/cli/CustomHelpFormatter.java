package com.obidea.semantika.cli;

import org.apache.commons.cli.HelpFormatter;

public class CustomHelpFormatter extends HelpFormatter
{
   private String defaultNewLine = System.getProperty("line.separator");

   /**
    * Render the specified text and return the rendered Options
    * in a StringBuffer.
    *
    * @param sb The StringBuffer to place the rendered text into.
    * @param width The number of characters to display per line
    * @param nextLineTabStop The position on the next line for the first tab.
    * @param text The text to be rendered.
    *
    * @return the StringBuffer with the rendered Options contents.
    */
   @Override
   protected StringBuffer renderWrappedText(StringBuffer sb, int width, int nextLineTabStop, String text)
   {
      int pos = findWrapPos(text, width, 0);
      if (pos == -1) {
         sb.append(rtrim(text));
         return sb;
      }
      sb.append(rtrim(text.substring(0, pos))).append(defaultNewLine);
      if (nextLineTabStop >= width) {
         // stops infinite loop happening
         nextLineTabStop = 1;
      }
      
      while (true) {
         text = text.substring(pos);
         pos = findWrapPos(text, width, 0);
         if (pos == -1) {
            sb.append(text);
            return sb;
         }
         if ((text.length() > width) && (pos == nextLineTabStop - 1)) {
            pos = width;
         }
         sb.append(rtrim(text.substring(0, pos))).append(defaultNewLine);
      }
   }
}
