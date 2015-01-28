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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.other.RowSelectDataBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public abstract class AbsDataExportBean implements Cloneable
{
    private final static Log log=LogFactory.getLog(AbsDataExportBean.class);
    
    private String filename;//本<dataexport/>对应的导出的文件名
    
    private Map<String,String> mDynFilename;
    
    private String type;

    private String includeApplicationids;

    private List<String> lstIncludeApplicationids;

    private Map<String,Integer> mReportRecordCounts;
    
    private String rowselect;//如果当前报表是列表报表，且支持导出选中行记录，这里存放相关配置对象，
    
    private RowSelectDataBean rowSelectDataBean;
    
    protected IComponentConfigBean owner;
    
    private DataExportLocalStroageBean localStroageBean;
    
    public AbsDataExportBean(IComponentConfigBean owner,String type)
    {
        this.owner=owner;
        this.type=type;
    }

    public String getType()
    {
        return type;
    }

    public String getIncludeApplicationids()
    {
        return includeApplicationids;
    }

    public void setIncludeApplicationids(String includeApplicationids)
    {
        this.includeApplicationids=includeApplicationids;
    }

    public List<String> getLstIncludeApplicationids()
    {
        return lstIncludeApplicationids;
    }

    public void setLstIncludeApplicationids(List<String> lstIncludeApplicationids)
    {
        this.lstIncludeApplicationids=lstIncludeApplicationids;
    }

    public RowSelectDataBean getRowSelectDataBean()
    {
        return rowSelectDataBean;
    }

    public Map<String,Integer> getMReportRecordCounts()
    {
        return mReportRecordCounts;
    }

    public void setMReportRecordCounts(Map<String,Integer> reportRecordCounts)
    {
        mReportRecordCounts=reportRecordCounts;
    }

    public IComponentConfigBean getOwner()
    {
        return owner;
    }

    public DataExportLocalStroageBean getLocalStroageBean()
    {
        return localStroageBean;
    }

    public void setOwner(IComponentConfigBean owner)
    {
        this.owner=owner;
    }
    
    public String getFilename(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.filename,this.mDynFilename,"");
    }

    public int getDataExportRecordcount(String reportid)
    {
        if(this.mReportRecordCounts==null) return -1;
        return this.mReportRecordCounts.get(reportid).intValue();
    }
    
    public void loadConfig(XmlElementBean eleDataExport)
    {
        String filename=eleDataExport.attributeValue("filename");
        if(filename!=null)
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(filename);
            this.filename=(String)objArr[0];
            this.mDynFilename=(Map<String,String>)objArr[1];
        }
        String dataexportinclude=eleDataExport.attributeValue("include");
        if(dataexportinclude!=null&&!dataexportinclude.trim().equals(""))
        {
            this.lstIncludeApplicationids=Tools.parseStringToList(dataexportinclude,";",false);
        }
        this.rowselect=eleDataExport.attributeValue("rowselect");
        String localstorage=eleDataExport.attributeValue("localstorage");
        if("true".equalsIgnoreCase(localstorage))
        {
            this.localStroageBean=new DataExportLocalStroageBean();
            this.localStroageBean.setDownload(!"false".equalsIgnoreCase(eleDataExport.attributeValue("download")));
            this.localStroageBean.setAutodelete(!"false".equalsIgnoreCase(eleDataExport.attributeValue("autodelete")));
            this.localStroageBean.setZip("true".equalsIgnoreCase(eleDataExport.attributeValue("zip")));
            this.localStroageBean.setDirectorydateformat(eleDataExport.attributeValue("directorydateformat"));
        }else
        {
            this.localStroageBean=null;
        }
    }
    
    public void doPostLoad()
    {
        Object[] objResult=ComponentConfigLoadAssistant.getInstance().parseIncludeApplicationids(this.owner,this.lstIncludeApplicationids);
        this.includeApplicationids=(String)objResult[0];
        this.lstIncludeApplicationids=(List<String>)objResult[1];
        this.mReportRecordCounts=(Map<String,Integer>)objResult[2];
        if(!Tools.isEmpty(this.rowselect)&&this.getOwner() instanceof ReportBean)
        {
            AbsReportType reportTypeObj=Config.getInstance().getReportType(((ReportBean)this.getOwner()).getType());
            if(reportTypeObj instanceof AbsListReportType)
            {
                this.rowSelectDataBean=new RowSelectDataBean();
                this.rowSelectDataBean.setReportBean((ReportBean)this.owner);
                this.rowSelectDataBean.setConfigColsExpression(this.rowselect);
                AbsListReportBean alrbean=(AbsListReportBean)((ReportBean)this.getOwner()).getExtendConfigDataForReportType(AbsListReportType.KEY);
                if(alrbean==null)
                {//如果此报表不是继承其它报表，也就没有父报表的AbsListReportTabBean对象，则生成一个自己的
                    alrbean=new AbsListReportBean(((ReportBean)this.getOwner()));
                    ((ReportBean)this.getOwner()).setExtendConfigDataForReportType(AbsListReportType.KEY,alrbean);
                }
                if(Tools.isEmpty(alrbean.getRowSelectType())||alrbean.getRowSelectType().equals(Consts.ROWSELECT_NONE))
                {
                    alrbean.setRowSelectType(Consts.ROWSELECT_MULTIPLE);
                }
            }
        }
        if(this.rowSelectDataBean==null&&!Tools.isEmpty(this.rowselect))
        {
            log.warn("在"+this.owner.getPath()+"的<dataexport/>中配置rowselect无效，它不是列表报表类型");
        }
        this.rowselect=null;
        if(this.localStroageBean!=null) this.localStroageBean.doPostLoad();
    }
    
    public AbsDataExportBean clone(IComponentConfigBean owner) 
    {
        try
        {
            AbsDataExportBean newBean=(AbsDataExportBean)super.clone();
            newBean.setOwner(owner);
            if(lstIncludeApplicationids!=null)
            {
                newBean.setLstIncludeApplicationids((List<String>)((ArrayList<String>)lstIncludeApplicationids).clone());
            }
            if(mReportRecordCounts!=null)
            {
                newBean.setMReportRecordCounts((Map<String,Integer>)((HashMap<String,Integer>)this.mReportRecordCounts).clone());
//                    if(entryTmp.getValue() instanceof RowSelectDataBean)
//                        mReportRecordCountsNew.put(entryTmp.getKey(),entryTmp.getValue());
            }
            return newBean;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone组件"+this.owner.getPath()+"的数据导出对象失败",e);
        }
    }
}
