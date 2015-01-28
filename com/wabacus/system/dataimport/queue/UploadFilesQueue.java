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
package com.wabacus.system.dataimport.queue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

import com.wabacus.system.dataimport.DataImportItem;

public class UploadFilesQueue
{
    private static UploadFilesQueue instance=new UploadFilesQueue();

    private List<Map<List<DataImportItem>,Map<File,FileItem>>> queueInstance;

    private UploadFilesQueue()
    {
        queueInstance=new ArrayList<Map<List<DataImportItem>,Map<File,FileItem>>>();
    }

    public static UploadFilesQueue getInstance()
    {
        return instance;
    }

    public void addUploadFile(Map<List<DataImportItem>,Map<File,FileItem>> uploadFile)
    {
        synchronized(queueInstance)
        {
            if(uploadFile!=null&&uploadFile.size()>0)
            {
                queueInstance.add(uploadFile);
                queueInstance.notifyAll();
            }
        }
    }

    public Map<List<DataImportItem>,Map<File,FileItem>> getUploadFile()
    {
        synchronized(queueInstance)
        {
            while(queueInstance.size()==0)
            {
                try
                {
                    queueInstance.wait();
                }catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            return queueInstance.remove(0);
        }
    }

    public List<Map<List<DataImportItem>,Map<File,FileItem>>> getLstAllUploadFiles()
    {
        synchronized(queueInstance)
        {
            while(queueInstance.size()==0)
            {
                try
                {
                    queueInstance.wait();
                }catch(InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            List<Map<List<DataImportItem>,Map<File,FileItem>>> lstResults=new ArrayList<Map<List<DataImportItem>,Map<File,FileItem>>>();
            lstResults.addAll(queueInstance);
            queueInstance.clear();
            return lstResults;
        }
    }

    public void notifyAllThread()
    {
        synchronized(queueInstance)
        {
            queueInstance.notifyAll();
        }
    }
}
