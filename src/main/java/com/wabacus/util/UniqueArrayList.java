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
package com.wabacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UniqueArrayList<E> extends ArrayList<E> implements Cloneable
{
    private static final long serialVersionUID=7739843329266051669L;

    public void add(int index,E element)
    {
        if(!super.contains(element)) super.add(index,element);
    }

    public boolean add(E element)
    {
        if(!super.contains(element)) return super.add(element);
        return false;
    }

    public boolean addAll(Collection<? extends E> collection)
    {
        for(E e:collection)
        {
            add(e);
        }
        return true;
    }

    public boolean addAll(int index,Collection<? extends E> collection)
    {
        List<E> cTmp=new ArrayList<E>();
        for(E e:collection)
        {
            if(!super.contains(e))
            {
                cTmp.add(e);
            }
        }
        return super.addAll(index,cTmp);
    }

    public UniqueArrayList<E> clone()
    {
        return (UniqueArrayList<E>)super.clone();
    }
}
