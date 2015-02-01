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
package com.wabacus.config.print;


public class PrintTemplateElementBean
{
    public final static int ELEMENT_TYPE_STATICTPL=1;
    
    public final static int ELEMENT_TYPE_DYNTPL=2;
    
    public final static int ELEMENT_TYPE_APPLICATIONID=3;//如果<print/>属于某个容器，且又没有在<print/>标签内容中指定有效打印代码，则默认将其include中所有组件依次打印，且每个应用放在一个单独的<subpage/>中，因此此时在这里存放本子页对应的应用ID
    
    public final static int ELEMENT_TYPE_APPLICATION=4;
    
    public final static int ELEMENT_TYPE_OTHER=5;//其它动态内容，比如从request/session中取打印数据
    
    private String placeholder;
    
    private int type;//打印内容的类型
    
    private Object valueObj;

    public PrintTemplateElementBean(int placeholderindex)
    {
        this.placeholder="WX_PRINT_CONTENT_PLACEHOLDER_"+placeholderindex;
    }
    
    public String getPlaceholder()
    {
        return placeholder;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type=type;
    }

    public Object getValueObj()
    {
        return valueObj;
    }

    public void setValueObj(Object valueObj)
    {
        this.valueObj=valueObj;
    }
}
