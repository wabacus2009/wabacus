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
package com.wabacus.system.dataset.select.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public class VerticalReportDataSet extends AbsReportDataSetType
{
    public VerticalReportDataSet(AbsReportType reportTypeObj)
    {
        super(reportTypeObj);
    }

    public List<AbsReportDataPojo> loadReportAllRowDatas(boolean isLoadAllDataMandatory)
    {
        boolean isLazyLoadReportData=rbean.isLazyLoadReportData(rrequest);
        if((!cdb.isLoadAllReportData()&&cdb.getRefreshNavigateInfoType()<=0&&!isLoadAllDataMandatory)||(cdb.isLoadAllReportData()&&isLazyLoadReportData))
        {//当前加载分页报表数据，且需要加载记录数，如果本次是加载所有数据，但是延迟加载，则也要加载记录数，因为后面在分批加载时要判断当前要从哪个<dataset/>中另载数据
            loadReportDataRecordcount();
            if(cdb.getRecordcount()==0) return null;
        }
        List<Map<String,Object>> lstOneDatasetValueDataTmp;//一个<value/>加载的数据集合
        List<AbsReportDataPojo> lstDataResult=new ArrayList<AbsReportDataPojo>();
        List<AbsReportDataPojo> lstOneDatasetData=new ArrayList<AbsReportDataPojo>();//一个<dataset/>加载的所有数据
        int pagesize=-1, startNum=-1;
        if(!cdb.isLoadAllReportData()&&!isLoadAllDataMandatory)
        {
            pagesize=cdb.getPagesize();
            startNum=(cdb.getFinalPageno()-1)*pagesize;
        }else if(cdb.isLoadAllReportData()&&isLazyLoadReportData)
        {
            pagesize=Integer.MAX_VALUE;
            startNum=0;
        }
        int maxrecordcount=cdb.getMaxrecordcount();
        if(maxrecordcount<=0) maxrecordcount=-1;
        int prevDatasetDisplayedTotalRowcount=0;//到上一个数据集为止显示的记录总数，这样可以决定要在循环的本数据集中要显示的起止记录号
        outer: for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
        {//逐个<dataset/>查询记录
            int maxDisplayedTotalRowCountThisDataset=0;//记录本<dataset/>中在翻到本页时各子<value/>数据集显示的最大记录数
            for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
            {
                if((lstOneDatasetData.size()==0)&&dsvbeanTmp.isDependentDataSet()) continue;
                int startRownum=-1,endRownum=-1;
                if(startNum>=0&&!dsvbeanTmp.isDependentDataSet())
                {
                    int[] tmp=calStartEndRownumForPartDataset(dsvbeanTmp,prevDatasetDisplayedTotalRowcount,maxDisplayedTotalRowCountThisDataset,
                            startNum,pagesize);
                    maxDisplayedTotalRowCountThisDataset=tmp[0];
                    startRownum=tmp[1];
                    endRownum=tmp[2];
                    cdb.setStartRownumOfDsvbean(dsvbeanTmp,startRownum);
                    cdb.setEndRownumOfDsvbean(dsvbeanTmp,endRownum);
                    if(startRownum<0||endRownum<=startRownum) continue;
                }
                if(!isLazyLoadReportData)
                {//不是延迟加载，则加载报表数据
                    lstOneDatasetValueDataTmp=dsvbeanTmp.getProvider().getDataSet(rrequest,lstOneDatasetData,startRownum,endRownum);
                    if(lstOneDatasetValueDataTmp==null||lstOneDatasetValueDataTmp.size()==0) continue;
                    if(dsvbeanTmp.isDependentDataSet())
                    {
                        parseDependentReportData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetData);
                    }else if(cdb.isLoadAllReportData()||isLoadAllDataMandatory)
                    {
                        copyListDataToLstResultsData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetData,maxrecordcount);
                    }else
                    {
                        copyListDataToLstResultsData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetData,cdb.getPagesize());
                    }
                }
            }
            if(!isLoadAllDataMandatory)
            {
                cdb.setStartRownumOfDsbean(dsbeanTmp,prevDatasetDisplayedTotalRowcount);
                if(startNum>=0)
                {//加载分页显示报表的数据，或者延迟加载所有数据
                    prevDatasetDisplayedTotalRowcount+=maxDisplayedTotalRowCountThisDataset;//累加上本<dataset/>显示的总记录数
                }else
                {
                    prevDatasetDisplayedTotalRowcount+=lstOneDatasetData.size();
                }
                cdb.setEndRownumOfDsbean(dsbeanTmp,prevDatasetDisplayedTotalRowcount);
            }
            for(AbsReportDataPojo dataObjTmp:lstOneDatasetData)
            {
                dataObjTmp.setLstAllDataObjs(lstDataResult);
                lstDataResult.add(dataObjTmp);
                if(pagesize>0)
                {
                    if(lstDataResult.size()==pagesize||(maxrecordcount>0&&startNum+lstDataResult.size()==maxrecordcount)) break outer;
                }else if(maxrecordcount>0&&lstDataResult.size()==maxrecordcount)
                {//已经显示了指定的最大记录数
                    break outer;
                }
            }
            lstOneDatasetData.clear();
        }
        return lstDataResult;
    }

    private void loadReportDataRecordcount()
    {
        if(cdb.getRefreshNavigateInfoType()<0)
        {
            for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
            {
                int recordcount=0;
                for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
                {
                    if(dsvbeanTmp.isDependentDataSet()) continue;
                    int recordcntTmp=dsvbeanTmp.getProvider().getRecordcount(rrequest);
                    cdb.addRecordcount(dsvbeanTmp.getGuid(),recordcntTmp);
                    if(recordcntTmp>recordcount) recordcount=recordcntTmp;
                }
                cdb.setRecordcount(cdb.getRecordcount()+recordcount);
            }
        }
        if(cdb.getPagesize()>0)
        {
            cdb.setPagecount(ReportAssistant.getInstance().calPageCount(cdb.getPagesize(),cdb.getRecordcount()));
        }
    }

    private int[] calStartEndRownumForPartDataset(ReportDataSetValueBean dsvbean,int prevDatasetDisplayedTotalRowcount,
            int maxDisplayedTotalRowCountThisDataset,int startNum,int pagesize)
    {
        int startRownum=0,endRownum=0;//本<value/>数据集要显示的起止记录号
        int myrecordcnt=cdb.getRecordcountOfDataset(dsvbean.getGuid());
        if(myrecordcnt>0)
        {
            if(prevDatasetDisplayedTotalRowcount+myrecordcnt<startNum)
            {
                if(myrecordcnt>maxDisplayedTotalRowCountThisDataset) maxDisplayedTotalRowCountThisDataset=myrecordcnt;
            }else
            {
                if(startNum>prevDatasetDisplayedTotalRowcount)
                {//如果本次显示的起始记录号大于前面所有数据集显示的总记录数，则说明本数据集在前面页也有显示（否则前面页的记录数不够）
                    startRownum=startNum-prevDatasetDisplayedTotalRowcount;
                }
                int pagesizelocal=pagesize;
                if(prevDatasetDisplayedTotalRowcount>startNum)
                {
                    pagesizelocal=startNum+pagesize-prevDatasetDisplayedTotalRowcount;
                }
                if(startRownum+pagesizelocal>myrecordcnt) pagesizelocal=myrecordcnt-startRownum;//这个数据集在本页没有这么多记录显示，则取出实际能显示的记录数做为显示记录数
                endRownum=startRownum+pagesizelocal;
                if(endRownum>maxDisplayedTotalRowCountThisDataset) maxDisplayedTotalRowCountThisDataset=endRownum;
            }
        }
        return new int[] { maxDisplayedTotalRowCountThisDataset, startRownum, endRownum};
    }

    public List<AbsReportDataPojo> loadLazyReportDatas(int startRownum,int endRownum)
    {
        List<AbsReportDataPojo> lstDataResult=new ArrayList<AbsReportDataPojo>();
        List<AbsReportDataPojo> lstOneDatasetData=new ArrayList<AbsReportDataPojo>();//一个<dataset/>加载的所有数据
        List<Map<String,Object>> lstOneDatasetValueDataTmp;//一个<value/>加载的数据集合
        int maxrecordcount=cdb.getMaxrecordcount();
        if(maxrecordcount>0&&maxrecordcount<endRownum) endRownum=maxrecordcount;
        if(endRownum<=startRownum) return lstDataResult;
        int dataCount=endRownum-startRownum;
        int dsStartRownum, dsEndRownum;
        outer: for(ReportDataSetBean dsbeanTmp:rbean.getSbean().getLstDatasetBeans())
        {//逐个<dataset/>查询记录
            if(cdb.getEndRownumOfDsbean(dsbeanTmp)<=startRownum) continue;
            if(cdb.getStartRownumOfDsbean(dsbeanTmp)>=endRownum) break;
            dsStartRownum=startRownum-cdb.getStartRownumOfDsbean(dsbeanTmp);
            dsEndRownum=endRownum-cdb.getStartRownumOfDsbean(dsbeanTmp);
            for(ReportDataSetValueBean dsvbeanTmp:dsbeanTmp.getLstValueBeans())
            {
                if((lstOneDatasetData.size()==0)&&dsvbeanTmp.isDependentDataSet()) continue;//如果当前数据集不是独立数据集，且被依赖的父数据集没有数据，则此子数据集也不需显示
                if(dsvbeanTmp.isDependentDataSet())
                {
                    dsStartRownum=-1;
                    dsEndRownum=-1;
                }
                lstOneDatasetValueDataTmp=dsvbeanTmp.getProvider().getDataSet(rrequest,lstOneDatasetData,dsStartRownum,dsEndRownum);
                if(lstOneDatasetValueDataTmp==null||lstOneDatasetValueDataTmp.size()==0) continue;
                if(dsvbeanTmp.isDependentDataSet())
                {
                    parseDependentReportData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetData);
                }else
                {
                    copyListDataToLstResultsData(dsvbeanTmp,lstOneDatasetValueDataTmp,lstOneDatasetData,dataCount);
                }
            }
            for(AbsReportDataPojo dataObjTmp:lstOneDatasetData)
            {
                dataObjTmp.setLstAllDataObjs(lstDataResult);
                lstDataResult.add(dataObjTmp);
                if(lstDataResult.size()==dataCount) break outer;
            }
            lstOneDatasetData.clear();
        }
        return lstDataResult;
    }
}
