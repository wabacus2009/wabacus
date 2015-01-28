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
package com.wabacus.config.component.application.report;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.dataset.select.report.value.AbsReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class ReportDataSetValueBean extends AbsConfigBean
{
    private static Log log=LogFactory.getLog(ReportDataSetValueBean.class);

    private String id;//<value/>的id，如果只有一个<value/>，则可以不配置id

    private Map<String,DependingColumnBean> mDependParents;

    private List<String[]> lstDependRelateColumns;

    private String dependsConditionExpression;

    private String dependstype="single";

    private String seperator=";";//如果是one-to-many的关系，指定同时匹配父数据集一条记录的多条子数据集数据的分隔符。

    private List<String> lstRowSelectValueColProperties;
    
    private AbsReportDataSetValueProvider provider;
    
    private String datasource;//此报表所使用的数据源，默认为<sql/>中的datasource配置，如果这里也没配置，则取wabacus.cfg.xml中<datasources/>标签中的default属性配置的值

    public ReportDataSetValueBean(AbsConfigBean parent)
    {
        super(parent);
        this.datasource=((ReportDataSetBean)parent).getDatasource();
    }

    public String getId()
    {
        return id;
    }

    public String getGuid()
    {
        return this.getReportBean().getId()+"__"+((ReportDataSetBean)this.getParent()).getId()+"__"+this.id;
    }
    
    public void setId(String id)
    {
        this.id=id;
    }

    public AbsReportDataSetValueProvider getProvider()
    {
        return provider;
    }

    public void setProvider(AbsReportDataSetValueProvider provider)
    {
        this.provider=provider;
    }

    public Map<String,DependingColumnBean> getMDependParents()
    {
        return mDependParents;
    }

    public void addRowSelectValueColProperty(String colproperty)
    {
        if(this.lstRowSelectValueColProperties==null) this.lstRowSelectValueColProperties=new ArrayList<String>();
        if(!this.lstRowSelectValueColProperties.contains(colproperty)) this.lstRowSelectValueColProperties.add(colproperty);
    }
    
    public List<String> getLstRowSelectValueColProperties()
    {
        return lstRowSelectValueColProperties;
    }
    
    public boolean hasRowSelectValueConditions()
    {
        return this.lstRowSelectValueColProperties!=null&&this.lstRowSelectValueColProperties.size()>0;
    }
    
    private List<String> lstDependParentColumns;

    private List<String> lstDependMyColumns;

    public List<String> getLstDependParentColumns()
    {
        if(lstDependParentColumns!=null&&lstDependParentColumns.size()>0) return lstDependParentColumns;
        if(lstDependRelateColumns==null||lstDependRelateColumns.size()==0) return null;
        List<String> lstDependParentColumnsTmp=new ArrayList<String>();
        for(String[] arrTmp:this.lstDependRelateColumns)
        {
            lstDependParentColumnsTmp.add(arrTmp[1]);
        }
        this.lstDependParentColumns=lstDependParentColumnsTmp;
        return lstDependParentColumns;
    }

    public List<String> getLstDependMyColumns()
    {
        if(lstDependMyColumns!=null&&lstDependMyColumns.size()>0) return lstDependMyColumns;
        if(lstDependRelateColumns==null||lstDependRelateColumns.size()==0) return null;
        List<String> lstDependMyColumnsTmp=new ArrayList<String>();
        for(String[] arrTmp:this.lstDependRelateColumns)
        {
            lstDependMyColumnsTmp.add(arrTmp[0]);
        }
        this.lstDependMyColumns=lstDependMyColumnsTmp;
        return lstDependMyColumns;
    }

    public void setDependParents(String depends)
    {
        if(depends==null) return;
        depends=depends.trim();
        if(depends.equals(""))
        {
            this.mDependParents=null;
            return;
        }
        this.mDependParents=new HashMap<String,DependingColumnBean>();
        List<String> lstDepends=Tools.parseStringToList(depends,";",false);
        String columnTmp;
        DependingColumnBean dcbeanTmp;
        for(String strTmp:lstDepends)
        {
            int idx=strTmp.indexOf("=");
            if(idx<=0)
            {
                log.warn("报表"+this.getReportBean().getPath()+"的<value/>的depends属性配置的依赖父数据集"+strTmp+"格式无效");
                continue;
            }
            dcbeanTmp=new DependingColumnBean();
            columnTmp=strTmp.substring(0,idx).trim();
            strTmp=strTmp.substring(idx+1).trim();
            idx=columnTmp.indexOf("(");
            if(columnTmp.endsWith(")")&&idx>0)
            {
                dcbeanTmp.setFormatMethodName(columnTmp.substring(0,idx).trim());
                columnTmp=columnTmp.substring(idx+1,columnTmp.length()-1).trim();
            }
            dcbeanTmp.setColumn(columnTmp);
            if(strTmp.length()>1&&strTmp.startsWith("'")&&strTmp.endsWith("'"))
            {
                dcbeanTmp.setVarcharType(true);
                strTmp=strTmp.substring(1,strTmp.length()-1).trim();
            }
            idx=strTmp.indexOf(".");
            if(idx<=0)
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的<value/>的depends属性配置的依赖父数据集"+strTmp+"不合法");
            }
            dcbeanTmp.setParentValueid(strTmp.substring(0,idx).trim());
            if(dcbeanTmp.getParentValueid().equals(this.id))
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"中id为"+this.id+"的<value/>自己依赖自己");
            }
            dcbeanTmp.setParentColumn(strTmp.substring(idx+1).trim());
            this.mDependParents.put(columnTmp,dcbeanTmp);
        }
        if(this.mDependParents.size()==0) this.mDependParents=null;
    }

    public String getDependsConditionExpression()
    {
        return dependsConditionExpression;
    }

    public void setDependsConditionExpression(String dependsConditionExpression)
    {
        if(this.mDependParents==null||this.mDependParents.size()==0)
        {
            this.dependsConditionExpression=null;
        }else if(dependsConditionExpression!=null&&!dependsConditionExpression.trim().equals(""))
        {
            this.dependsConditionExpression=dependsConditionExpression.trim();
        }else
        {//如果没有配置，则构造默认值
            DependingColumnBean dcbeanTmp;
            StringBuffer expressionBuf=new StringBuffer();
            if(this.provider instanceof SQLReportDataSetValueProvider)
            {
                for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
                {
                    dcbeanTmp=entryTmp.getValue();
                    expressionBuf.append(dcbeanTmp.getColumn()).append(" in (");
                    expressionBuf.append("#").append(dcbeanTmp.getParentValueid()).append(".").append(dcbeanTmp.getParentColumn()).append("#) and ");
                }
                this.dependsConditionExpression=expressionBuf.toString().trim();
                if(this.dependsConditionExpression.endsWith(" and"))
                {
                    this.dependsConditionExpression=this.dependsConditionExpression.substring(0,this.dependsConditionExpression.length()-4);
                }
            }else
            {
                for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
                {
                    dcbeanTmp=entryTmp.getValue();
                    expressionBuf.append(dcbeanTmp.getColumn()).append("=");
                    expressionBuf.append("#").append(dcbeanTmp.getParentValueid()).append(".").append(dcbeanTmp.getParentColumn()).append("#;");
                }
                if(expressionBuf.charAt(expressionBuf.length()-1)==';') expressionBuf.deleteCharAt(expressionBuf.length()-1);
                this.dependsConditionExpression=expressionBuf.toString().trim();
            }
        }
    }

    public String getDependstype()
    {
        return dependstype;
    }

    public void setDependstype(String dependstype)
    {
        if(dependstype==null) return;
        dependstype=dependstype.toLowerCase().trim();
        if(dependstype.equals(""))
        {
            dependstype="single";
        }else if(!dependstype.equals("single")&&!dependstype.equals("multiple"))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的数据集"+this.id+"失败，不能将数据集依赖关系配置为"+dependstype);
        }
        this.dependstype=dependstype;
    }

    public String getSeperator()
    {
        return seperator;
    }

    public void setSeperator(String seperator)
    {
        this.seperator=seperator;
    }

    public String getDatasource()
    {
        if(datasource==null||datasource.trim().equals(""))
        {
            datasource=((ReportDataSetBean)this.getParent()).getDatasource();
        }
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }

    public boolean isMatchDatasetid(String datasetid)
    {
        if(datasetid==null||datasetid.trim().equals(""))
        {
            return this.id==null||this.id.trim().equals("")||this.id.equals(Consts.DEFAULT_KEY);
        }else
        {
            return datasetid.equals(id);
        }
    }

    public boolean isDependentDataSet()
    {
        return this.mDependParents!=null&&this.mDependParents.size()>0;
    }

    public List<String> getAllParentValueIds()
    {
        if(this.mDependParents==null||this.mDependParents.size()==0) return null;
        List<String> lstResults=new ArrayList<String>();
        DependingColumnBean dcbeanTmp;
        for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
        {
            dcbeanTmp=entryTmp.getValue();
            if(dcbeanTmp.getParentValueid()!=null&&!dcbeanTmp.getParentValueid().trim().equals(""))
            {
                lstResults.add(dcbeanTmp.getParentValueid());
            }
        }
        return lstResults;
    }

    public String format(String columnname,String value)
    {
        if(this.mDependParents==null||this.mDependParents.size()==0) return value;
        DependingColumnBean dcbean=this.mDependParents.get(columnname);
        if(dcbean==null) return value;
        return dcbean.format(value);
    }

    public String getRealDependsConditionExpression(List<AbsReportDataPojo> lstReportData)
    {
        if(!this.isDependentDataSet()) return "";
        if(lstReportData==null||lstReportData.size()==0||this.dependsConditionExpression==null||this.dependsConditionExpression.trim().equals(""))
            return "";
        String realConExpress=this.dependsConditionExpression;
        DependingColumnBean dcbeanTmp;
        StringBuffer parentValuesBuf=new StringBuffer();
        for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
        {
            dcbeanTmp=entryTmp.getValue();
            for(AbsReportDataPojo dataObjTmp:lstReportData)
            {//取到报表已加载的POJO数据中所有此父字段列的值
                Object parentColVal=dataObjTmp.getColValue(dcbeanTmp.getParentColumn());
                if(parentColVal==null) parentColVal="";
                if(dcbeanTmp.isVarcharType()) parentValuesBuf.append("'");
                parentValuesBuf.append(String.valueOf(parentColVal));
                if(dcbeanTmp.isVarcharType()) parentValuesBuf.append("'");
                parentValuesBuf.append(",");
            }
            if(parentValuesBuf.length()>0&&parentValuesBuf.charAt(parentValuesBuf.length()-1)==',')
            {
                parentValuesBuf.deleteCharAt(parentValuesBuf.length()-1);
            }
            if(parentValuesBuf.length()==0&&dcbeanTmp.isVarcharType()) parentValuesBuf.append("''");
            realConExpress=Tools.replaceAll(realConExpress,"#"+dcbeanTmp.getParentValueid()+"."+dcbeanTmp.getParentColumn()+"#",parentValuesBuf
                    .toString());
        }
        return realConExpress;
    }

    public void afterSqlLoad()
    {
        this.provider.afterSqlLoad();
    }

    public void doPostLoad()
    {
        this.provider.doPostLoad();
        if(this.mDependParents!=null&&this.mDependParents.size()>0)
        {
            this.getReportBean().setPageLazyloadataCount(-1);
            this.getReportBean().setDataexportLazyloadataCount(-1);
            this.lstDependRelateColumns=new ArrayList<String[]>();
            for(Entry<String,DependingColumnBean> entryTmp:this.mDependParents.entrySet())
            {
                this.lstDependRelateColumns.add(new String[] { entryTmp.getValue().getColumn(), entryTmp.getValue().getParentColumn() });
            }
        }
    }
    
    public ReportDataSetValueBean clone(AbsConfigBean parent)
    {
        ReportDataSetValueBean dsvbeanNew=(ReportDataSetValueBean)super.clone(parent);
        if(this.provider!=null)
        {
            dsvbeanNew.setProvider(provider.clone(dsvbeanNew));
        }
        cloneExtendConfig(dsvbeanNew);
        return dsvbeanNew;
    }

    private class DependingColumnBean
    {
        private String column;

        private Class formatClass;//对此字段数据进行格式化的JAVA类对象

        private Method formatMethod;

        private String parentValueid;//父数据集<value/>的ID

        private String parentColumn;

        private boolean isVarcharType;

        public String getColumn()
        {
            return column;
        }

        public void setColumn(String column)
        {
            this.column=column;
        }

        public String getParentValueid()
        {
            return parentValueid;
        }

        public void setParentValueid(String parentValueid)
        {
            this.parentValueid=parentValueid;
        }

        public String getParentColumn()
        {
            return parentColumn;
        }

        public void setParentColumn(String parentColumn)
        {
            this.parentColumn=parentColumn;
        }

        public boolean isVarcharType()
        {
            return isVarcharType;
        }

        public void setVarcharType(boolean isVarcharType)
        {
            this.isVarcharType=isVarcharType;
        }

        public void setFormatMethodName(String formatmethodname)
        {
            if(formatmethodname==null||formatmethodname.trim().equals(""))
            {
                this.formatClass=null;
                this.formatMethod=null;
            }else
            {
                formatmethodname=formatmethodname.trim();
                this.formatClass=getReportBean().getFormatMethodClass(formatmethodname,new Class[] { String.class });
                try
                {
                    this.formatMethod=this.formatClass.getMethod(formatmethodname,new Class[] { String.class });
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("加载报表"+getReportBean().getPath()+"的<value/>子标签时，获取格式化方法"+formatmethodname+"对象失败",e);
                }
            }
        }

        public String format(String value)
        {
            if(formatClass==null||formatMethod==null) return value;
            try
            {
                return (String)formatMethod.invoke(formatClass,new Object[] { value });
            }catch(Exception e)
            {
                log.warn("在查询报表"+getReportBean().getPath()+"的ID为"+id+"的记录集时，格式化字段"+column+"上的数据"+value+"失败",e);
                return value;
            }
        }
    }
}
