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
package com.wabacus.config.component.application.report.condition;

import java.util.List;

import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportDataSetValueBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Tools;

public class ConditionInSqlBean implements Cloneable
{
    private String conditionname;
    
    private String placeholder;
    
    private ConditionExpressionBean conditionExpression;
    
    private ReportDataSetValueBean owner;
    
    public ConditionInSqlBean(ReportDataSetValueBean owner)
    {
        this.owner=owner;
    }
    
    public String getConditionname()
    {
        return conditionname;
    }

    public String getRealConditionname()
    {
        if(conditionname==null||conditionname.trim().equals("")||conditionname.equals("{#condition#}"))
        {
            return null;
        }
        if(conditionname.startsWith("{#")&&conditionname.endsWith("#}"))
        {
            return conditionname.substring(2,conditionname.length()-2);
        }
        if(conditionname.startsWith("#")&&conditionname.endsWith("#"))
        {//#name#形式
            return conditionname.substring(1,conditionname.length()-1);
        }
        return conditionname;
    }
    
    public void setConditionname(String conditionname)
    {
        this.conditionname=conditionname;
    }

    public String getPlaceholder()
    {
        return placeholder;
    }

    public void setPlaceholder(String placeholder)
    {
        this.placeholder=placeholder;
    }

    public ConditionExpressionBean getConditionExpression()
    {
        return conditionExpression;
    }

    public void setConditionExpression(ConditionExpressionBean conditionExpression)
    {
        this.conditionExpression=conditionExpression;
    }

    public String parseConditionInSql(ReportRequest rrequest,String sql,List<String> lstConditionValues,
            List<IDataType> lstConditionTypes)
    {
        String conname=this.getRealConditionname();
        if(conname==null||conname.trim().equals(""))
        {
            if(conditionname.equals("{#condition#}"))
            {
                return ReportAssistant.getInstance().addDynamicConditionExpressionsToSql(rrequest,this.owner.getReportBean(),this.owner,sql,
                        owner.getProvider().getLstMyConditionBeans(rrequest),lstConditionValues,lstConditionTypes);
            }
            throw new WabacusRuntimeException("报表"+this.owner.getReportBean().getPath()+"中ConditionBeanInSqlBean的conditionname属性为空");
        }
        ConditionBean cbeanRefered=owner.getReportBean().getSbean().getConditionBeanByName(conname);//取到被此条件引用的<condition/>
        String conditionvalue=cbeanRefered.getDynamicConditionvalueForSql(rrequest,-1);
        if(!conditionvalue.equals("")||(conditionname.startsWith("#")&&conditionname.endsWith("#")))
        {
            if(conditionname.startsWith("{#")&&conditionname.endsWith("#}"))
            {
                return Tools.replaceAll(sql,this.placeholder,conditionvalue);
            }
            return Tools.replaceAll(sql,this.placeholder,conditionExpression.getRuntimeConditionExpressionValue(cbeanRefered,conditionvalue,
                    lstConditionValues,lstConditionTypes));
        }else
        {
            return ReportAssistant.getInstance().replaceSQLConditionPlaceHolderByRealValue(this.owner.getReportBean(),sql,this.placeholder,null);
        }
    }

    public ConditionInSqlBean clone() 
    {
        try
        {
            ConditionInSqlBean newBean=(ConditionInSqlBean)super.clone();
            if(this.conditionExpression!=null)
            {
                newBean.setConditionExpression((ConditionExpressionBean)conditionExpression.clone());
            }
            return newBean;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone ConditionInSqlBean对象失败",e);
        }
    }
}

