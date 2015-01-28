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
package com.wabacus.system.dataimport.thread;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.resource.dataimport.configbean.AbsDataImportConfigBean;
import com.wabacus.exception.WabacusDataImportException;
import com.wabacus.system.assistant.DataImportAssistant;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.dataimport.DataImportItem;
import com.wabacus.system.task.ITask;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.FileLockTools;

public class TimingDataImportTask implements ITask
{
    private static Log log=LogFactory.getLog(TimingDataImportTask.class);

    private long lastExecuteMilSeconds;

    private long intervalMilSeconds=Long.MIN_VALUE;

    public TimingDataImportTask()
    {
        lastExecuteMilSeconds=0L;
    }

    public String getTaskId()
    {
        return TimingDataImportTask.class.getName();
    }

    public boolean shouldExecute()
    {
        if(this.intervalMilSeconds==Long.MIN_VALUE)
        {
            intervalMilSeconds=Config.getInstance().getSystemConfigValue("dataimport-autodetect-interval",30)*1000L;
            if(intervalMilSeconds<=0) intervalMilSeconds=30*1000L;
        }
        return System.currentTimeMillis()-lastExecuteMilSeconds>=intervalMilSeconds;
    }

    public synchronized void execute()
    {
        lastExecuteMilSeconds=System.currentTimeMillis();
        Map<String,List<AbsDataImportConfigBean>> mAutoDetectedDataImportBeans=Config.getInstance().getMAutoDetectedDataImportBeans();
        if(mAutoDetectedDataImportBeans!=null&&mAutoDetectedDataImportBeans.size()>0)
        {
            String lockfile;
            for(Entry<String,List<AbsDataImportConfigBean>> entryTmp:mAutoDetectedDataImportBeans.entrySet())
            {
                String filepath=entryTmp.getKey();
                File f=new File(filepath);
                if(!f.exists()||!f.isDirectory())
                {
                    log.warn("数据导入项的监控路径"+filepath+"不存在或不是目录，无法导入其上的数据文件");
                    continue;
                }
                lockfile=FilePathAssistant.getInstance().standardFilePath(filepath+"\\"+Consts_Private.DATAIMPORT_LOCKFILENAME);
                Object lockresource=FileLockTools.lock(lockfile,10,5);
                if(lockresource==null)
                {
                    log.debug("定时监控线程没有取到文件锁"+lockfile+"，本周期没有对此目录进行监控");
                    continue;
                }
                try
                {
                    File[] filesArr=f.listFiles();
                    if(filesArr==null||filesArr.length==0) continue;
                    for(int i=0;i<filesArr.length;i++)
                    {
                        if(filesArr[i].isDirectory()) continue;
                        if(Consts_Private.DATAIMPORT_LOCKFILENAME.equalsIgnoreCase(filesArr[i].getName())) continue;
                        doImportData(filesArr[i],entryTmp.getValue());
                    }
                }catch(Exception e)
                {
                    log.error("监控并导入路径"+filepath+"下的数据文件失败",e);
                }finally
                {
                    FileLockTools.unlock(lockfile,lockresource);
                }
            }
        }
    }

    private void doImportData(File file,List<AbsDataImportConfigBean> lstDataImportBeans)
    {
        if(lstDataImportBeans==null||lstDataImportBeans.size()==0) return;
        List<DataImportItem> lstResults=new ArrayList<DataImportItem>();
        try
        {
            String[] strArrTmp=DataImportAssistant.getInstance().getRealFileNameAndImportType(file.getName());
            DataImportItem diitem;
            for(AbsDataImportConfigBean dibeanTmp:lstDataImportBeans)
            {
                if(dibeanTmp.isMatch(strArrTmp[0]))
                {
                    diitem=new DataImportItem(dibeanTmp,file);
                    diitem.setDynimportype(strArrTmp[1]);
                    lstResults.add(diitem);
                }
            }
            if(lstResults.size()==0)
            {
                log.warn("数据文件"+file.getAbsolutePath()+"没有对应的数据导入项，不会对其进行导入");
            }else
            {
                for(DataImportItem diiTmp:lstResults)
                {
                    diiTmp.doImportData();
                }
            }
        }catch(Exception e)
        {
            throw new WabacusDataImportException("导入数据文件"+file.getPath()+"失败",e);
        }finally
        {
            DataImportItem.backupOrDeleteDataFile(file);
        }
    }

    public void destory()
    {}
}
