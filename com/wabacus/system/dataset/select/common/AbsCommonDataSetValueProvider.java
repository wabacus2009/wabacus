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
package com.wabacus.system.dataset.select.common;

import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.crosslist.AbsCrossListReportColAndGroupBean;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.system.inputbox.autocomplete.AutoCompleteBean;
import com.wabacus.system.inputbox.option.AbsOptionBean;
import com.wabacus.util.Tools;

public abstract class AbsCommonDataSetValueProvider implements Cloneable
{
    protected String datasource;//此报表所使用的数据源，默认为wabacus.cfg.xml中<datasources/>标签中的default属性配置的值

    protected List<ConditionBean> lstConditions;

    protected AbsOptionBean ownerOptionBean;//如果当前是查询下拉选项或输入联想选项数据的数据集，这里存放<option/>配置对象

    protected AutoCompleteBean ownerAutoCompleteBean;//如果当前是查询自动填充数据，这里存放<autocomplete/>配置对象

    protected AbsCrossListReportColAndGroupBean ownerCrossReportColAndGroupBean;//如果当前是查询交叉报表动态列的数据集，这里存放相应的<group/>或<col/>配置对象

    public String getDatasource()
    {
        if(datasource==null||datasource.trim().equals(""))
        {
            datasource=this.getReportBean().getSbean().getDatasource();
        }
        return datasource;
    }

    public List<ConditionBean> getLstConditions()
    {
        return lstConditions;
    }

    public ReportBean getReportBean()
    {
        if(this.ownerAutoCompleteBean!=null)
        {
            return this.ownerAutoCompleteBean.getOwner().getOwner().getReportBean();
        }else if(ownerOptionBean!=null)
        {
            return ownerOptionBean.getOwnerInputboxObj().getOwner().getReportBean();
        }else if(ownerCrossReportColAndGroupBean!=null)
        {
            return ownerCrossReportColAndGroupBean.getOwner().getReportBean();
        }
        return null;
    }

    public void setOwnerOptionBean(AbsOptionBean ownerOptionBean)
    {
        this.ownerOptionBean=ownerOptionBean;
    }

    public void setOwnerAutoCompleteBean(AutoCompleteBean ownerAutoCompleteBean)
    {
        this.ownerAutoCompleteBean=ownerAutoCompleteBean;
    }

    public void setOwnerCrossReportColAndGroupBean(AbsCrossListReportColAndGroupBean ownerCrossReportColAndGroupBean)
    {
        this.ownerCrossReportColAndGroupBean=ownerCrossReportColAndGroupBean;
    }

    public abstract List<Map<String,String>> getLstSelectBoxOptions(ReportRequest rrequest,Map<String,String> mParentInputboxValues);

    public abstract List<Map<String,String>> getLstTypePromptOptions(ReportRequest rrequest,String txtValue);

    public abstract Map<String,String> getAutoCompleteColumnsData(ReportRequest rrequest,Map<String,String> mParams);

    public abstract List<Map<String,String>> getDynamicColGroupDataSet(ReportRequest rrequest);

    public void loadConfig(XmlElementBean eleDatasetAttributeOwnerBean)
    {
        this.lstConditions=ComponentConfigLoadManager.loadCommonDatasetConditios(this.getReportBean(),eleDatasetAttributeOwnerBean
                .getChildElementByName("datasetconditions"));
        String datasource=eleDatasetAttributeOwnerBean.attributeValue("datasource");
        if(datasource!=null) this.datasource=datasource.trim();
    }

    public void doPostLoad()
    {
        ReportBean reportbean=this.getReportBean();
        SqlBean sqlbean=reportbean.getSbean();
        if(lstConditions!=null&&lstConditions.size()>0)
        {
            ConditionBean cbRefered=null;
            for(ConditionBean cbTmp:lstConditions)
            {
                cbRefered=null;
                if(Tools.isDefineKey("ref",cbTmp.getName()))
                {//此条件引用了其它<sql/>中配置的条件
                    cbRefered=sqlbean.getConditionBeanByName(Tools.getRealKeyByDefine("ref",cbTmp.getName()));
                    if(cbRefered==null)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()+"失败，其<datasetconditions/>、<staticonditions/>中配置的name为"
                                +cbTmp.getName()+"的查询条件引用的查询条件在<sql/>中不存在");
                    }
                }
                parseCondition(reportbean,cbTmp,cbRefered);
            }
        }
    }

    protected void parseCondition(ReportBean rbean,ConditionBean cbean,ConditionBean cbeanRefered)
    {
        if(cbeanRefered!=null)
        {//此条件引用了其它<sql/>中配置的条件
            cbean.setName(cbeanRefered.getName());
            cbean.setConstant(cbeanRefered.isConstant());
            cbean.setDefaultvalue(cbeanRefered.getDefaultvalue());
            cbean.setKeepkeywords(cbeanRefered.isKeepkeywords());
            cbean.setSource(cbeanRefered.getSource());
            if(cbean.getConditionExpression()==null||cbean.getConditionExpression().getValue()==null
                    ||cbean.getConditionExpression().getValue().trim().equals(""))
            {
                cbean.setConditionExpression(cbeanRefered.getConditionExpression());
            }
        }else
        {
            if(cbean.isConditionValueFromUrl()) rbean.addParamNameFromURL(cbean.getName());
        }
        if(cbean.getConditionExpression()==null||cbean.getConditionExpression().getValue()==null
                ||cbean.getConditionExpression().getValue().trim().equals(""))
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的name为"+cbean.getName()+"的查询条件或其引用的查询条件没有配置条件表达式");
        }
        if(!cbean.isConstant()&&!cbean.isConditionValueFromSession()&&this.ownerOptionBean!=null
                &&this.ownerOptionBean.getOwnerInputboxObj() instanceof AbsSelectBox
                &&((AbsSelectBox)this.ownerOptionBean.getOwnerInputboxObj()).isDependsOtherInputbox())
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"配置的选择框类型的输入框失败，依赖其它选择框的子选择框的查询条件的数据不能配置为从url中获取，只能配置为从session中获取");
        }
    }
    
    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public AbsCommonDataSetValueProvider clone(Object newOwner)
    {
        try
        {
            AbsCommonDataSetValueProvider newObj=(AbsCommonDataSetValueProvider)clone();
            if(this.ownerCrossReportColAndGroupBean!=null)
            {
                newObj.setOwnerCrossReportColAndGroupBean((AbsCrossListReportColAndGroupBean)newOwner);
            }else if(this.ownerOptionBean!=null)
            {
                newObj.setOwnerOptionBean((AbsOptionBean)newOwner);
            }else if(this.ownerAutoCompleteBean!=null)
            {
                newObj.setOwnerAutoCompleteBean((AutoCompleteBean)newOwner);
            }
            newObj.lstConditions=ComponentConfigLoadAssistant.getInstance().cloneLstConditionBeans(null,this.lstConditions);
            return newObj;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static AbsCommonDataSetValueProvider createCommonDataSetValueProviderObj(ReportBean rbean,String dataset)
    {
        if(dataset==null||dataset.trim().equals("")) return null;
        dataset=dataset.trim();
        AbsCommonDataSetValueProvider providerObjResult=null;
        if(Tools.isDefineKey("class",dataset))
        {//定义的是class类
            dataset=Tools.getRealKeyByDefine("class",dataset).trim();
            if(dataset.equals("")) return null;
            Class c=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(dataset);
            Object obj=null;
            try
            {
                obj=c.newInstance();
            }catch(Exception e)
            {
                throw new WabacusConfigLoadingException("实例化报表"+rbean.getPath()+"的数据集类"+dataset+"失败",e);
            }
            if(!(obj instanceof AbsCommonDataSetValueProvider))
            {
                throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"的数据集类"+dataset+"没有继承框架的"+AbsCommonDataSetValueProvider.class.getName()
                        +"父类");
            }
            providerObjResult=(AbsCommonDataSetValueProvider)obj;
        }else
        {
            String[] providerDataset=parseDatsetProvider(dataset);
            if(providerDataset==null||providerDataset.length!=2||Tools.isEmpty(providerDataset[1])) return null;
            AbsCommonDataSetValueProvider providerObj=Config.getInstance().getCommonDatasetValueProvider(providerDataset[0]);
            if(providerObj instanceof RelationalDBCommonDataSetValueProvider)
            {
                if(providerDataset[1].toLowerCase().startsWith("call "))
                {
                    providerObjResult=new SPCommonDataSetValueProvider();
                }else
                {
                    providerObjResult=new SQLCommonDataSetValueProvider();
                }
            }else
            {
                try
                {
                    providerObjResult=providerObj.getClass().newInstance();
                }catch(Exception e)
                {
                    throw new WabacusConfigLoadingException("实例化报表"+rbean.getPath()+"的数据集类"+providerObj.getClass()+"失败",e);
                }
            }
        }
        return providerObjResult;
    }
    
    protected static String[] parseDatsetProvider(String dataset)
    {
        if(dataset==null||dataset.trim().equals("")) return null;
        String providerName=null;
        int idx1=dataset.indexOf("{");
        int idx2=dataset.lastIndexOf("}");
        if(idx1>0&&idx2==dataset.length()-1)
        {
            providerName=dataset.substring(0,idx1);
            if(providerName.indexOf(" ")>0||providerName.indexOf(",")>0||providerName.indexOf("=")>0)
            {
                providerName=null;
            }else
            {
                providerName=providerName.trim();
                dataset=dataset.substring(idx1+1,idx2).trim();
                if(dataset.equals("")) return null;
            }
        }
        return new String[]{providerName,dataset};
    }
}
