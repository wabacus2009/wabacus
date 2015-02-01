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
package com.wabacus.config.other;

public class JavascriptFileBean implements Comparable<JavascriptFileBean>
{
    private String jsfileurl;

    private int loadorder;

    public JavascriptFileBean(String jsfileurl,int loadorder)
    {
        this.jsfileurl=jsfileurl;
        this.loadorder=loadorder;
    }
    
    public String getJsfileurl()
    {
        return jsfileurl;
    }

    public void setJsfileurl(String jsfileurl)
    {
        this.jsfileurl=jsfileurl;
    }

    public int getLoadorder()
    {
        return loadorder;
    }

    public void setLoadorder(int loadorder)
    {
        this.loadorder=loadorder;
    }

    public int compareTo(JavascriptFileBean otherbean)
    {
        if(otherbean==null) return -1;
        if(this.loadorder>otherbean.getLoadorder()) return -1;
        if(this.loadorder<otherbean.getLoadorder()) return 1;
        return 0;
    }

    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((jsfileurl==null)?0:jsfileurl.hashCode());
        result=prime*result+loadorder;
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final JavascriptFileBean other=(JavascriptFileBean)obj;
        if(jsfileurl==null)
        {
            if(other.jsfileurl!=null) return false;
        }else if(!jsfileurl.equals(other.jsfileurl)) return false;
        if(loadorder!=other.loadorder) return false;
        return true;
    }

}
