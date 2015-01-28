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
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Tools;

public class ConditionExpressionBean implements Cloneable
{
    private final static String[][] CONDITION_PLACEHOLDER= { { "'%#data#%'", "%#data#%" },
            { "%#data#%", "%#data#%" }, { "'%#data#'", "%#data#" }, { "%#data#", "%#data#" },
            { "'#data#%'", "#data#%" }, { "#data#%", "#data#%" }, { "'#data#'", null },
            { "#data#", null } };
    
    private String value;

    private int index_count=1;

    private String pattern="";
    
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public int getIndex_count()
    {
        return index_count;
    }

    public void setIndex_count(int index_count)
    {
        this.index_count=index_count;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern=pattern;
    }

    public void parseConditionExpression()
    {
        if(value==null||value.trim().equals("")) return;
        int i=0;
        for(;i<CONDITION_PLACEHOLDER.length;i++)
        {
            if(value.indexOf(CONDITION_PLACEHOLDER[i][0])>=0)
            {
                break;
            }
        }
        if(i==CONDITION_PLACEHOLDER.length)
        {
            this.index_count=0;
            this.pattern=null;
            return;
        }
        int count=0;
        String conditionExpressionTmp=this.value;
        int idxTemp=conditionExpressionTmp.indexOf(CONDITION_PLACEHOLDER[i][0]);
        while(idxTemp>=0)
        {
            count++;
            conditionExpressionTmp=conditionExpressionTmp.substring(idxTemp+CONDITION_PLACEHOLDER[i][0].length());
            idxTemp=conditionExpressionTmp.indexOf(CONDITION_PLACEHOLDER[i][0]);
        }
        this.index_count=count;
        this.pattern=CONDITION_PLACEHOLDER[i][1];
        conditionExpressionTmp=this.value;
        this.value=Tools.replaceAll(this.value,CONDITION_PLACEHOLDER[i][0],"?");
        for(int j=0;j<CONDITION_PLACEHOLDER.length;j++)
        {
            if(this.value.indexOf(CONDITION_PLACEHOLDER[j][0])>=0)
            {
                throw new WabacusConfigLoadingException("在同一查询条件中，只能出现同一种类型的占位符(可以根据需要出现多次)，而"
                        +conditionExpressionTmp+"中含有多种占位符，不合法");
            }
        }
    }
    
    public String getRuntimeConditionExpressionValue(ConditionBean cbean,String conditionvalue,List<String> lstConditionValues,
            List<IDataType> lstConditionTypes)
    {
        if(lstConditionValues!=null&&lstConditionTypes!=null)
        {//当前是采用preparedstatement方式执行sql语句
            if(pattern!=null&&pattern.indexOf("#data#")>=0) conditionvalue=Tools.replaceAll(pattern,"#data#",conditionvalue);
            for(int i=0;i<this.index_count;i++)
            {
                lstConditionValues.add(conditionvalue);
                lstConditionTypes.add(cbean.getDatatypeObj());
            }
            return this.value;
        }else
        {
            if(!cbean.isKeepkeywords())
            {
                conditionvalue=Tools.removeSQLKeyword(conditionvalue);
            }
            return Tools.replaceAll(this.value,"#data#",conditionvalue);
        }
    }
    
    public ConditionExpressionBean clone()
    {
        try
        {
            return (ConditionExpressionBean)super.clone();
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone ConditionExpressionBean对象失败",e);
        }
    }
}
