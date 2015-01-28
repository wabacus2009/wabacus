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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.wabacus.system.ReportRequest;

public class ValidateTools
{
    public static boolean isNotEmpty(ReportRequest rrequest,String value,Map<String,Object> mValues,List<String> lstErrorMessages)
    {
        if(value==null||value.trim().equals("")) return false;
        return true;
    }

    public static boolean isNumeric(ReportRequest rrequest,String num,Map<String,Object> mValues,List<String> lstErrorMessages)
    {
        if(num==null||num.trim().equals(""))
        {
            return true;
        }
        num=num.trim();
        int i=0;
        if(num.charAt(i)=='.')
        {
            return false;
        }
        boolean flag=false;
        for(;i<num.length();i++)
        {
            if(num.charAt(i)>'9'||num.charAt(i)<'0')
            {
                if(num.charAt(i)=='.')
                {
                    if(flag)
                    {
                        break;
                    }
                    flag=true;
                }else
                {
                    break;
                }
            }
        }
        if(i!=num.length())
        {
            return false;
        }

        return true;

    }

    public static boolean isInteger(ReportRequest rrequest,String str,Map<String,Object> mValues,List<String> lstErrorMessages)
    {
        if(str==null||str.trim().equals(""))
        {
            return true;
        }
        try
        {
            Integer.parseInt(str.trim());
            return true;
        }catch(Exception e)
        {
            return false;
        }

    }

    public static boolean isDate(ReportRequest rrequest,String date,Map<String,Object> mValues,List<String> lstErrorMessages)
    {
        if(date==null||date.trim().equals(""))
        {
            return true;
        }
        String[] str=date.split("-");
        if(str.length!=3||date.endsWith("-"))
        {
            return false;
        }
        try
        {
            int year=Integer.parseInt(str[0]);
            if(year<1900||year>2068)
            {
                return false;
            }
        }catch(Exception e)
        {
            return false;
        }
        try
        {
            int month=Integer.parseInt(str[1]);
            if(month<0||month>12)
            {
                return false;
            }
        }catch(Exception e)
        {
            return false;
        }
        try
        {
            int day=Integer.parseInt(str[2]);
            if(day<0||day>31)
            {
                return false;
            }
        }catch(Exception e)
        {
            return false;
        }
        return isDate(date,"yyyy-MM-dd");
    }

    public static boolean isShortDate(ReportRequest rrequest,String date,Map<String,Object> mValues,List<String> lstErrorMessages)
    {

        if(date==null||date.trim().equals(""))
        {
            return true;
        }
        date=date.trim();
        String[] str=date.split("-");
        if(str.length!=2||date.endsWith("-"))
        {
            return false;
        }
        try
        {
            int year=Integer.parseInt(str[0]);
            if(year<1000||year>10000)
            {
                return false;
            }
        }catch(Exception e)
        {
            return false;
        }
        try
        {
            int month=Integer.parseInt(str[1]);
            if(month<0||month>12)
            {
                return false;
            }
        }catch(Exception e)
        {
            return false;
        }
        return isDate(date,"yyyy-MM");
    }

    public static boolean isDateTime(ReportRequest rrequest,String date,Map<String,Object> mValues,List<String> lstErrorMessages)
    {
        return isDate(date,"yyyy-MM-dd HH:mm:ss");
    }

    private static boolean isDate(String date,String pattern)
    {

        try
        {
            SimpleDateFormat sdformat=new SimpleDateFormat(pattern);
            sdformat.setLenient(false);
            sdformat.parse(date);
        }catch(Exception e)
        {
            return false;
        }

        return true;
    }
}
