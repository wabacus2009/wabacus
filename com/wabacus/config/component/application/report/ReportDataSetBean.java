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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.dataset.select.report.value.AbsReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.RelationalDBReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.SPReportDataSetValueProvider;
import com.wabacus.system.dataset.select.report.value.SQLReportDataSetValueProvider;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class ReportDataSetBean extends AbsConfigBean
{
    private String id;

    private String datasource;//此报表所使用的数据源，默认为wabacus.cfg.xml中<datasources/>标签中的default属性配置的值
    
    private String groupid;//所有通过mergetop属性合并在一起的算一个组，它们的组ID都是第一个<dataset/>的id，如果是一个独立的<dataset/>，它的组id就是它自己的id
    
    private List<XmlElementBean> lstEleValueBeans;//存放其下配置的所有<value/>子标签，只在加载时存放一次，加载完后清空

    private List<ReportDataSetValueBean> lstValueBeans;//本<dataset/>中配置的所有<value/>对象
    
    private Map<String,ReportDataSetValueBean> mValueBeans;
    
    private boolean mergetop;//当前数据集查询出的数据是否简单的合并到上一个<dataset/>中，做为上一个数据集的记录，目前只有图形报表的数据集会用到此属性
    
    private String datasetstyleproperty;//配置在<dataset/>的styleproperty的样式，只对图形报表类型有效
    
    private List<String> lstDynDatasetstylepropertyParts;
    
    public ReportDataSetBean(AbsConfigBean parent)
    {
        super(parent);
        this.datasource=((SqlBean)parent).getDatasource();
    }
    
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id=id;
    }

    public String getGroupid()
    {
        return groupid;
    }

    public void setGroupid(String groupid)
    {
        this.groupid=groupid;
    }

    public boolean isMergetop()
    {
        return mergetop;
    }

    public void setMergetop(boolean mergetop)
    {
        this.mergetop=mergetop;
    }

    public List<XmlElementBean> getLstEleValueBeans()
    {
        return lstEleValueBeans;
    }

    public void setLstEleValueBeans(List<XmlElementBean> lstEleValueBeans)
    {
        this.lstEleValueBeans=lstEleValueBeans;
    }

    public List<ReportDataSetValueBean> getLstValueBeans()
    {
        return lstValueBeans;
    }

    public String getDatasource()
    {
        if(datasource==null||datasource.trim().equals(""))
        {
            datasource=((SqlBean)this.getParent()).getDatasource();
        }
        return datasource;
    }

    public void setDatasource(String datasource)
    {
        this.datasource=datasource;
    }

    public String getDatasetstyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.datasetstyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.datasetstyleproperty,this.lstDynDatasetstylepropertyParts,"");
    }

    public void setDatasetstyleproperty(String datasetstyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.datasetstyleproperty=datasetstyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(datasetstyleproperty);
            this.datasetstyleproperty=(String)objArr[0];
            this.lstDynDatasetstylepropertyParts=(List<String>)objArr[1];
        }
    }

    public ReportDataSetValueBean getDatasetValueBeanById(String valueid)
    {
        if(this.mValueBeans==null) return null;
        if(valueid==null||valueid.trim().equals("")) valueid=Consts.DEFAULT_KEY;
        return this.mValueBeans.get(valueid);
    }
    
    public ReportDataSetValueBean getDatasetValueBeanOfCbean(ColBean cbean)
    {
        if(this.mValueBeans==null||this.mValueBeans.size()==0) return null;
        if(cbean.getLstDatasetValueids()==null||cbean.getLstDatasetValueids().size()==0)
        {//如果没有为它配置datasetvalueid，则说明所有<dataset/>下面只有一个<value/>，直接返回即可
            return this.mValueBeans.entrySet().iterator().next().getValue();
        }
        SqlBean sbean=(SqlBean)this.getParent();
        if(sbean.isHorizontalDataset()
                &&(cbean.getColumn().equals(sbean.getHdsTitleLabelCbean().getColumn())||cbean.getColumn().equals(
                        sbean.getHdsTitleValueCbean().getColumn())))
        {//如果是横向数据集，且当前<col/>就是查询标题行的各列数据或显示label，则返回true，因为所有<value/>都会查询这两列数据
            throw new WabacusRuntimeException("报表"+cbean.getReportBean().getPath()+"是横向数据集，且"+cbean.getColumn()
                    +"为显示标题行的列，所有<value/>都会加载它的数据，因此不能调用此方法获取它的数据集<value/>对象");
        }
        for(String valueidTmp:cbean.getLstDatasetValueids())
        {
            if(this.mValueBeans.containsKey(valueidTmp)) return this.mValueBeans.get(valueidTmp);
        }
        return null;
    }
    
    boolean isDependentDatasetValue(String valueid)
    {
        if(valueid==null||valueid.trim().equals("")) valueid=Consts.DEFAULT_KEY;
        if(this.mValueBeans==null||this.mValueBeans.get(valueid)==null) return false;
        return this.mValueBeans.get(valueid).isDependentDataSet();
    }
    
    public boolean loadDatasetValues()
    {
        if(lstEleValueBeans==null||lstEleValueBeans.size()==0) return false;
        this.lstValueBeans=new ArrayList<ReportDataSetValueBean>();
        this.mValueBeans=new HashMap<String,ReportDataSetValueBean>();
        ReportBean rbean=this.getReportBean();
        List<String> lstExistDatasetids=new ArrayList<String>();//存放已经处理过的<value/>的id属性
        boolean isExistNoIdValue=false;//是否已经存在没有配置id的<value/>
        String valueidTmp;
        for(XmlElementBean eleValueBeanTmp:lstEleValueBeans)
        {
            if(eleValueBeanTmp==null) continue;
            valueidTmp=eleValueBeanTmp.attributeValue("id");
            valueidTmp=valueidTmp==null?"":valueidTmp.trim();
            if(valueidTmp.equals(""))
            {
                if(this.lstValueBeans.size()>0)
                {//已经有<value/>，说明配置了多个<value/>
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的<value/>失败，当使用多个<value/>时，所有<value/>标签必须配置id属性，且不能重复");
                }
                valueidTmp=Consts.DEFAULT_KEY;
                isExistNoIdValue=true;
            }else
            {
                if(isExistNoIdValue)
                {//已经存在没有配置id的<value/>
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的<value/>失败，当使用多个<value/>时，所有<value/>标签必须配置id属性，且不能重复");
                }
                if(lstExistDatasetids.contains(valueidTmp))
                {
                    throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的<value/>失败，id属性为"+valueidTmp+"的<value/>存在重复");
                }
                lstExistDatasetids.add(valueidTmp);
            }
            ReportDataSetValueBean datasetValueBean=new ReportDataSetValueBean(this);
            datasetValueBean.setElementBean(eleValueBeanTmp);
            datasetValueBean.setId(valueidTmp.trim());
            datasetValueBean.setDatasource(eleValueBeanTmp.attributeValue("datasource"));
            datasetValueBean.setDependParents(eleValueBeanTmp.attributeValue("depends"));
            datasetValueBean.setDependsConditionExpression(eleValueBeanTmp.attributeValue("dependscondition"));
            String dependstype=eleValueBeanTmp.attributeValue("dependstype");
            if(dependstype!=null) datasetValueBean.setDependstype(dependstype.trim());
            String seperator=eleValueBeanTmp.attributeValue("seperator");
            if(seperator!=null) datasetValueBean.setSeperator(seperator);
            AbsReportDataSetValueProvider dsvProviderObj=null;
            String provider=eleValueBeanTmp.attributeValue("provider");
            if(provider!=null&&Tools.isDefineKey("class",provider))
            {
                Object datasetValueTypeObj=null;
                try
                {
                    datasetValueTypeObj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(Tools.getRealKeyByDefine("class",provider)).newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("配置<datasetvalue-provider/>的class："+provider+"类无法实例化",e);
                }
                if(!AbsReportDataSetValueProvider.class.isInstance(datasetValueTypeObj))
                {
                    throw new WabacusConfigLoadingException("配置<datasetvalue-provider/>的class："+provider+"没有继承"+AbsReportDataSetValueProvider.class.getName()+"类");
                }
                dsvProviderObj=(AbsReportDataSetValueProvider)datasetValueTypeObj;
            }else
            {
                dsvProviderObj=Config.getInstance().getReportDatasetValueProvider(provider);
                if(dsvProviderObj==null)
                {
                    if(provider==null||provider.trim().equals(""))
                    {
                        throw new WabacusConfigLoadingException("没有在wabacus.cfg.xml中指定全局默认报表数据集provider，必须为报表"+rbean.getPath()+"的数据集<value/>指定provider");
                    }else
                    {
                        throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的数据集<value/>指定的provider："+provider+"不存在");
                    }
                }
                if(dsvProviderObj instanceof RelationalDBReportDataSetValueProvider)
                {
                    String sqlValue=eleValueBeanTmp.getContent();
                    sqlValue=Tools.formatStringBlank(sqlValue);
                    if(sqlValue==null||sqlValue.trim().equals(""))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"的<sql/>下的<value/>配置失败，没有为<value/>标签配置查询数据的SQL语句或JAVA类");
                    }
                    sqlValue=sqlValue.trim();
                    while(sqlValue.endsWith(";"))
                    {
                        sqlValue=sqlValue.substring(0,sqlValue.length()-1).trim();
                    }
                    if(sqlValue.startsWith("{")&&sqlValue.endsWith("}")) sqlValue=sqlValue.substring(1,sqlValue.length()-1).trim();
                    if(sqlValue.toLowerCase().indexOf("call ")==0||sqlValue.toLowerCase().indexOf("{call ")==0)
                    {
                        dsvProviderObj=new SPReportDataSetValueProvider();
                    }else
                    {
                        dsvProviderObj=new SQLReportDataSetValueProvider();
                    }
                    ((RelationalDBReportDataSetValueProvider)dsvProviderObj).setValue(sqlValue);
                }else
                {
                    try
                    {
                        dsvProviderObj=dsvProviderObj.getClass().newInstance();
                    }catch(Exception e)
                    {
                        throw new WabacusConfigLoadingException("实例化"+dsvProviderObj.getClass().getName()+"失败",e);
                    }
                }
            }
            dsvProviderObj.setOwnerDataSetValueBean(datasetValueBean);
            datasetValueBean.setProvider(dsvProviderObj);
            dsvProviderObj.loadConfig(eleValueBeanTmp);
            this.lstValueBeans.add(datasetValueBean);
            this.mValueBeans.put(datasetValueBean.getId(),datasetValueBean);
        }
        this.lstEleValueBeans=null;
        return true;
    }
    
    public void afterSqlLoad()
    {
        for(ReportDataSetValueBean valuebeanTmp:this.lstValueBeans)
        {
            valuebeanTmp.afterSqlLoad();
        }
        List<ReportDataSetValueBean> lstResults=new ArrayList<ReportDataSetValueBean>();//按依赖关系依次存放各数据集对象，这里存放的顺序就是它们的执行顺序
        List<String> lstProcessedValueIds=new ArrayList<String>();
        ReportDataSetValueBean vbeanTmp;
        List<ReportDataSetValueBean> lstTmp=new ArrayList<ReportDataSetValueBean>();
        while(this.lstValueBeans.size()>0)
        {
            for(int i=0;i<this.lstValueBeans.size();i++)
            {
                vbeanTmp=this.lstValueBeans.get(i);
                if(vbeanTmp.getMDependParents()==null||vbeanTmp.getMDependParents().size()==0
                        ||hasProcessedAllParentValues(vbeanTmp,lstProcessedValueIds))
                {
                    lstProcessedValueIds.add(vbeanTmp.getId());
                    lstResults.add(vbeanTmp);
                }else
                {
                    lstTmp.add(vbeanTmp);
                }
            }
            if(lstTmp.size()==this.lstValueBeans.size())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的<sql/>中的<value/>配置失败，存在循环依赖或者依赖的父数据集ID不存在的配置");
            }
            this.lstValueBeans=lstTmp;
            lstTmp.clear();
        }
        this.lstValueBeans=lstResults;
    }
    
    private boolean hasProcessedAllParentValues(ReportDataSetValueBean svbeanTmp,List<String> lstProcessedValueIds)
    {
        if(svbeanTmp.getMDependParents()==null||svbeanTmp.getMDependParents().size()==0) return true;//如果没有父数据集
        if(lstProcessedValueIds==null||lstProcessedValueIds.size()==0) return false;
        List<String> lstParentValueids=svbeanTmp.getAllParentValueIds();
        for(String parentValueidTmp:lstParentValueids)
        {
            if(!lstProcessedValueIds.contains(parentValueidTmp)) return false;
        }
        return true;
    }

    public AbsConfigBean clone(AbsConfigBean parent)
    {
        ReportDataSetBean dsbeanNew=(ReportDataSetBean)super.clone(parent);
        if(this.lstValueBeans!=null)
        {
            List<ReportDataSetValueBean> lstSqlValueBeansNew=new ArrayList<ReportDataSetValueBean>();
            Map<String,ReportDataSetValueBean> mValueBeansNew=new HashMap<String,ReportDataSetValueBean>();
            ReportDataSetValueBean dsvbeanTmp;
            for(ReportDataSetValueBean svbeanTmp:this.lstValueBeans)
            {
                dsvbeanTmp=svbeanTmp.clone(dsbeanNew);
                lstSqlValueBeansNew.add(dsvbeanTmp);
                mValueBeansNew.put(dsvbeanTmp.getId(),dsvbeanTmp);
            }
            dsbeanNew.lstValueBeans=lstSqlValueBeansNew;
            dsbeanNew.mValueBeans=mValueBeansNew;
        }
        cloneExtendConfig(dsbeanNew);
        return dsbeanNew;
    }
}
