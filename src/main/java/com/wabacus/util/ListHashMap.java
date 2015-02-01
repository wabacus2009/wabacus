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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class ListHashMap<K,V> extends HashMap<K,V> implements Cloneable
{
    private List<K> lstKeys;
    
    private static final long serialVersionUID=-4715541043628653075L;

    public ListHashMap()
    {
        super();
        lstKeys=new ArrayList<K>();
    }
    
    public Set<java.util.Map.Entry<K,V>> entrySet()
    {
        throw new RuntimeException(ListHashMap.class.getName()+"类没有entrySet()方法，请调用其entryList()方法");
    }

    public Set<K> keySet()
    {
        throw new RuntimeException(ListHashMap.class.getName()+"类没有keySet()方法，请调用其keyList()方法");
    }

    
    
    public V put(K k,V v)
    {
        if(lstKeys.contains(k))
        {
            lstKeys.remove(k);
        }
        lstKeys.add(k);
        return super.put(k,v);
    }

    public void putAll(Map<? extends K,? extends V> map)
    {
        for(K key:map.keySet())
        {
            if(lstKeys.contains(key))
            {
                lstKeys.remove(key);
            }
            lstKeys.add(key);
        }
        super.putAll(map);
    }

    public V remove(Object key)
    {
        if(lstKeys.contains(key))
        {
            lstKeys.remove(key);
        }
        return super.remove(key);
    }

    
}

