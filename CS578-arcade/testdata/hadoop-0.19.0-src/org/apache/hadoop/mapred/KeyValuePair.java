/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Provides methods for writing to and reading from job history. 
 * Job History works in an append mode, JobHistory and its inner classes provide methods 
 * to log job events. 
 * 
 * JobHistory is split into multiple files, format of each file is plain text where each line 
 * is of the format [type (key=value)*], where type identifies the type of the record. 
 * Type maps to UID of one of the inner classes of this class. 
 * 
 * Job history is maintained in a master index which contains star/stop times of all jobs with
 * a few other job level properties. Apart from this each job's history is maintained in a seperate history 
 * file. name of job history files follows the format jobtrackerId_jobid
 *  
 * For parsing the job history it supports a listener based interface where each line is parsed
 * and passed to listener. The listener can create an object model of history or look for specific 
 * events and discard rest of the history.  
 * 
 * CHANGE LOG :
 * Version 0 : The history has the following format : 
 *             TAG KEY1="VALUE1" KEY2="VALUE2" and so on. 
               TAG can be Job, Task, MapAttempt or ReduceAttempt. 
               Note that a '"' is the line delimiter.
 * Version 1 : Changes the line delimiter to '.'
               Values are now escaped for unambiguous parsing. 
               Added the Meta tag to store version info.
 */
package org.apache.hadoop.mapred;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.StringUtils;

static class KeyValuePair{
    private Map<Keys, String> values = new HashMap<Keys, String>(); 

    /**
     * Get 'String' value for given key. Most of the places use Strings as 
     * values so the default get' method returns 'String'.  This method never returns 
     * null to ease on GUIs. if no value is found it returns empty string ""
     * @param k 
     * @return if null it returns empty string - "" 
     */
    public String get(Keys k){
      String s = values.get(k); 
      return s == null ? "" : s; 
    }
    /**
     * Convert value from history to int and return. 
     * if no value is found it returns 0.
     * @param k key 
     */
    public int getInt(Keys k){
      String s = values.get(k); 
      if (null != s){
        return Integer.parseInt(s);
      }
      return 0; 
    }
    /**
     * Convert value from history to int and return. 
     * if no value is found it returns 0.
     * @param k
     */
    public long getLong(Keys k){
      String s = values.get(k); 
      if (null != s){
        return Long.parseLong(s);
      }
      return 0; 
    }
    /**
     * Set value for the key. 
     * @param k
     * @param s
     */
    public void set(Keys k, String s){
      values.put(k, s); 
    }
    /**
     * Adds all values in the Map argument to its own values. 
     * @param m
     */
    public void set(Map<Keys, String> m){
      values.putAll(m);
    }
    /**
     * Reads values back from the history, input is same Map as passed to Listener by parseHistory().  
     * @param values
     */
    public synchronized void handle(Map<Keys, String> values){
      set(values); 
    }
    /**
     * Returns Map containing all key-values. 
     */
    public Map<Keys, String> getValues(){
      return values; 
    }
  }