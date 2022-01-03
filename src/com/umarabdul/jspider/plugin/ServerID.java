package com.umarabdul.jspider.plugin;

import java.io.*;
import java.util.*;
import org.jsoup.Connection.Method;
import com.umarabdul.jspider.*;
import com.umarabdul.jbrowser.*;


/**
* A JSpider plugin for identifying web servers using the "Server" HTTP response header.
*
* @author Umar Abdul
* @version 1.0
* Date: 01/Oct/2020
*/

public class ServerID extends Plugin{

  public ServerID(JSpider spider){
  
    super(spider);
    setPluginName("ServerID"); // Define the plugin name to be used in console outputs.
  }
  
  /**
  * Return a string array containing the names of options required by this plugin.
  * @return Array of option names.
  */
  @Override
  public String[] getRequiredOptions(){

    String opts[] = {};
    return opts;
  }

  /**
  * Called in a background thread to start the plugin.
  */
  @Override
  public void run(){

    String url = null;
    ArrayList<String> scanned = new ArrayList<String>();
    JBrowser jb = getBrowser();
    String host = null;
    String banner = null;
    String outfile = getOption("outfile");
    BufferedWriter writer = null;
    if (outfile != null){
      try{
        writer = new BufferedWriter(new FileWriter(outfile));
        writer.write("HOST\tSERVER\n");
        writer.flush();
      }catch(IOException e){
        printError(String.format("Error creating output file: %s", e.getMessage()));
        return;
      }
    }
    while (true){
      url = getURL();
      if (url == null){ // Shutdown the plugin.
        if (writer != null){
          try{
            writer.close();
          }catch(IOException e){}
        }
        return;
      }
      try{
        host = url.split("://")[1];
        host = host.split("/")[0];
      }catch(IndexOutOfBoundsException e){
        continue;
      }
      if (scanned.contains(host))
        continue;
      try{
        jb.open(String.format("%s://%s/", url.split("://")[0], host), Method.HEAD, null);
      }catch(JBrowserException e){
        continue;
      }
      scanned.add(host);
      banner = jb.getResponseHeader("Server");
      if (banner == null){
        printError("Error identifying host: " +host);
        continue;
      }
      banner = banner.trim();
      printSuccess(String.format("Host: %s  Server: %s", host, banner));
      if (writer != null){
        try{
          writer.write(String.format("%s\t%s\n", host, banner));
          writer.flush();
        }catch(IOException e){}
      }
    }
  }

}
