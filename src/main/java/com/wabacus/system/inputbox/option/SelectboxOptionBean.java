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
package com.wabacus.system.inputbox.option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.util.RegexTools;

public class SelectboxOptionBean extends AbsOptionBean
{
    private String label="";

    private String value="";

    private String[] type;

    public SelectboxOptionBean(AbsInputBox ownerInputboxObj)
    {
        super(ownerInputboxObj);
    }
    
    public void setLabel(String label)
    {
        this.label=label;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public String getLabel()
    {
        return this.label;
    }

    public String getValue()
    {
        return this.value;
    }

    public String[] getType()
    {
        return type;
    }

    public void setType(String[] type)
    {
        this.type=type;
    }

    public List<Map<String,String>> getLstRuntimeOptions(ReportRequest rrequest,Map<String,String> mParentInputboxValues)
    {
        List<Map<String,String>> lstResults=null;
        if(this.datasetProvider==null)
        {//没有指定动态获取选项数据的数据源对象，则说明此<option/>就是一个常量选项的配置
            String name_temp=rrequest.getI18NStringValue(this.label);
            String value_temp=this.value;
            if(((AbsSelectBox)this.ownerInputboxObj).isDependsOtherInputbox()&&this.type!=null&&this.type.length>0)
            {
                if(this.type.length==1&&(this.type[0].equals("%false-false%")||this.type[0].equals("%true-true%")))
                {
                    return null;
                }
                if(mParentInputboxValues!=null&&mParentInputboxValues.size()>1)
                {
                    throw new WabacusRuntimeException("显示报表"+this.ownerInputboxObj.getOwner().getReportBean().getPath()+"的选择框"
                            +this.getOwnerInputboxObj().getOwner().getInputBoxId()+"失败，此选择框不是从数据库中获取选项数据，不能同时依赖多个父选择框");
                }
                String parentVal=null;
                if(mParentInputboxValues!=null&&mParentInputboxValues.size()>0) mParentInputboxValues.entrySet().iterator().next().getValue();
                if(!isMatch(parentVal,((AbsSelectBox)this.ownerInputboxObj).isRegex())&&!"[%ALL%]".equals(parentVal)) return null;//不匹配父选择框的数据或不是取所有数据
            }
            Map<String,String> mOptionTmp=new HashMap<String,String>();
            mOptionTmp.put("label",name_temp);
            mOptionTmp.put("value",value_temp);
            lstResults=new ArrayList<Map<String,String>>();
            lstResults.add(mOptionTmp);
        }else
        {
            lstResults=this.datasetProvider.getLstSelectBoxOptions(rrequest,mParentInputboxValues);
        }
        return lstResults;
    }

    private boolean isMatch(String parentValue,boolean isRegex)
    {
        if(this.type==null||this.type.length==0)
        {
            return true;
        }
        if(this.type.length==1&&(this.type[0].equals("%true-true%"))||this.type[0].equals("%false-false%")) return false;
        if(parentValue==null) return false;
        for(int i=0;i<type.length;i++)
        {
            if(!isRegex&&parentValue.equals(type[i])) return true;
            if(isRegex&&RegexTools.isMatch(parentValue,type[i])) return true;
        }
        return false;
    }    
}
