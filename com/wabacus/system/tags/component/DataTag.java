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
package com.wabacus.system.tags.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.assistant.TagAssistant;
import com.wabacus.system.component.application.report.EditableDetailReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsDetailReportType;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.inputbox.CustomizedBox;
import com.wabacus.util.Consts;

public class DataTag extends AbsComponentTag
{
    private static final long serialVersionUID=4778705333034157642L;


    private String showlabel="true"; 

    private String showdata="true";

    private String col;

    private String rowidx;

    private String styleproperty;

    private String top;

    private boolean isShowDetailColInputboxByClientSelf;

    private ColBean colbean;

    private AbsReportType displayReportTypeObj;
    
    public void setShowlabel(String showlabel)
    {
        this.showlabel=showlabel;
    }

    public void setShowdata(String showdata)
    {
        this.showdata=showdata;
    }

    public void setCol(String col)
    {
        this.col=col;
    }

    public void setRowidx(String rowidx)
    {
        this.rowidx=rowidx;
    }

    public void setStyleproperty(String styleproperty)
    {
        this.styleproperty=styleproperty;
    }

    public void setTop(String top)
    {
        this.top=top;
    }

    public int doMyStartTag() throws JspException,IOException
    {
        if(this.displayComponentObj==null) return SKIP_BODY;
        if(!(this.displayComponentObj instanceof AbsReportType))
        {
            throw new WabacusRuntimeException("组件"+this.displayComponentObj.getConfigBean().getPath()+"不是报表，不能调用<wx:data/>显示其数据部分");
        }
        displayReportTypeObj=(AbsReportType)this.displayComponentObj;
        isShowDetailColInputboxByClientSelf=showInputBoxSelf();
        if(isShowDetailColInputboxByClientSelf)
        {//用户自己显示输入框
            println(((AbsDetailReportType)displayComponentObj).showColData(colbean,true,null));
            return EVAL_BODY_INCLUDE;
        }
        return SKIP_BODY;
    }

    public int doMyEndTag() throws JspException,IOException
    {
        if(displayComponentObj==null) return EVAL_PAGE;
        if(isShowDetailColInputboxByClientSelf)
        {
            println("</font>");
        }else
        {
            Map<String,String> mAttributes=new HashMap<String,String>();
            mAttributes.put("showlabel",showlabel);
            mAttributes.put("showdata",showdata);
            mAttributes.put("col",col);
            mAttributes.put("rowidx",rowidx);
            mAttributes.put("styleproperty",styleproperty);
            mAttributes.put("top",top);
            println(TagAssistant.getInstance().getReportDataPartDisplayValue(displayReportTypeObj,mAttributes));
        }
        return EVAL_PAGE;
    }

    private boolean showInputBoxSelf()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return false;
        if(col==null||col.trim().equals("")) return false;
        if(!(displayComponentObj instanceof EditableDetailReportType)) return false;
        if(showdata!=null&&showdata.toLowerCase().trim().equals("false")) return false;
        colbean=displayReportTypeObj.getReportBean().getDbean().getColBeanByColProperty(col);
        if(colbean==null)
        {
            throw new WabacusRuntimeException("用<wx:data/>显示报表"+this.displayComponentObj.getConfigBean().getPath()+"上的列时，没有取到property(或column)属性为"
                    +col+"的<col/>");
        }
        if(((EditableDetailReportType)displayComponentObj).isReadonlyCol(colbean)) return false;
        EditableReportColBean ercbeanTmp=(EditableReportColBean)colbean.getExtendConfigDataForReportType(EditableReportColBean.class);
        return ercbeanTmp!=null&&ercbeanTmp.getInputbox() instanceof CustomizedBox;
    }
}
