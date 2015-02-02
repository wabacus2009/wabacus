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
package com.wabacus.system.inputbox;

import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.system.ReportRequest;

public class CustomizedBox extends AbsInputBox
{
    public CustomizedBox(String typename)
    {
        super(typename);
    }

    protected String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly)
    {
        return "";
    }

    public String filledInContainer()
    {
        return "";
    }

    protected String getDefaultStylePropertyForDisplayMode2()
    {
        return "";
    }

    public String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,boolean isReadonly)
    {
        return "";
    }

    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        loadValidateConfig(eleInputboxBean);
    }

    public void doPostLoad()
    {
        this.styleproperty="";
        processJsValidate();
        processServerValidate();
    }

}
