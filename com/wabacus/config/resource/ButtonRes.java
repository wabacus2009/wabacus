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
package com.wabacus.config.resource;

import org.dom4j.Element;

import com.wabacus.config.xml.XmlAssistant;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.util.Tools;

public class ButtonRes extends AbsResource
{
    public Object getValue(Element itemElement)
    {
        if(itemElement==null) return null;
        String key=itemElement.attributeValue("key");
        if(key==null||key.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("配置的按钮资源项没有配置key属性");
        }
        Element eleButton=itemElement.element("button");
        if(eleButton==null) return null;
        String extendsParent=eleButton.attributeValue("extends");
        if(extendsParent!=null&&!extendsParent.trim().equals(""))
        {
            if(!Tools.isDefineKey("$",extendsParent))
            {
                throw new WabacusConfigLoadingException("加载按钮资源项"+key+"下配置的按钮失败，被继承的父按钮对应的KEY："+extendsParent+"不是合法的资源项KEY，必须是${key}格式");
            }
            String parentKey=Tools.getRealKeyByDefine("$",extendsParent).trim();
            if(parentKey.equals(""))
            {
                throw new WabacusConfigLoadingException("加载按钮资源项"+key+"下配置的按钮失败，被继承的父按钮对应的KEY："+extendsParent+"的真正key为空");
            }
            if(parentKey.equals(key.trim()))
            {
                throw new WabacusConfigLoadingException("加载按钮资源项"+key+"下配置的按钮失败，不能自己继承自己");
            }
        }
        return XmlAssistant.getInstance().parseXmlValueToXmlBean(eleButton);
    }
}
