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

public class ListReportColPositionBean
{
    private String firstColid;
    
    private String lastColid;
    
    private int totalColCount;

    public String getFirstColid()
    {
        return firstColid;
    }

    public void setFirstColid(String firstColid)
    {
        this.firstColid=firstColid;
    }

    public String getLastColid()
    {
        return lastColid;
    }

    public void setLastColid(String lastColid)
    {
        this.lastColid=lastColid;
    }

    public int getTotalColCount()
    {
        return totalColCount;
    }

    public void setTotalColCount(int totalColCount)
    {
        this.totalColCount=totalColCount;
    }        
}

