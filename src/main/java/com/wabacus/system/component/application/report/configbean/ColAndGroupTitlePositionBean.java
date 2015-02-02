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

public class ColAndGroupTitlePositionBean implements Cloneable
{
    private int startcolindex=0;

    private int startrowindex=0;

    private int rowspan=1;

    private int colspan=1;
    
    private int layer=0;//当前列标题所在的层级，最上一层为0
    
    private int displaymode;
    
    public int getStartcolindex()
    {
        return startcolindex;
    }

    public void setStartcolindex(int startcolindex)
    {
        this.startcolindex=startcolindex;
    }

    public int getStartrowindex()
    {
        return startrowindex;
    }

    public void setStartrowindex(int startrowindex)
    {
        this.startrowindex=startrowindex;
    }

    public int getRowspan()
    {
        return rowspan;
    }

    public void setRowspan(int rowspan)
    {
        this.rowspan=rowspan;
    }

    public int getColspan()
    {
        return colspan;
    }

    public void setColspan(int colspan)
    {
        this.colspan=colspan;
    }

    public int getLayer()
    {
        return layer;
    }

    public void setLayer(int layer)
    {
        this.layer=layer;
    }

    public int getDisplaymode()
    {
        return displaymode;
    }

    public void setDisplaymode(int displaymode)
    {
        this.displaymode=displaymode;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
