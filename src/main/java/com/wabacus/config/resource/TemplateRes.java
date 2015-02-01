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

import com.wabacus.config.template.TemplateParser;
import com.wabacus.exception.WabacusConfigLoadingException;

public class TemplateRes extends AbsResource
{
    public Object getValue(Element itemElement)
    {
        if(itemElement==null)
        {
            throw new WabacusConfigLoadingException("加载模板类型资源项失败");
        }
        String value=itemElement.getText();
        value=value==null?"":value.trim();
        return TemplateParser.parseTemplateByContent(value);
    }
}

