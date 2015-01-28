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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.typeprompt.TypePromptBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.dataset.select.rationaldbassistant.GetDataSetBySP;
import com.wabacus.system.dataset.select.rationaldbassistant.ISPDataSetProvider;
import com.wabacus.system.dataset.select.rationaldbassistant.SPDataSetValueBean;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.option.TypepromptOptionBean;
import com.wabacus.util.Tools;

public class SPCommonDataSetValueProvider extends RelationalDBCommonDataSetValueProvider implements ISPDataSetProvider
{
    private SPDataSetValueBean spbean;
    
    private Map<String,ConditionBean> mConditions;
    
    private String typepromptMatchExpression;
    
    public SPDataSetValueBean getSpbean()
    {
        return spbean;
    }

    public ConditionBean getConditionBeanByName(String name)
    {
        if(mConditions==null) return null;
        return this.mConditions.get(name);
    }

    public boolean isUseSystemParams()
    {
        if(this.ownerAutoCompleteBean!=null) return true;
        if(this.ownerCrossReportColAndGroupBean!=null) return false;
        if(this.ownerOptionBean!=null&&this.ownerOptionBean.getOwnerInputboxObj() instanceof AbsSelectBox
                &&((AbsSelectBox)this.ownerOptionBean.getOwnerInputboxObj()).isDependsOtherInputbox())
        {
            return true;
        }
        return false;
    }
    
    public List<Map<String,String>> getLstSelectBoxOptions(ReportRequest rrequest,Map<String,String> mParentInputboxValues)
    {
        StringBuffer parentValuesBuf=new StringBuffer();
        if(mParentInputboxValues!=null&&mParentInputboxValues.size()>0)
        {//有父输入框数据，说明当前是依赖其它输入框的下拉框，将所有父输入框数据组装成字符串传给存储过程
            for(Entry<String,String> entryTmp:mParentInputboxValues.entrySet())
            {
                parentValuesBuf.append("{[(<[").append(entryTmp.getKey()+":"+entryTmp.getValue()).append(">)]}");
            }
        }
        GetDataSetBySP spDataSet=new GetDataSetBySP(rrequest,this.getReportBean());
        return parseOptionsDataSet(spDataSet.getCommonDataSet(this,this.ownerOptionBean,parentValuesBuf),getMSelectBoxColKeyAndColumns(),-1);
    }

    public List<Map<String,String>> getLstTypePromptOptions(ReportRequest rrequest,String txtValue)
    {
        if(this.typepromptMatchExpression==null)
        {
            this.typepromptMatchExpression=((TypepromptOptionBean)this.ownerOptionBean).getMatchColSQLConditionExpression();
        }
        if(txtValue==null) txtValue="";
        StringBuffer systemParamsBuf=new StringBuffer();
        systemParamsBuf.append("{[(<[text:").append(txtValue).append(">)]}");
        systemParamsBuf.append("{[(<[expression:").append(Tools.replaceAll(this.typepromptMatchExpression,"#data#",txtValue)).append(">)]}");
        GetDataSetBySP spDataSet=new GetDataSetBySP(rrequest,this.getReportBean());
        TypePromptBean typePromptBean=((TextBox)this.ownerOptionBean.getOwnerInputboxObj()).getTypePromptBean();
        return parseOptionsDataSet(spDataSet.getCommonDataSet(this,this.ownerOptionBean,systemParamsBuf),getMTypePromptColKeyAndColumns(),
                typePromptBean.getResultcount());
    }

    public Map<String,String> getAutoCompleteColumnsData(ReportRequest rrequest,Map<String,String> mParams)
    {
        StringBuffer systemParamsBuf=new StringBuffer();
        String colValTmp;
        for(String colpropertyTmp:this.ownerAutoCompleteBean.getLstColPropertiesInColvalueConditions())
        {
            colValTmp=mParams.get(colpropertyTmp);
            if(colValTmp==null) colValTmp="";
            systemParamsBuf.append("{[(<["+colpropertyTmp+":"+colValTmp+">)]}");
        }
        GetDataSetBySP spDataSet=new GetDataSetBySP(rrequest,this.getReportBean());
        return parseAutoCompleteDataSet(spDataSet.getCommonDataSet(this,this.ownerAutoCompleteBean,systemParamsBuf));
    }

    public List<Map<String,String>> getDynamicColGroupDataSet(ReportRequest rrequest)
    {
        GetDataSetBySP spDataSet=new GetDataSetBySP(rrequest,this.getReportBean());
        return parseDynamicColGroupDataSet(spDataSet.getCommonDataSet(this,this.ownerCrossReportColAndGroupBean,null));
    }
    
    public void doPostLoad()
    {
        super.doPostLoad();
        if(this.lstConditions!=null&&this.lstConditions.size()>0)
        {
            this.mConditions=new HashMap<String,ConditionBean>();
            for(ConditionBean cbTmp:this.lstConditions)
            {
                this.mConditions.put(cbTmp.getName(),cbTmp);
            }
        }else
        {
            this.mConditions=null;
        }
        this.spbean=new SPDataSetValueBean(this);
        this.spbean.parseStoreProcedure(this.getReportBean(),this.value);
    }

}

