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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.exception.WabacusConfigLoadingException;

public class WabacusClassLoader extends ClassLoader
{
    private static Log log=LogFactory.getLog(WabacusClassLoader.class);
    
    private ClassLoader parentLoader;

    private List<String> classRepository=new ArrayList<String>();

    public WabacusClassLoader(ClassLoader parent)
    {
        super(parent);
        parentLoader=parent;
        if(parent==null) throw new IllegalArgumentException("必须提供父装载器!");
    }

    public synchronized Class loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name,false);
    }

    protected synchronized Class loadClass(String name,boolean resolve)
            throws ClassNotFoundException
    {
        Class c=findLoadedClass(name);
        ClassNotFoundException ex=null;
        if(c==null&&parentLoader!=null)
        {
            try
            {
                c=parentLoader.loadClass(name);
            }catch(ClassNotFoundException e)
            {
                ex=e;
            }
        }
        if(c==null)
        {
            try
            {
                c=this.findSystemClass(name);
            }catch(ClassNotFoundException e)
            {
                ex=e;
            }
        }
        if(c==null)
        {
            throw ex;
        }

        if(resolve)
        {
            resolveClass(c);
        }
        return c;

    }

    public synchronized Class loadClass(String className,byte[] classBytes)
    {
        if(classBytes==null||className==null||className.trim().equals(""))
        {
            return null;
        }
        return this.defineClass(className,classBytes,0,classBytes.length);

    }

    public void addClassPath(String strClassPath)
    {
        if(classRepository==null) classRepository=new ArrayList<String>();
        if((strClassPath!=null)&&!(strClassPath.equals("")))
        {
            StringTokenizer tokenizer=new StringTokenizer(strClassPath,File.pathSeparator);
            while(tokenizer.hasMoreTokens())
            {
                classRepository.add(tokenizer.nextToken());
            }
        }
    }

    public synchronized Class loadClassFromClassPath(String className)
    {

        Iterator<String> dirs=classRepository.iterator();
        byte[] classBytes=null;

        while(dirs.hasNext())
        {
            String dir=(String)dirs.next();

            String classFileName=className.replace('.',File.separatorChar);
            classFileName+=".class";
            try
            {
                File file=new File(dir+File.separatorChar+classFileName);
                if(file.exists())
                {
                    InputStream is=new FileInputStream(file);
                    classBytes=new byte[is.available()];
                    is.read(classBytes);
                    break;
                }
            }catch(IOException ex)
            {

                ex.printStackTrace();
                return null;
            }
        }
        return this.defineClass(className,classBytes,0,classBytes.length);
    }
    
    public Class loadClassByCurrentLoader(String classname)
    {
        if(classname==null||classname.trim().equals("")) return null;
       /* ClassLoader contextClassLoader=Thread.currentThread().getContextClassLoader();
        if(contextClassLoader==null)  contextClassLoader=WabacusClassLoader.class.getClassLoader();
        try
        {
            return contextClassLoader.loadClass(classname);
        }catch(ClassNotFoundException e)
        {
            throw new WabacusConfigLoadingException("装载类"+classname+"失败，没有找到此类!",e);
        }*/
        try
        {
            return this.loadClass(classname);
        }catch(ClassNotFoundException e)
        {
            throw new WabacusConfigLoadingException("装载类"+classname+"失败，没有找到此类!",e);
        }
        
    }
}
