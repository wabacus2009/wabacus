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
package com.wabacus.config.dataexport;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.system.task.DeleteTempFileTask;
import com.wabacus.util.Tools;

public class DeleteDataExportTempFileTask extends DeleteTempFileTask
{
    private final static Log log=LogFactory.getLog(DeleteDataExportTempFileTask.class);
    
    public synchronized void execute()
    {
        lastExecuteMilSeconds=System.currentTimeMillis();
        if(Tools.isEmpty(this.filepath)) return;
        File f=new File(this.filepath);
        if(!f.exists()||!f.isDirectory()) return;
        File[] filesArr=f.listFiles();
        for(int i=0;i<filesArr.length;i++)
        {
            try
            {
            if(filesArr[i].isFile()&&isShouldDeleteFile(filesArr[i].lastModified()))
            {
                filesArr[i].delete();
            }else if(filesArr[i].isDirectory())
            {
                File[] childFilesArr=filesArr[i].listFiles();
                if(childFilesArr!=null&&childFilesArr.length>0)
                {
                    for(int j=0,len2=childFilesArr.length;j<len2;j++)
                    {
                        try
                        {
                            if(isShouldDeleteFile(childFilesArr[j].lastModified())) childFilesArr[j].delete();
                        }catch(Exception e)
                        {
                            log.error("删除文件或目录"+childFilesArr[j].getAbsolutePath()+"失败",e);
                        }
                    }
                    childFilesArr=filesArr[i].listFiles();
                }
                if(childFilesArr==null||childFilesArr.length==0) filesArr[i].delete();
            }
            }catch(Exception e)
            {
                log.error("删除文件或目录"+filesArr[i].getAbsolutePath()+"失败",e);
            }
        }
    }
        
    public void destory()
    {}

}

