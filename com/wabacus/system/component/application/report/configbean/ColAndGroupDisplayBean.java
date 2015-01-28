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
package com.wabacus.system.component.application.report.configbean;

import java.util.List;
import java.util.Map;


public class ColAndGroupDisplayBean
{
    private String id;//<col/>或<group/>的id
    
    private boolean checked;
    
    private String childIds="";
    
    private String parentGroupId="";
    
    private boolean isAlways;
    
    private String title;//列标题
    
    private int layer=0;

    private boolean isControlCol;//如果当前是存放<col/>的显示信息，用于判断当前<col/>是否是控制列，控制列在数据导出时不能显示出来
    
    private boolean isNonFixedCol;
    
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id=id;
    }

    public boolean isControlCol()
    {
        return isControlCol;
    }

    public void setControlCol(boolean isControlCol)
    {
        this.isControlCol=isControlCol;
    }

    public boolean isChecked()
    {
        return checked;
    }

    public void setChecked(boolean checked)
    {
        this.checked=checked;
    }

    public String getChildIds()
    {
        return childIds;
    }

    public void setChildIds(String childIds)
    {
        this.childIds=childIds;
    }

    public String getParentGroupId()
    {
        return parentGroupId;
    }

    public void setParentGroupId(String parentGroupId)
    {
        this.parentGroupId=parentGroupId;
    }

    public boolean isAlways()
    {
        return isAlways;
    }

    public void setAlways(boolean isAlways)
    {
        this.isAlways=isAlways;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title=title;
    }

    public int getLayer()
    {
        return layer;
    }

    public void setLayer(int layer)
    {
        this.layer=layer;
    }

    public boolean isNonFixedCol()
    {
        return isNonFixedCol;
    }

    public void setNonFixedCol(boolean isNonFixedCol)
    {
        this.isNonFixedCol=isNonFixedCol;
    }
}

