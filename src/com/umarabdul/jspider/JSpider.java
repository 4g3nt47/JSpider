package com.umarabdul.jspider;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Constructor;
import com.umarabdul.jspider.plugin.Plugin;
import com.umarabdul.jbrowser.*;
import com.umarabdul.argparser.ArgParser;


/**
* A powerful, multi-threaded web crawler using JBrowser.
* Designed to be easily utilised by other Java programs with ease.
* Allows for live collection of URLs while the spider is still running.
*
* @author Umar Abdul
* @version 1.1
* Date: 22/Aug/2020
*/

public class JSpider implements Runnable{

  private ArrayList<String> exts; // Page extensions considered to be web pages.
  private ArrayList<String> crawled; // URLs that have been parsed.
  private ArrayList<String> urls; // All URLs yielded, used by JSpider to avoid duplicate outputs.
  private ArrayList<String> inputQueue; // Queue of URLs yet to be parsed.
  private ArrayList<String> outputQueue; // URLs output queue for use by the invoking code.
  private ArrayList<String> statusQueue; // Logs/status queue for use by the invoking code.
  private URL baseUrl; // Starting URL.
  private String baseHost; // Hostname of base URL.
  private String[] ignore; // URLs containing these keywords will not be opened.
  private int threads; // Number of threads to run.
  private int workers; // Number of active threads.
  private boolean followExternal; // Control whether external URLs (not in same host with baseUrl) are spidered.
  private boolean hideExternal; // Control whether external URLs are yielded to the invoking code.
  private int timeout; // URL read timeout.
  private HashMap<String, String> headers; // Request headers to pass to JBrowser.
  private HashMap<String, String> cookies; // Request cookies to pass to JBrowser.
  private HashMap<String, String> proxy; // Proxy host and port to pass to JBrowser.
  private int parsing; // Number of threads that are actively parsing pages. Used to avoid early exit of threads when inputQueue is exhausted, but a page is being parsed.
  private boolean active; // Set to true when the spider is running.
  private int max; // Max number of URLs to parse.
  private ArrayList<Plugin> plugins; // Plugins to activate.
  private HashMap<String, String> pluginOptions; // Options defined for the plugins.

  /**
  * JSpider's constructor.
  * @param baseUrl Starting page URL.
  * @param threads Number of threads to use.
  * @param timeout Page read timeout, in milliseconds.
  * @throws MalformedURLException on URL parse failure.
  */
  public JSpider(String baseUrl, int threads, int timeout) throws MalformedURLException{

    exts = new ArrayList<String>();
    // Extension of URLs to consider as web pages.
    String[] defExts = {"/", ".html", ".htm", ".htmls", ".dhtml", ".xhtml", ".php", ".php3", ".asp", ".aspx", ".ece"};
    addExt(defExts);
    crawled = new ArrayList<String>();
    urls = new ArrayList<String>();
    inputQueue = new ArrayList<String>();
    outputQueue = new ArrayList<String>();
    statusQueue = new ArrayList<String>();
    this.baseUrl = new URL(baseUrl);
    baseHost = this.baseUrl.getHost();
    ignore = new String[0];
    this.threads = Math.max(1, threads);
    workers = 0;
    followExternal = false;
    hideExternal = false;
    this.timeout = timeout;
    headers = new HashMap<String, String>();
    cookies = new HashMap<String, String>();
    proxy = new HashMap<String, String>();
    parsing = 0;
    active = false;
    max = 100;
    plugins = new ArrayList<Plugin>();
    pluginOptions = new HashMap<String, String>();
  }

  /**
  * Add an extension to the list of web page extensions.
  * @param ext Extension to add.
  */
  public void addExt(String ext){
    if (!(exts.contains(ext)))
      exts.add(ext);
  }

  /**
  * Add extensions from given array.
  * @param exts Extensions to add.
  */
  public void addExt(String[] exts){
    for (String ext : exts)
      addExt(ext);
  }

  /**
  * Return an ArrayList of web page extensions in use.
  * @return An ArrayList of web page extensions.
  */
  public ArrayList<String> getExts(){

    ArrayList<String> copy = new ArrayList<String>();
    for (String ext : exts)
      copy.add(ext);
    return copy;
  }

  /**
  * Determine whether to spider URLs not in the base URL's domain.
  * @param flag {@code true/false}.
  */
  public void setFollowExternal(boolean flag){
    followExternal = flag;
  }

  /**
  * Determine whether external URLs are yeilded to the invoking code.
  * @param flag {@code true/false}.
  */
  public void setHideExternal(boolean flag){
    hideExternal = flag;
  }

  /**
  * Set max number of pages to parse before aborting.
  * @param max Max number of pages to parse.
  */
  public void setMax(int max){
    this.max = max;
  }

  /**
  * Set read timeout for JBrowser.
  * @param timeout Read timeout in milliseconds.
  */
  public void setTimeout(int timeout){
    this.timeout = timeout;
  }

  /**
  * Set keywords of URLs that are not to be opened. The matching is case-insensitive.
  * @param keywords Array of keywords.
  */
  public void setIgnore(String[] keywords){
    ignore = keywords;
  }

  /**
  * Load plugins defined by user.
  * @param names Name of plugins.
  * @return {@code true} if all plugins are loaded successfully.
  */
  public boolean loadPlugins(String[] names){

    Class<?> cl;
    Constructor<?> cons;
    Plugin pl;
    String path = "";
    for (String name : names){
      if (name.equals("Plugin"))
        continue;
      path = "com.umarabdul.jspider.plugin." + name;
      try{
        cl = Class.forName(path);
        cons = cl.getConstructor(JSpider.class);
        pl = (Plugin)(cons.newInstance(this));
        String[] opts = pl.getRequiredOptions();
        if (opts != null){
          for (String opt : opts){
            if (!(pluginOptions.containsKey(opt.toLowerCase()))){
              System.out.println(String.format("[-] JSpider: Plugin option '%s' not defined and is required by %s", opt.toLowerCase(), name));
              return false;
            }
          }
        }
        plugins.add(pl);
      }catch(ClassNotFoundException e){
        System.out.println("[-] JSpider: Invalid plugin: " +name);
        return false;
      }catch(Exception e2){
        System.out.println("[-] JSpider: Error loading plugin: " +name);
        e2.printStackTrace();
        return false;
      }
    }
    return true;
  }

  /**
  * Set an option for loaded plugins.
  * @param name Name of option (will be made case-insensitive).
  * @param value Value to assign to the option.
  */
  public void setPluginOption(String name, String value){
    pluginOptions.put(name.toLowerCase(), value);
  }

  /**
  * Obtain a HashMap of defined plugin options.
  * @return HashMap of defined plugin options.
  */
  public HashMap<String, String> getPluginOptions(){
    return pluginOptions;
  }

  /**
  * Start all loaded plugins. To be called only after JSpider is started successfuly.
  */
  private void startPlugins(){

    Thread t = null;
    for (Plugin pl : plugins){
      t = new Thread(pl);
      t.start();
    }
  }

  /**
  * Check if the spider is actively running.
  * @return {@code true/false}.
  */
  public boolean isActive(){
    return (active == true && workers > 0);
  }

  /**
  * Add a web page URL to targets/input queue.
  * @param url URL to add.
  */
  private void addTarget(String url) throws MalformedURLException{
    
    URL urlObj = new URL(url);
    // Enforce external URL policy.
    if (followExternal == false && urlObj.getHost().endsWith(baseHost) == false)
      return;
    // Validate page extension.
    boolean valid = false;
    for (String ext : exts){
      if (urlObj.getPath().endsWith(ext)){
        valid = true;
        break;
      }
    }
    if (!(valid))
      return;
    // Check if url is to be ignored.
    for (String keyword : ignore){
      if (url.toLowerCase().contains(keyword.toLowerCase()))
        return;
    }
    // Add to targets queue, if not already crawled, and max number of URLs to parse is not exceeded.
    synchronized(this){
      if (inputQueue.contains(url) == false && crawled.contains(url) == false && crawled.size() < max)
        inputQueue.add(url);
    }
  }

  /**
  * Fetch a URL to crawl in a thread-safe manner.
  * @return URL to crawl, {@code null} if none currently available (which doesn't necessarily mean the operation is over).
  */
  private synchronized String getTarget(){
    return ((inputQueue.size() > 0 && crawled.size() < max) ? inputQueue.remove(0) : null);
  }

  /**
  * Obtain the output queue. Used by the invoking code to receive URLs as they are captured.
  * @return An ArrayList that will be continuously updated with captured URLs.
  */
  public ArrayList<String> getOutputQueue(){
    return outputQueue;
  }

  /**
  * Obtain the status queue. Used by the invoking code to access status logs.
  * @return An ArrayList that will be continuously updated with status logs.
  */
  public ArrayList<String> getStatusQueue(){
    return statusQueue;
  }

  /**
  * Obtain an ArrayList of URLs already spidered.
  * Warning: Modifying the returned ArrayList while the spider is active may cause redundancy.
  * @return ArrayList of spidered URLs.
  */
  public ArrayList<String> getCrawled(){
    return crawled;
  }

  /**
  * Obtain total number of URLs obtained by the spider.
  * @return Total number of URLs obtained.
  */ 
  public int getURLCount(){
    return urls.size();
  }

  /**
  * Set request headers.
  * @param headers HashMap of headers to add.
  */
  public void setHeaders(HashMap<String, String> headers){
    this.headers = headers;
  }

  /**
  * Set request cookies.
  * @param cookies HashMap of cookies to add.
  */
  public void setCookies(HashMap<String, String> cookies){
    this.cookies = cookies;
  }

  /**
  * Set proxy for JBrowser.
  * @param host Proxy host.
  * @param port Proxy port.
  */
  public void setProxy(String host, int port){
    proxy.put("host", host);
    proxy.put("port", String.valueOf(port));
  }

  /**
  * Get a configured instance of JBrowser to use. Called once by all threads.
  * @return Configured instance of JBrowser.
  */
  public JBrowser getBrowser(){

    JBrowser jb = new JBrowser();
    jb.setAutoParse(true);
    jb.setCookie(cookies);
    jb.setRequestHeader(headers);
    if (proxy.size() > 0)
      jb.setProxy(proxy.get("host"), Integer.parseInt(proxy.get("port")));
    jb.setTimeout(timeout);
    return jb;
  }

  /**
  * Yield a discovered URL to the invoking code in a thead-safe manner while avoiding duplicates and adding filter.
  * @param url URL to yield.
  */
  private synchronized void yieldURL(String url){
    
    // filter.
    try{
      if (hideExternal && new URL(url).getHost().endsWith(baseHost) == false)
        return;
    }catch(MalformedURLException e){
      return;
    }
    // yield.
    if (!(urls.contains(url))){
      urls.add(url);
      outputQueue.add(url);
    }
  }

  /**
  * This is where the party is hosted ;)
  */
  @Override
  public void run(){

    try{
      Thread.sleep(50); // A delay to allow other threads to be dispatched faster.
    }catch(InterruptedException e1){}
    String targetUrl = null;
    JBrowser jb = getBrowser();
    HashMap<String, ArrayList<String>> rawURLs = null;
    while (active){
      targetUrl = getTarget();
      if (targetUrl == null){
        if (parsing == 0)
          break;
        try{
          Thread.sleep(50); // Wait for pages to be parsed.
        }catch(InterruptedException e2){}
        continue;
      }
      synchronized(this){
        parsing++;
        crawled.add(targetUrl);
        statusQueue.add("[*] Parsing page: " +targetUrl+ "...");
      }
      try{
        jb.open(targetUrl);
      }catch(JBrowserException e3){
        synchronized(this){
          statusQueue.add("[-] JBrowserException: " +e3.getMessage());
          parsing--;
        }
        continue;
      }
      // Extract URLs
      rawURLs = jb.getURLs();
      for (String category : rawURLs.keySet()){
        for (String link : rawURLs.get(category)){
          link = link.split("#")[0];
          try{
            addTarget(link);
            yieldURL(link);
          }catch(MalformedURLException ignored){
            continue;
          }
        }
      }
      synchronized(this){
        parsing--;
      }
    }
    // quit.
    synchronized(this){
      workers--;
    }
  }

  /**
  * Sets the ball rolling. This is a non-blocking function and will return once all threads are dispatched.
  * The invoking code will need to periodically call the {@code isActive()} method to know if the spider is still active.
  * @param startupLog Print startup logs.
  * @return {@code true} if the spider is started successfully.
  */
  public boolean start(boolean startupLog){
    
    // Flush out all queues.
    inputQueue.clear();
    outputQueue.clear();
    statusQueue.clear();
    urls.clear();
    crawled.clear();
    if (startupLog)
      System.out.println(String.format("[*] JSpider: Parsing base URL: %s...", baseUrl.toString()));
    // Parse our base URL.
    JBrowser jb = getBrowser();
    try{
      jb.open(baseUrl.toString());
    }catch(JBrowserException e1){
      if (startupLog)
        System.out.println("[-] JSpider: JBrowserException: " + e1.getMessage());
      return false;
    }
    crawled.add(baseUrl.toString());
    yieldURL(baseUrl.toString());
    // Populate targets queue.
    ArrayList<String> links = jb.getURLs().get("href");
    for (String link : links){
      link = link.split("#")[0];
      try{
        addTarget(link);
        yieldURL(link);
      }catch(MalformedURLException e2){
        statusQueue.add("[-] MalformedURLException: " + link);
      }
    }
    if (inputQueue.size() == 0){
      if (startupLog)
        System.out.println("[-] JSpider: No URL to spider!");
      return false;
    }
    // Unleash the workers :)
    if (startupLog)
      System.out.println(String.format("[*] JSpider: Starting %d threads...", threads));
    active = true;
    Thread t = null;
    for (int i = 0; i < threads; i++){
      t = new Thread(this);
      t.start();
      workers++;
    }
    if (startupLog)
      System.out.println(String.format("[+] JSpider: Threads dispatched, JSpider is now active!"));
    if (plugins.size() > 0){
      if (startupLog)
        System.out.println("[*] JSpider: Starting plugins...");
      startPlugins();
      if (startupLog)
        System.out.println("[+] JSpider: Plugins started!");
    }
    return active;
  }

  /**
  * Kill the spider and abort all threads. Will block until all threads exit.
  */
  public void kill(){

    try{
      active = false;
      while (workers > 0)
        Thread.sleep(50);
    }catch(InterruptedException ignored){}
  }

  /**
  * Main launcher.
  * @param args Command line args.
  * @throws MalformedURLException on URL parse error.
  * @throws IOException on IO error.
  */
  public static void main(String[] args) throws MalformedURLException, IOException{

    String helpPage = "JSpider v1.1 - A Java Web Crawler  (Author: https://github.com/UmarAbdul01)\n"+
                      "     Usage: jspider --url <url> [options]\n"+
                      "   Options:\n"+
                      "        -u|--url           <url>            :  Starting URL\n"+
                      "       -ua|--useragent     <str>            :  User agent\n"+
                      "     -tout|--timeout       <int>            :  Read timeout\n"+
                      "        -t|--threads       <int>            :  Number of threads to use\n"+
                      "        -m|--max           <int>            :  Max number of pages to parse\n"+
                      "        -o|--output        <str>            :  Output file\n"+
                      "        -c|--cookie        <cookie>         :  Cookie string to use\n"+
                      "        -e|--external      <bool>           :  Follow external URLs\n"+
                      "       -he|--hide-external <bool>           :  Hide external URLs\n"+
                      "        -i|--ignore        <str1,str2,...>  :  Keywords of URLs not to open\n"+
                      "        -p|--proxy         <host:port>      :  Proxy host and port\n"+
                      "       -pl|--plugin        <pl1,...>        :  Plugin(s) to activate\n"+
                      "       -po|--plugin-options <name=val;...>  :  Plugin options\n"+
                      "        -v|--verbose       <bool>           :  Verbose output\n"+
                      "        -h|--help                           :  Print this help page";
    ArgParser argParser = new ArgParser(args);
    argParser.setAlias("url", "u");
    argParser.setAlias("useragent", "ua");
    argParser.setDefault("useragent", new JBrowser().getUserAgent());
    argParser.setAlias("timeout", "tout");
    argParser.setDefault("timeout", "5000");
    argParser.setAlias("threads", "t");
    argParser.setDefault("threads", "5");
    argParser.setAlias("max", "m");
    argParser.setDefault("max", "100");
    argParser.setAlias("output", "o");
    argParser.setAlias("cookie", "-c");
    argParser.setAlias("external", "e");
    argParser.setDefault("external", "false");
    argParser.setAlias("hide-external", "he");
    argParser.setDefault("hide-external", "false");
    argParser.setAlias("ignore", "i");
    argParser.setAlias("proxy", "p");
    argParser.setAlias("plugin", "pl");
    argParser.setAlias("plugin-options", "po");
    argParser.setAlias("verbose", "v");
    argParser.setDefault("verbose", "true");
    if (argParser.hasArg("--help") || argParser.hasArg("-h")){
      System.out.println(helpPage);
      return;
    }
    String baseUrl = argParser.getString("url");
    if (baseUrl == null){
      System.out.println(helpPage);
      return;
    }

    JSpider spider = new JSpider(baseUrl, argParser.getInt("threads"), argParser.getInt("timeout"));
    DataOutputStream dos = null;
    String outfile = argParser.getString("output");
    if (outfile != null)
      dos = new DataOutputStream(new FileOutputStream(outfile));
    boolean verbose = argParser.getBoolean("verbose");
    if (argParser.hasKWarg("plugin"))
      verbose = false; // always run in non-verbose mode if a plugin is defined.
    spider.setFollowExternal(argParser.getBoolean("external"));
    spider.setHideExternal(argParser.getBoolean("hide-external"));
    if (argParser.getString("ignore") != null)
      spider.setIgnore(argParser.getString("ignore").split(","));
    if (argParser.getString("useragent") != null){
      HashMap<String, String> header = new HashMap<String, String>();
      header.put("User-Agent", argParser.getString("useragent"));
      spider.setHeaders(header); 
    }
    spider.setMax(argParser.getInt("max"));
    String cookie = argParser.getString("cookie");
    if (cookie != null)
      spider.setCookies(JBrowser.parseCookies(cookie));
    if (argParser.getString("proxy") != null)
      spider.setProxy(argParser.getString("proxy").split(":")[0], Integer.valueOf(argParser.getString("proxy").split(":")[1]));
    
    // Process plugins data. Plugin options must be loaded first before loading the plugin objects to allow for options verification by JSpider.loadPlugins()
    if (argParser.hasKWarg("plugin-options")){
      for (String opt : argParser.getString("plugin-options").split(";")){
        int pos = opt.indexOf("=");
        if (pos != -1)
          spider.setPluginOption(opt.substring(0, pos).trim(), opt.substring(pos+1).trim());
      }
    }
    if (argParser.hasKWarg("plugin")){
      if (!(spider.loadPlugins(argParser.getString("plugin").split(","))))
        return;
    }

    ArrayList<String> outputs = spider.getOutputQueue();
    ArrayList<String> status = spider.getStatusQueue();
    String url = null;
    long stime = System.currentTimeMillis(); // Start our timer.
    spider.start(true);
    int fetchCount = 0;
    while (spider.isActive()){
      while (status.size() > 0 && verbose)
        System.out.println(status.remove(0));
      while (outputs.size() > fetchCount){
        fetchCount++;
        url = outputs.get(fetchCount-1);
        if (verbose)
          System.out.println("[+]  ==>  " + url);
        if (dos != null)
          dos.write((url+"\n").getBytes(), 0, url.length()+1);
      }
      try{
        Thread.sleep(50);
      }catch(InterruptedException ignored){}
    }
    long etime = System.currentTimeMillis();
    // Extract remaining outputs, if any.
    while (outputs.size() > fetchCount){
      fetchCount++;
      url = outputs.get(fetchCount-1);
      if (verbose)
        System.out.println("[+]  ==>  " + url);
      if (dos != null)
        dos.write((url+"\n").getBytes(), 0, url.length()+1);
    }
    if (dos != null)
      dos.close();
    System.out.println(String.format("[+] JSpider: Crawling completed, %d URLs found in %d pages!", spider.getURLCount(), spider.getCrawled().size()));
    System.out.println(String.format("[*] JSpider: Time taken: %.3f seconds.", (float)(etime - stime) / 1000.0));
  }
}
