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
package com.wabacus.system.inputbox.validate;

import java.util.Map.Entry;

import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.util.Tools;

public class JavascriptValidateBean extends AbsValidateBean
{
    public JavascriptValidateBean(AbsInputBox owner)
    {
        super(owner);
    }
    
    protected String getJsonStringQuote()
    {
        return "'";//在js中，json字符串必须以单引号括住，不能用双引号，否则没办法显示到前面metadata的<span/>中
    }
    
    public String displayValidateInfoOnMetadata(ReportRequest rrequest,String inputboxid)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(" validateMethod_"+inputboxid+"=\"{method:validate_"+inputboxid+"}\"");
        if(validatetype!=null) resultBuf.append(" validateType_"+inputboxid+"=\""+validatetype+"\"");
        if(mParamBeans!=null&&mParamBeans.size()>0)
        {
            resultBuf.append(" jsValidateParamsObj_"+inputboxid+"=\"{");
            ParamBean paramBeanTmp;
            for(Entry<String,ParamBean> paramEntryTmp:mParamBeans.entrySet())
            {
                paramBeanTmp=paramEntryTmp.getValue();
                resultBuf.append(paramEntryTmp.getKey()).append(":");
                if(paramBeanTmp.isStringType()) resultBuf.append("'");
                resultBuf.append(Tools.jsParamEncode(paramBeanTmp.getRealParamValue(rrequest)));
                if(paramBeanTmp.isStringType()) resultBuf.append("'");
                resultBuf.append(",");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("}\"");
        }
        return resultBuf.toString();
    }
}
