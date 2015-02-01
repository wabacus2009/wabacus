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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileLockTools
{
    private static Log log=LogFactory.getLog(FileLockTools.class);

    public static Object lock(String lockfile,int waitsec,int maxtimes)
    {
        Object lockresource=lock(lockfile);
        Random random=new Random();
        int times=0;
        while(lockresource==null)
        {
            if(times++>=maxtimes)
            {
                log.debug("获取文件锁"+lockfile+"时，重试"+maxtimes+"次仍失败");
                break;
            }
            try
            {
                Thread.sleep(random.nextInt(waitsec)*1000);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
            lockresource=lock(lockfile);
        }
        return lockresource;
    }

    public static Object lock(String lockfile)
    {
        RandomAccessFile raf=null;
        FileChannel fc=null;
        boolean islocked=true;
        try
        {
            raf=new RandomAccessFile(new File(lockfile),"rw");
            fc=raf.getChannel();
            FileLock fl=fc.tryLock();
            if(fl!=null&&fl.isValid())
            {
                Map mapResources=new HashMap();
                mapResources.put("RAF",raf);
                mapResources.put("FC",fc);
                mapResources.put("FL",fl);
                return mapResources;
            }else
            {
                islocked=false;
                return null;
            }
        }catch(Exception e)
        {
            log.error("获取文件"+lockfile+"锁失败",e);
            return null;
        }finally
        {
            if(!islocked)
            {
                release(fc);
                release(raf);

            }
        }

    }

    public static boolean unlock(String lockfile,Object resource)
    {
        try
        {
            if(resource!=null&&resource instanceof Map)
            {
                Map mResources=(Map)resource;
                RandomAccessFile raf=(RandomAccessFile)mResources.get("RAF");
                FileChannel fc=(FileChannel)mResources.get("FC");
                FileLock fl=(FileLock)mResources.get("FL");
                release(fl);
                release(fc);
                release(raf);
                resource=null;
            }
        }catch(Exception e)
        {
            log.error("释放文件"+lockfile+"锁失败");
            return false;
        }
        return true;
    }

    private static boolean release(RandomAccessFile raf)
    {
        try
        {
            if(raf!=null)
            {
                raf.close();
                raf=null;
            }
            return true;
        }catch(Exception e)
        {
            System.out.println("关闭RandomAccessFile对象失败");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteLockFile(String lockfile)
    {
        try
        {
            File f=new File(lockfile);
            if(f!=null&&f.isFile())
            {
                f.delete();
                return true;
            }
            return false;
        }catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean createLockFile(String lockfile)
    {
        try
        {
            File f=new File(lockfile);
            if(f!=null&&f.isFile())
            {
                f.delete();
            }
            return f.createNewFile();
        }catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean release(FileChannel fc)
    {
        try
        {
            if(fc!=null)
            {
                fc.close();
                fc=null;
            }
            return true;
        }catch(Exception e)
        {
            System.out.println("关闭FileChannel对象失败");
            e.printStackTrace();
            return false;
        }
    }

    private static boolean release(FileLock fl)
    {
        try
        {
            if(fl!=null)
            {
                fl.release();
                fl=null;
            }
            return true;
        }catch(Exception e)
        {
            System.out.println("释放FileLock对象时失败");
            e.printStackTrace();
            return false;
        }
    }

}
