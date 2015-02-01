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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.util.Tools;

public class EditableReportSecretColValueBean implements Serializable
{
    private static final long serialVersionUID=-9046271193435621058L;
    
    private Map<String,String> mSecretColValues;

    public boolean containsColkey(String colkey)
    {
        if(colkey==null||colkey.trim().equals("")) return false;
        if(mSecretColValues==null) return false;
        return mSecretColValues.containsKey(colkey);
    }
    
    public String getParamValue(String colkey)
    {
        if(mSecretColValues==null||mSecretColValues.size()==0) return null;
        return mSecretColValues.get(colkey);
    }

    public void addParamValue(String colkey,String colvalue)
    {
        if(mSecretColValues==null) mSecretColValues=new HashMap<String,String>();
        mSecretColValues.put(colkey,colvalue);
    }

    public boolean isEmpty()
    {
        return mSecretColValues==null||mSecretColValues.size()==0;
    }

    public void storeToSession(ReportRequest rrequest,ReportBean rbean)
    {
        if(rrequest.getRequest()==null) return;
        if(this.isEmpty())
        {
            rrequest.getRequest().getSession().removeAttribute(rbean.getGuid()+"_SECRET_COLVALUES");
        }else
        {
            rrequest.getRequest().getSession().setAttribute(rbean.getGuid()+"_SECRET_COLVALUES",this);
        }
    }

    public String getUniqueEncodeString(int length)
    {
        if(length<=0) return "";
        String randomStr=Tools.getRandomString(length);
        int i=0;
        while(this.containsColkey(randomStr))
        {
            randomStr=Tools.getRandomString(length);
            if(i++>=100)
            {
                throw new WabacusRuntimeException("生成长度为"+length+"位的随机字符串失败，这个长度太短，已经出现了重复字符串，建议加大要生成的随机字符串长度");
            }
        }
        return randomStr;
    }
    
    public static EditableReportSecretColValueBean loadFromSession(ReportRequest rrequest,ReportBean rbean)
    {
        if(rrequest.getRequest()==null) return null;
        return (EditableReportSecretColValueBean)rrequest.getRequest().getSession().getAttribute(rbean.getGuid()+"_SECRET_COLVALUES");
    }
}
