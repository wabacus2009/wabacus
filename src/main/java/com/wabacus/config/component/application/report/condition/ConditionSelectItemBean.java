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

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;

public class ConditionSelectItemBean implements Cloneable
{
    protected String id;

    protected String label;

    protected ConditionExpressionBean conditionExpression;

    protected ConditionBean cbean;
    
    public ConditionSelectItemBean(ConditionBean cbean)
    {
        this.cbean=cbean;
    }
    
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id=id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label=label;
    }

    public ConditionBean getCbean()
    {
        return cbean;
    }

    public void setCbean(ConditionBean cbean)
    {
        this.cbean=cbean;
    }

    public ConditionExpressionBean getConditionExpression()
    {
        return conditionExpression;
    }

    public void setConditionExpression(ConditionExpressionBean conditionExpression)
    {
        this.conditionExpression=conditionExpression;
    }

    void loadConfig(XmlElementBean eleSelectItemBean)
    {
        String selectItemId=eleSelectItemBean.attributeValue("id");
        if(selectItemId==null||selectItemId.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"的<condition/>的<"+eleSelectItemBean.getName()+"/>失败，必须配置id属性");
        }
        selectItemId=selectItemId.trim();
        this.setId(selectItemId);
        String labelTmp=eleSelectItemBean.attributeValue("label");
        labelTmp=labelTmp==null?"":Config.getInstance().getResourceString(null,cbean.getPageBean(),labelTmp,true);
        if(labelTmp.equals("")) labelTmp=selectItemId;
        this.setLabel(labelTmp);
    }
    
    public ConditionSelectItemBean clone(ConditionBean cbeanNew)
    {
        try
        {
            ConditionSelectItemBean csibeanNew=(ConditionSelectItemBean)super.clone();
            csibeanNew.setCbean(cbeanNew);
            if(conditionExpression!=null)
            {
                csibeanNew.setConditionExpression(conditionExpression.clone());
            }
            return csibeanNew;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone ConditionSelectItemBean对象失败",e);
        }
    }
}

