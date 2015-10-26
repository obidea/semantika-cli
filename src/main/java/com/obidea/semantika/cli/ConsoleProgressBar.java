package com.obidea.semantika.cli;

import java.text.DecimalFormat;

import com.obidea.semantika.materializer.IProgressMonitor;

public class ConsoleProgressBar implements IProgressMonitor
{
   private static final int PROGRESS_LENGTH = 70;
   private static final int REFRESH_RATE = 500; // in milliseconds

   private int mMax = 0;
   private int mCurrent = 0;
   private int mProduced = 0;
   private int mTotalProduced = 0;
   private long mStart = 0;
   private long mLastUpdate = 0;

   private static DecimalFormat mOutputTriplesFormat = new DecimalFormat("###,###,###,###"); //$NON-NLS-1$
   private static DecimalFormat mThroughputFormat = new DecimalFormat("###,###,###"); //$NON-NLS-1$

   public ConsoleProgressBar()
   {
      // NO-OP
   }

   @Override
   public void start(int max)
   {
      reset();
      mMax = max;
      mStart = System.currentTimeMillis();
   }

   protected void reset()
   {
      mMax = 0;
      mCurrent = 0;
      mProduced = 0;
      mTotalProduced = 0;
      mStart = 0;
      mLastUpdate = 0;
   }

   @Override
   public void advanced()
   {
      // NO-OP
   }

   @Override
   public void advanced(int value)
   {
      mCurrent++;
      mProduced = value;
      mTotalProduced += mProduced;
      if ((System.currentTimeMillis() - mLastUpdate) > REFRESH_RATE) {
         mLastUpdate = System.currentTimeMillis();
         printBar(false);
      }
   }

   @Override
   public void finish()
   {
      mCurrent = mMax;
      printBar(true);
   }

   private void printBar(boolean finished)
   {
      double numbar = Math.floor(PROGRESS_LENGTH * (double) mCurrent / (double) mMax);
      String strbar = ""; //$NON-NLS-1$
      int ii = 0;
      for (ii = 0; ii < numbar; ii++) {
         strbar += "="; //$NON-NLS-1$
      }
      strbar += ">"; //$NON-NLS-1$
      for (ii = (int) numbar; ii < PROGRESS_LENGTH; ii++) {
         strbar += " "; //$NON-NLS-1$
      }
      
      int percent = (int) Math.floor(100 * (double) mCurrent / (double) mMax);
      long elapsed = (System.currentTimeMillis() - mStart);
      int throughput = 0;
      if (elapsed > 0) {
         throughput = (int) (mTotalProduced / (double) (elapsed / 1000));
      }
      
      String strtriples = mOutputTriplesFormat.format(mTotalProduced);
      String strthroughput = mThroughputFormat.format(throughput);
      String strend = String.format("%-15s (%sT/s)", strtriples, strthroughput); //$NON-NLS-1$
      String outputMsg = String.format("%3d%%[%s] %s", percent, strbar, strend); //$NON-NLS-1$
      System.out.print(outputMsg);
      
      if (finished) {
         int seconds = (int) (elapsed / 1000) % 60;
         int minutes = (int) (elapsed / 1000) / 60;
         String finishMsg = String.format("%02d:%02d (%sT/s) | %s Triples - Done.", minutes, seconds, strthroughput, strtriples); //$NON-NLS-1$
         System.out.print("\n"); //$NON-NLS-1$
         System.out.println(finishMsg);
      }
      else {
         System.out.print("\r"); //$NON-NLS-1$
      }
   }
}
