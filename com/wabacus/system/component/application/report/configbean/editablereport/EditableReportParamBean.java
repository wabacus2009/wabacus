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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.system.datatype.IntType;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.util.Tools;
import com.wabacus.util.UUIDGenerator;

public class EditableReportParamBean
{
    private String paramname;

    private String defaultvalue;

    private boolean hasLeftPercent;

    private boolean hasRightPercent;

    private String placeholder;//对于采用statement方式执行的SQL语句，这里存放本参数在SQL语句中对应的占位符，以方便运行时替换
    
    private Object owner;

    public String getParamname()
    {
        return paramname;
    }

    public void setParamname(String paramname)
    {
        this.paramname=paramname;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public boolean isHasLeftPercent()
    {
        return hasLeftPercent;
    }

    public void setHasLeftPercent(boolean hasLeftPercent)
    {
        this.hasLeftPercent=hasLeftPercent;
    }

    public boolean isHasRightPercent()
    {
        return hasRightPercent;
    }

    public void setHasRightPercent(boolean hasRightPercent)
    {
        this.hasRightPercent=hasRightPercent;
    }

    public String getPlaceholder()
    {
        return placeholder;
    }

    public void setPlaceholder(String placeholder)
    {
        this.placeholder=placeholder;
    }

    public Object getOwner()
    {
        return owner;
    }

    public void setOwner(Object owner)
    {
        this.owner=owner;
    }

    public ColBean getColbeanOwner()
    {
        if(!(this.owner instanceof ColBean))
        {
            return null;
        }
        return (ColBean)this.owner;
    }

    public IDataType getDataTypeObj()
    {
        IDataType typeObj=null;
        if(this.owner instanceof EditableReportExternalValueBean)
        {
            typeObj=((EditableReportExternalValueBean)this.owner).getTypeObj();
        }else if(this.owner instanceof ColBean)
        {
            typeObj=((ColBean)this.owner).getDatatypeObj();
        }else if(Tools.isDefineKey("increment",this.paramname))
        {
            typeObj=new IntType();
        }
        if(typeObj==null)
        {
            typeObj=new VarcharType();
        }
        return typeObj;
    }

    public String getRuntimeParamValue(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,String datasource,boolean isAutoReportdata)
    {
        String paramvalue=null;
        if(this.getOwner() instanceof EditableReportExternalValueBean)
        {
            /**paramvalue=((EditableReportExternalValueBean)paramBean.getOwner()).getValue();
             if(Tools.isDefineKey("#",paramvalue))
            {//当前变量是引用绑定保存的其它报表的<params/>中定义的某个变量值
                paramvalue=getReferedOtherExternalValue(rbean,rrequest,paramBean,paramvalue);
            }else if(Tools.isDefineKey("@",paramvalue))
            {
                paramvalue=getExternalValueOfReferedCol(rbean,rrequest,paramBean,paramvalue);
            }else
            {*/
            paramvalue=this.getRealParamValue(mParamValues.get(this.paramname),rrequest,rbean);
            //}
        }else if(this.getOwner() instanceof ColBean)
        {
            paramvalue=EditableReportAssistant.getInstance().getColParamValue(rrequest,rbean,mRowData,this.paramname);
            paramvalue=this.getRealParamValue(paramvalue,rrequest,rbean);
        }else if(Tools.isDefineKey("@",this.paramname)&&!isAutoReportdata)
        {//当前是配置在<button/>中的更新语句，且此<button/>不是自动从报表中获取保存数据，而是用户在客户端传入的数据
            paramvalue=mRowData.get(Tools.getRealKeyByDefine("@",this.getParamname()));
        }else if("uuid{}".equals(this.getParamname()))
        {
            paramvalue=UUIDGenerator.generateID();
        }else if(Tools.isDefineKey("increment",this.getParamname()))
        {
            paramvalue=EditableReportAssistant.getInstance().getAutoIncrementIdValue(rrequest,rbean,datasource,this.paramname);
        }else if(Tools.isDefineKey("!",this.getParamname()))
        {
            String customizeParamName=Tools.getRealKeyByDefine("!",this.paramname);
            Map<String,String> mCustomizedValues=rrequest.getMCustomizeEditData(rbean);
            if(mCustomizedValues==null||!mCustomizedValues.containsKey(customizeParamName))
            {
                paramvalue=null;
            }else
            {
                paramvalue=mCustomizedValues.get(customizeParamName);
            }
        }else if(WabacusAssistant.getInstance().isGetRequestContextValue(this.paramname))
        {
            paramvalue=WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,this.paramname,null);  
        }
        return paramvalue;
    }
    
    public String getRealParamValue(String value,ReportRequest rrequest,ReportBean rbean)
    {
        if(value==null||value.trim().equals(""))
        {
            value=ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
        }
        if(getDataTypeObj() instanceof VarcharType)
        {
            if(this.hasLeftPercent)
            {
                if(value==null) value="";
                value="%"+value;
            }
            if(this.hasRightPercent)
            {
                if(value==null) value="";
                value=value+"%";
            }
        }
        return value;
    }
}
