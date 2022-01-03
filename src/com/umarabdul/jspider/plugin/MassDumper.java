package com.umarabdul.jspider.plugin;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import com.umarabdul.jspider.*;
import com.umarabdul.jbrowser.*;


/**
* A JSpider plugin for mass download of specific files.
*
* @author Umar Abdul
* @version 1.0
* Date: 01/Oct/2020
*/

public class MassDumper extends Plugin{

  private String outdir = null;
  private String[] exts = null;
  private int workers = 0;
  private int downloaded = 0;

  public MassDumper(JSpider spider){

    super(spider);
    setPluginName("MassDumper"); // Define the plugin name to be used in console outputs.
  }
  
  /**
  * Return a string array containing the names of options required by this plugin.
  * @return Array of option names.
  */
  @Override
  public String[] getRequiredOptions(){

    String opts[] = {"exts", "outdir"};
    return opts;
  }

  /**
  * Called in a background thread to start the plugin.
  */
  @Override
  public void run(){

    String url = null;
    exts = getOption("exts").split(";"); // Target file extensions.
    outdir = getOption("outdir"); // Directory to write files to.
    int threads = 5; // Max number of download threads to run at a time.
    if (getOption("threads") != null)
      threads = Integer.valueOf(getOption("threads"));
    int maxdl = 100; // Max number of files to download.
    if (getOption("max") != null)
      maxdl = Integer.valueOf(getOption("max"));
    String path = "";
    File file = null;
    Thread t = null;
    URL urlObj = null;
    while (true){
      if (downloaded >= maxdl){
        printSuccess(String.format("%d files downloaded successfully!", downloaded));
        return;
      }
      url = getURL();
      if (url == null){ // shutdown the plugin.
        return;
      }
      try{
        urlObj = new URL(url);
      }catch(MalformedURLException e){
        continue;
      }
      try{
        path = new File(urlObj.getPath()).getCanonicalPath();
        if (path.length() == 0 || path.endsWith("/"))
          path += "index.html";
        if (!(path.split("/")[path.split("/").length - 1].contains(".")))
          path += "/index.html";
        if (!(path.startsWith("/")))
          path = "/" + path;
        path = new File(outdir + path).getCanonicalPath();
        boolean valid = false;
        for (String e : exts){
          if (path.endsWith(e) || e.equals("*")){
            valid = true;
            break;
          }
        }
        if (!(valid))
          continue;
        file = new File(path);
        if (file.exists())
          continue;
        synchronized(this){
          file.getParentFile().mkdirs();
        }
        while (workers >= threads){ // wait.
          try{
            Thread.sleep(50);
          }catch(InterruptedException e){}
        }
        t = new Thread(new Downloader(url, file.getPath()));
        t.start();
        workers++;
      }catch(Exception e){
        printError(e.getMessage());
      }
    }
  }

  /**
  * This inner class handles the actual file download.
  */
  private class Downloader implements Runnable{

    private String url;
    private String path;

    public Downloader(String url, String path){
      this.url = url;
      this.path = path;
    }

    @Override
    public void run(){
      
      try{
        JBrowser jb = getBrowser();
        printStatus(String.format("Downloading %s...", url));
        jb.download(url, null, path);
        downloaded++;
        printSuccess(String.format("%s downloaded!", url));
      }catch(JBrowserException e){
        printError(e.getMessage());
      }
      workers--;
    }
  }
}
