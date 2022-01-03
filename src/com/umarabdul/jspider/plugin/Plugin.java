package com.umarabdul.jspider.plugin;

import java.util.*;
import com.umarabdul.jspider.*;
import com.umarabdul.jbrowser.*;


/**
* This is an abtract class designed for JSpider's plugins.
* All plugins must inherit this class and overide the getRequiredOptions() and run() methods.
* JSpider allows the usage of multiple plugins at a time. However, all plugins share same options
* data, which could cause problems when two or more active plugins have an option with the same name.
*
* @author Umar Abdul
* @version 1.0
* Date: 30/Sep/2020
*/

public abstract class Plugin implements Runnable{

  private JSpider spider;
  private HashMap<String, String> options;
  private ArrayList<String> outputQueue;
  private int fetchCount;
  private String pluginName;

  /**
  * Plugin's standard constructor.
  * @param spider Instance of JSpider in use.
  */
  public Plugin(JSpider spider){

    this.spider = spider;
    this.options = spider.getPluginOptions();
    outputQueue = spider.getOutputQueue();
    fetchCount = 0;
    pluginName = "plugin";
  }

  /**
  * Obtain an array of strings containing names of all required options for the plugin, null if none required.
  * Used by JSpider to validate options before starting a plugin.
  * @return A string array of option names.
  */
  public abstract String[] getRequiredOptions();

  /**
  * Obtain the value of the given option name (case insensitive). Note: options are shared by all active plugins.
  * @param name Name of the target option.
  * @return Value of the target option.
  */
  public final String getOption(String name){
    return options.get(name.toLowerCase());
  }

  /**
  * Obtain a URL from JSpider's output queue, without actually removing it.
  * This function will block until a url is obtained or JSpider is no longer active,
  * in which case it will return null.
  * @return A URL, {@code null} on failure.
  */
  public final String getURL(){

    while (true){
      if (fetchCount < outputQueue.size()){
        fetchCount++;
        return outputQueue.get(fetchCount-1);
      }
      if (fetchCount >= outputQueue.size() && spider.isActive() == false)
        return null;
      try{
        Thread.sleep(50);
      }catch(InterruptedException e){}
    }
  }

  /**
  * Obtain a configured instance of JBrowser that is used by JSpider.
  * @return Instance of JBrowser.
  */
  public final JBrowser getBrowser(){
    return spider.getBrowser();
  }

  /**
  * Obtain the instance of JSpider in use.
  * @return Instance of JSpider in use.
  */
  public final JSpider getSpider(){
    return spider;
  }

  /**
  * Set the name of the plugin to use in console outputs.
  * @param name Name of plugin.
  */
  public final void setPluginName(String name){
    pluginName = name;
  }

  /**
  * Print plugin success message to console.
  * @param msg Message to print.
  */
  public final void printSuccess(String msg){
    System.out.println(String.format("[+] %s: %s", pluginName, msg));
  }

  /**
  * Print plugin status message to console.
  * @param msg Message to print.
  */
  public final void printStatus(String msg){
    System.out.println(String.format("[*] %s: %s", pluginName, msg));
  }

  /**
  * Print plugin error message to console.
  * @param msg Message to print.
  */
  public final void printError(String msg){
    System.out.println(String.format("[-] %s: %s", pluginName, msg));
  }

  /**
  * Print plugin warning message to console.
  * @param msg Message to print.
  */
  public final void printWarning(String msg){
    System.out.println(String.format("[!] %s : %s", pluginName, msg));
  }

}