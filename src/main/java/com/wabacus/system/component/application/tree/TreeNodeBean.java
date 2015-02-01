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
package com.wabacus.system.component.application.tree;

import java.util.ArrayList;
import java.util.List;

public class TreeNodeBean
{
    private String id;

    private int parentidx;

    private List<Integer> lstChildIdxs;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id=id;
    }

    public int getParentidx()
    {
        return parentidx;
    }

    public void setParentidx(int parentidx)
    {
        this.parentidx=parentidx;
    }

    public List<Integer> getLstChildIdxs()
    {
        return lstChildIdxs;
    }

    public void setLstChildIdxs(List<Integer> lstChildIdxs)
    {
        this.lstChildIdxs=lstChildIdxs;
    }

    public void addChildIdx(int childidx)
    {
        if(this.lstChildIdxs==null) this.lstChildIdxs=new ArrayList<Integer>();
        if(!this.lstChildIdxs.contains(childidx)) this.lstChildIdxs.add(childidx);
    }

}
