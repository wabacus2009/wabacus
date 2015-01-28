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
package com.wabacus.config.typeprompt;

public class TypePromptColBean implements Cloneable
{
    private String label;
    
    private String value;

    private String title;
    
    private int matchmode=0;
    
    private String matchexpression;//如果当前匹配联想数据是从SQL语句中数据库中查询，可以在此属性中指定此联想列的匹配条件表达式，用#data#做为用户输入值的占位符，如果没有指定框架会自动生成
    
    private boolean hidden;

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label=label;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title=title;
    }

    public int getMatchmode()
    {
        return matchmode;
    }

    public void setMatchmode(int matchmode)
    {
        this.matchmode=matchmode;
    }

    public String getMatchexpression()
    {
        return matchexpression;
    }

    public void setMatchexpression(String matchexpression)
    {
        this.matchexpression=matchexpression;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden=hidden;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
