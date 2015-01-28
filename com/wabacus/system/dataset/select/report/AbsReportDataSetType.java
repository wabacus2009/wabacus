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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.ReportDataSetBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;

public abstract class AbsReportDataSetType
{
    private static Log log=LogFactory.getLog(AbsReportDataSetType.class);

    protected AbsReportType reportTypeObj;

    protected ReportRequest rrequest;

    protected ReportBean rbean;

    protected CacheDataBean cdb;

    public AbsReportDataSetType(AbsReportType reportTypeObj)
    {
        this.reportTypeObj=reportTypeObj;
        this.rrequest=reportTypeObj.getReportRequest();
        this.rbean=reportTypeObj.getReportBean();
        this.cdb=rrequest.getCdb(rbean.getId());
    }

    public abstract List<AbsReportDataPojo> loadReportAllRowDatas(boolean isLoadAllDataMandatory);

    protected List<ColBean> getLstRealColBeans()
    {
        List<ColBean> lstColBeans=this.cdb.getLstDynOrderColBeans();
        if(lstColBeans==null||lstColBeans.size()==0) lstColBeans=rbean.getDbean().getLstCols();
        return lstColBeans;
    }
    
    protected void parseDependentReportData(ReportDataSetValueBean datasetbean,List<Map<String,Object>> lstOneDatasetValueData,
            List<AbsReportDataPojo> lstOneDatasetData)
    {
        try
        {
            Map<String,Map<ColBean,Object>> mChildDatasetDataObjs=getChildDatasetDataObjs(datasetbean,lstOneDatasetValueData);
            if(mChildDatasetDataObjs.size()==0) return;
            List<String> lstDependParentColumns=datasetbean.getLstDependParentColumns();
            if(lstDependParentColumns==null||lstDependParentColumns.size()==0) return;
            String keyTmp;
            StringBuffer allKeysBuf=new StringBuffer();
            for(AbsReportDataPojo dataObjTmp:lstOneDatasetData)
            {
                keyTmp=getRealDependsColumnsValueAsKey(datasetbean,lstDependParentColumns,dataObjTmp);
                Map<ColBean,Object> mColData=mChildDatasetDataObjs.get(keyTmp);
                if(mColData==null) continue;
                for(Entry<ColBean,Object> entryTmp:mColData.entrySet())
                {
                    dataObjTmp.setColValue(entryTmp.getKey(),entryTmp.getValue());
                }
                allKeysBuf.append(keyTmp).append(";");
            }
            log.debug("父数据集关联字段数据："+allKeysBuf.toString());
        }catch(SQLException sqle)
        {
            throw new WabacusRuntimeException("获取报表"+rbean.getPath()+"数据时失败",sqle);
        }
    }

    private Map<String,Map<ColBean,Object>> getChildDatasetDataObjs(ReportDataSetValueBean dsvbean,List<Map<String,Object>> lstOneDatasetValueData)
            throws SQLException
    {
        Map<String,Map<ColBean,Object>> mChildDatasetDataObjs=new HashMap<String,Map<ColBean,Object>>();
        if(lstOneDatasetValueData==null||lstOneDatasetValueData.size()==0) return mChildDatasetDataObjs;
        StringBuffer allKeysBuf=new StringBuffer();
        for(Map<String,Object> mRowDataObjTmp:lstOneDatasetValueData)
        {
            String relateColValues=getRealDependsColumnsValueAsKey(dsvbean,dsvbean.getLstDependMyColumns(),mRowDataObjTmp);//获取所有参与数据集依赖的字段的值组成的KEY字符串
            allKeysBuf.append(relateColValues).append(";");
            Map<ColBean,Object> mColData=mChildDatasetDataObjs.get(relateColValues);
            if(mColData==null)
            {
                mColData=new HashMap<ColBean,Object>();
                mChildDatasetDataObjs.put(relateColValues,mColData);
            }
            for(ColBean cbTmp:getLstRealColBeans())
            {
                if(!cbTmp.isMatchDataSet(dsvbean)) continue;
                Object colValObjTmp=null;
                if(!"[DYN_COL_DATA]".equals(cbTmp.getProperty()))
                {//如果当前列不是存放交叉统计数据的动态列，则每一列都在POJO中有一个名为property的值的成员变量
                    colValObjTmp=mRowDataObjTmp.get(cbTmp.getProperty());
                }else
                {
                    colValObjTmp=mRowDataObjTmp.get(cbTmp.getColumn());
                }
                if(colValObjTmp==null) continue;
                addChildDataSetColValueToMColData(dsvbean,mColData,cbTmp,colValObjTmp);
            }
        }
        log.debug("子数据集关联字段数据："+allKeysBuf.toString());
        return mChildDatasetDataObjs;
    }

    private String getRealDependsColumnsValueAsKey(ReportDataSetValueBean datasetbean,List<String> lstDependsColumns,Object rowDataObj) throws SQLException
    {
        StringBuffer keyBuf=new StringBuffer();
        Object keyTmp;
        boolean isParentColumns=rowDataObj instanceof AbsReportDataPojo;
        for(String relateColumnTmp:lstDependsColumns)
        {
            if(isParentColumns)
            {
                keyTmp=((AbsReportDataPojo)rowDataObj).getColValue(relateColumnTmp);
            }else
            {
                if(!((Map<String,Object>)rowDataObj).containsKey(relateColumnTmp))
                {
                    throw new WabacusRuntimeException("获取报表"+rbean.getPath()+"数据失败，子数据集"+datasetbean.getId()+"中没有取到参与依赖的字段"+relateColumnTmp+"数据");
                }
                keyTmp=((Map<String,Object>)rowDataObj).get(relateColumnTmp);
            }
            if(keyTmp==null) keyTmp="";
            if(!isParentColumns) keyTmp=datasetbean.format(relateColumnTmp,String.valueOf(keyTmp));
            keyBuf.append(String.valueOf(keyTmp)).append("_");
        }
        return keyBuf.toString();
    }
    
    private void addChildDataSetColValueToMColData(ReportDataSetValueBean dsvbean,Map<ColBean,Object> mColData,ColBean cbean,Object colValObj)
    {
        if(!"multiple".equals(dsvbean.getDependstype()))
        {
            mColData.put(cbean,colValObj);
        }else
        {//一条父数据集上的数据可能会对应多条子数据集上的数据
            if(String.valueOf(colValObj).equals("")) return;
            String oldColValTmp=(String)mColData.get(cbean);
            if(oldColValTmp==null||oldColValTmp.trim().equals(""))
            {
                mColData.put(cbean,colValObj);
            }else
            {
                mColData.put(cbean,oldColValTmp+dsvbean.getSeperator()+String.valueOf(colValObj));
            }
        }
    }

    protected void copyListDataToLstResultsData(ReportDataSetValueBean dsvbean,List<Map<String,Object>> lstOneDatasetValueData,
            List<AbsReportDataPojo> lstOneDatasetData,int maxrecordcount)
    {
        if(lstOneDatasetValueData==null||lstOneDatasetValueData.size()==0) return;
        if(maxrecordcount>0&&lstOneDatasetValueData.size()>maxrecordcount)
        {
            while(lstOneDatasetValueData.size()>maxrecordcount)
            {
                lstOneDatasetValueData.remove(lstOneDatasetValueData.size()-1);
            }
        }
        boolean isMultiDataset=rbean.getSbean().isMultiDatasetRows(false);
        List<ColBean> lstColBeans=getLstRealColBeans();
        int i=-1;
        AbsReportDataPojo dataObjTmp;
        for(Map<String,Object> mRowDataObjTmp:lstOneDatasetValueData)
        {
            i++;
            if(mRowDataObjTmp==null||mRowDataObjTmp.size()==0) continue;
            if(i>=lstOneDatasetData.size())
            {
                dataObjTmp=ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,rbean.getPojoClassObj());
                if(isMultiDataset) dataObjTmp.setWx_belongto_datasetid(((ReportDataSetBean)dsvbean.getParent()).getId());
                lstOneDatasetData.add(dataObjTmp);
            }else
            {
                dataObjTmp=lstOneDatasetData.get(i);
            }
            for(ColBean cbTmp:lstColBeans)
            {//依次将dataObjLocalTmp中的各列数据拷入dataObjTmp对象中
                if(!cbTmp.isMatchDataSet(dsvbean)) continue;
                dataObjTmp.setColValue(cbTmp,mRowDataObjTmp.get("[DYN_COL_DATA]".equals(cbTmp.getProperty())?cbTmp.getColumn():cbTmp.getProperty()));
            }
        }
    }
}
