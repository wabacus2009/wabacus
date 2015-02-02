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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;

public class ConditionValueSelectItemBean extends ConditionSelectItemBean
{
    private List<ConditionSelectItemBean> lstColumnsBean;//如果此<value/>下面配置的是<column/>，则存放所有<column/>配置，此时它的conditionExpression为null
    
    private Map<String,ConditionSelectItemBean> mColumnsBean;
    
    public ConditionValueSelectItemBean(ConditionBean cbean)
    {
        super(cbean);
    }
    
    public List<ConditionSelectItemBean> getLstColumnsBean()
    {
        return lstColumnsBean;
    }

    public void setLstColumnsBean(List<ConditionSelectItemBean> lstColumnsBean)
    {
        this.lstColumnsBean=lstColumnsBean;
    }

    public ConditionSelectItemBean getColumnBeanById(String columnid)
    {
        if(this.lstColumnsBean==null||this.lstColumnsBean.size()==0) return null;
        if(columnid==null||columnid.trim().equals("")) return this.lstColumnsBean.get(0);
        if(this.mColumnsBean==null)
        {
            Map<String,ConditionSelectItemBean> mColumnsBeanTmp=new HashMap<String,ConditionSelectItemBean>();
            for(ConditionSelectItemBean ccbeanTmp:this.lstColumnsBean)
            {
                mColumnsBeanTmp.put(ccbeanTmp.getId(),ccbeanTmp);
            }
            this.mColumnsBean=mColumnsBeanTmp;
        }
        return this.mColumnsBean.get(columnid);
    }
    
    void loadConfig(XmlElementBean eleSelectItemBean)
    {
        super.loadConfig(eleSelectItemBean);
        /*if(((SqlBean)this.cbean.getParent()).isStoreProcedure()) return;*///存储过程的<values/>下面的<value/>不用配置条件表达式，因为它们的条件表达式在存储过程中
        Object valueObj=ComponentConfigLoadManager.loadConditionValueConfig(cbean,eleSelectItemBean);
        if(valueObj instanceof ConditionExpressionBean)
        {//<value/>下面直接是条件表达式
            this.setConditionExpression((ConditionExpressionBean)valueObj);
        }else
        {//<value/>下面是多个<column/>
            this.setLstColumnsBean((List<ConditionSelectItemBean>)valueObj);
        }
    }

    public ConditionSelectItemBean clone(ConditionBean cbeanNew)
    {
        ConditionValueSelectItemBean cvbeanNew=(ConditionValueSelectItemBean)super.clone(cbeanNew);
        if(lstColumnsBean!=null)
        {
            List<ConditionSelectItemBean> lstColumnsBeanNew=new ArrayList<ConditionSelectItemBean>();
            for(ConditionSelectItemBean ccbeanTmp:lstColumnsBean)
            {
                lstColumnsBeanNew.add((ConditionSelectItemBean)ccbeanTmp.clone(cbeanNew));
            }
            cvbeanNew.setLstColumnsBean(lstColumnsBeanNew);
        }
        return cvbeanNew;
    }    
}

