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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.xml.XmlElementBean;

public class PasswordBox extends TextBox
{
    private static Log log=LogFactory.getLog(PasswordBox.class);
    
    private int encodelength;
    
    public PasswordBox(String typename)
    {
        super(typename);
    }

    public int getEncodelength()
    {
        return encodelength;
    }

    protected String getTextBoxType()
    {
        return "password";
    }
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        if(eleInputboxBean==null) return;
        String strencodelength=eleInputboxBean.attributeValue("encodelength");
        if(strencodelength!=null)
        {
            strencodelength=strencodelength.trim();
            if(strencodelength.equals(""))
            {
                this.encodelength=0;
            }else
            {
                try
                {
                    this.encodelength=Integer.parseInt(strencodelength);
                }catch(NumberFormatException e)
                {
                    log.warn("为密码框配置的encodelength属性值："+strencodelength+"不是有效数字");
                    this.encodelength=0;
                }
                if(this.encodelength<5)
                {
                    log.warn("为密码框配置的encodelength属性值："+strencodelength+"小于5，将采用5做为编码长度");
                    this.encodelength=5;
                }
            }
        }
    }

    public void doPostLoad()
    {
        this.setTypePromptBean(null);
        super.doPostLoad();
    }
    
    protected void initDisplayMode()
    {
        this.displayOnClick=false;
    }
}
