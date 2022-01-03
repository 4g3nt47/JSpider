package com.umarabdul.jspider.plugin;

import java.io.*;
import java.util.*;
import java.net.*;
import org.jsoup.nodes.Element;
import com.umarabdul.jspider.*;
import com.umarabdul.jbrowser.*;


/**
* A JSpider plugin for locating web pages containing atleast one form.
*
* @author Umar Abdul
* @version 1.0
* Date: 01/Oct/2020
*/

public class FormFinder extends Plugin{

  private BufferedWriter writer = null;

  public FormFinder(JSpider spider){
  
    super(spider);
    setPluginName("FormFinder"); // Define the plugin name to be used in console outputs.
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
    ArrayList<String> exts = getSpider().getExts();
    String method = getOption("method"); // Submission method of forms to look for.
    if (method != null){
      method = method.toLowerCase();
      if (!(method.equals("post") || method.equals("get"))){
        printError("Unknown target method: " +method);
        return;
      }
    }
    String outfile = getOption("outfile"); // File to log URLs to.
    if (outfile != null){
      try{
        writer = new BufferedWriter(new FileWriter(outfile));
      }catch(IOException e){
        printError("Error opening output file: " +outfile);
        return;
      }
    }
    URL urlObj = null;
    String path = null;
    JBrowser jb = getBrowser();
    ArrayList<Element> forms = null;
    int count = 0;
    while (true){
      url = getURL();
      if (url == null){ // Shutdown the plugin.
        if (count > 1)
          printSuccess(String.valueOf(count) + " URLs with forms located!");
        if (writer != null){
          try{
            writer.close();
          }catch(IOException e){}
        }
        return;
      }
      try{
        urlObj = new URL(url);
      }catch(MalformedURLException e){
        continue;
      }
      path = urlObj.getPath();
      if (path.length() == 0)
        path += "/index.html";
      boolean valid = false;
      for (String ext : exts){
        if (path.endsWith(ext)){
          valid = true;
          break;
        }
      }
      if (!(valid))
        continue;
      try{
        jb.open(url);
      }catch(JBrowserException e){
        continue;
      }
      forms = jb.getForms();
      if (forms.size() == 0)
        continue;
      if (method == null){
        count++;
        yieldURL(url);
        continue;
      }
      for (Element form : forms){
        if (form.attr("method").toLowerCase().equals(method)){
          count++;
          yieldURL(url);
          break;
        }
      }
    }
  }

  /**
  * Called when a target form is found in a URL.
  * @param url URL containing the form.
  */
  private void yieldURL(String url){

    printSuccess("Form found: " + url);
    if (writer != null){
      try{
        writer.write(url+"\n");
        writer.flush();
      }catch(IOException e){}
    } 
  }
}
