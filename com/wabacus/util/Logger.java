/* 
 * Copyright (C) 2010---2014 星星(wuweixing)<349446658@qq.com>
 * 
 * This file is part of Wabacus 
 * 
 * Wabacus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wabacus.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Logger
{
    private static Log log=LogFactory.getLog(Logger.class);

    private static Map<String,Log> mLogClasses=new HashMap<String,Log>();

    public static void debug(String message)
    {
        log.debug(message);
    }

    public static void debug(String message,Throwable t)
    {
        log.debug(message,t);
    }

    public static void info(String message)
    {
        log.info(message);
    }

    public static void info(String message,Throwable t)
    {
        log.info(message,t);
    }

    public static void warn(String message)
    {
        log.warn(message);
    }

    public static void warn(String message,Throwable t)
    {
        log.warn(message,t);
    }

    public static void error(String message)
    {
        log.error(message);
    }

    public static void error(String message,Throwable t)
    {
        log.error(message,t);
    }

    public static void fatal(String message)
    {
        log.fatal(message);
    }

    public static void fatal(String message,Throwable t)
    {
        log.fatal(message,t);
    }

    public static void trace(String message)
    {
        log.trace(message);
    }

    public static void trace(String message,Throwable t)
    {
        log.trace(message,t);
    }
    
    public static void debug(String message,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.debug(message);
        }else
        {
            getLogObj(sourceClass).debug(message);
        }
    }

    public static void debug(String message,Throwable t,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.debug(message,t);
        }else
        {
            getLogObj(sourceClass).debug(message,t);
        }
    }

    public static void info(String message,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.info(message);
        }else
        {
            getLogObj(sourceClass).info(message);
        }
    }

    public static void info(String message,Throwable t,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.info(message,t);
        }else
        {
            getLogObj(sourceClass).info(message,t);
        }
    }
   
    public static void warn(String message,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.warn(message);
        }else
        {
            getLogObj(sourceClass).warn(message);
        }
    }

    public static void warn(String message,Throwable t,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.warn(message,t);
        }else
        {
            getLogObj(sourceClass).warn(message,t);
        }
    }
    
    public static void error(String message,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.error(message);
        }else
        {
            getLogObj(sourceClass).error(message);
        }
    }

    public static void error(String message,Throwable t,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.error(message,t);
        }else
        {
            getLogObj(sourceClass).error(message,t);
        }
    }
    
    public static void fatal(String message,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.fatal(message);
        }else
        {
            getLogObj(sourceClass).fatal(message);
        }
    }

    public static void fatal(String message,Throwable t,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.fatal(message,t);
        }else
        {
            getLogObj(sourceClass).fatal(message,t);
        }
    }
    
    public static void trace(String message,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.trace(message);
        }else
        {
            getLogObj(sourceClass).trace(message);
        }
    }

    public static void trace(String message,Throwable t,Class sourceClass)
    {
        if(sourceClass==null)
        {
            log.trace(message,t);
        }else
        {
            getLogObj(sourceClass).trace(message,t);
        }
    }
    
    private static Log getLogObj(Class sourceClass)
    {
        Log logObj=mLogClasses.get(sourceClass.getName());
        if(logObj==null)
        {
            logObj=LogFactory.getLog(sourceClass);
            mLogClasses.put(sourceClass.getName(),logObj);
        }
        return logObj;
    }
}
