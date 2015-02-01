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
package com.wabacus.system.task;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.dataexport.DeleteDataExportTempFileTask;
import com.wabacus.util.Tools;

public class DeleteTempFileTask implements ITask
{
    private final static Log log=LogFactory.getLog(DeleteTempFileTask.class);
    
    protected String id;
    
    protected String filepath;
    
    protected long lastExecuteMilSeconds;

    private long intervalMilSeconds;
    
    private long persistencePeriods;
    
    public DeleteTempFileTask()
    {
        lastExecuteMilSeconds=0L;
    }
    
    public void setId(String id)
    {
        this.id=id;
    }

    public void setFilepath(String filepath)
    {
        this.filepath=filepath;
    }

    public void setLastExecuteMilSeconds(long lastExecuteMilSeconds)
    {
        this.lastExecuteMilSeconds=lastExecuteMilSeconds;
    }

    public void setIntervalMilSeconds(long intervalMilSeconds)
    {
        this.intervalMilSeconds=intervalMilSeconds;
    }

    public void setPersistencePeriods(long persistencePeriods)
    {
        this.persistencePeriods=persistencePeriods;
    }

    public void parseInterval(String deleteFilesInterval)
    {
        intervalMilSeconds=Long.MIN_VALUE;
        persistencePeriods=Long.MIN_VALUE;
        int idx=deleteFilesInterval.indexOf("|");
        if(!deleteFilesInterval.equals("")&&idx>0)
        {
            intervalMilSeconds=Long.parseLong(deleteFilesInterval.substring(0,idx).trim())*1000;
            persistencePeriods=Long.parseLong(deleteFilesInterval.substring(idx+1).trim())*1000;
        }
        if(intervalMilSeconds<=0) intervalMilSeconds=300*1000L;//默认5分钟
        if(persistencePeriods<=0) persistencePeriods=600*1000L;
    }
    
    public String getTaskId()
    {
        return this.id;
    }
    
    public boolean shouldExecute()
    {
        if(Tools.isEmpty(this.filepath)) return false;
        return System.currentTimeMillis()-lastExecuteMilSeconds>=intervalMilSeconds;
    }
    
    public synchronized void execute()
    {
        lastExecuteMilSeconds=System.currentTimeMillis();
        if(Tools.isEmpty(this.filepath)) return;
        File f=new File(this.filepath);
        if(!f.exists()||!f.isDirectory()) return;
        File[] filesArr=f.listFiles();
        if(filesArr==null) return;
        for(int i=0;i<filesArr.length;i++)
        {
            try
            {
                if(isShouldDeleteFile(filesArr[i].lastModified())) filesArr[i].delete();
            }catch(Exception e)
            {
                log.error("删除文件或目录"+filesArr[i].getAbsolutePath()+"失败",e);
            }
        }
    }

    protected boolean isShouldDeleteFile(long lastupdatetime)
    {
        return lastExecuteMilSeconds-lastupdatetime>=persistencePeriods;
    }
    
    public void destory()
    {
        if(Tools.isEmpty(this.filepath)) return;
        File f=new File(this.filepath);
        if(f.exists()&&f.isDirectory())
        {
            File[] filesArr=f.listFiles();
            if(filesArr==null) return;
            for(int i=0,len=filesArr.length;i<len;i++)
            {
                try
                {
                    filesArr[i].delete();
                }catch(Exception e)
                {
                    log.error("删除文件或目录"+filesArr[i].getAbsolutePath()+"失败",e);
                }
            }
        }
    }
    
}

