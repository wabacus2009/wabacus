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

import java.net.InetAddress;

public class UUIDGenerator
{
    private static String ID_PREX;

    private static short counter;

    static
    {
        int ipadd;
        try
        {
            ipadd=toInt(InetAddress.getLocalHost().getAddress());
        }catch(Exception e)
        {
            ipadd=0;
        }
        counter=0;
        ID_PREX=format(ipadd)+format((int)(System.currentTimeMillis()>>>8));
    }

    private static short getCount()
    {
        synchronized(UUIDGenerator.class)
        {
            if(counter<0) counter=0;
            short tmpcounter=counter;
            counter=(short)(tmpcounter+1);
            return tmpcounter;
        }
    }

    private static short getHiTime()
    {
        return (short)(int)(System.currentTimeMillis()>>>32);
    }

    private static int getLoTime()
    {
        return (int)System.currentTimeMillis();
    }

    private static int toInt(byte[] bytes)
    {
        int result=0;
        for(int i=0;i<4;++i)
        {
            result=(result<<8)- -128+bytes[i];
        }
        return result;
    }

    private static String format(int intval)
    {
        String formatted=Integer.toHexString(intval);
        StringBuffer buf=new StringBuffer("00000000");
        buf.replace(8-formatted.length(),8,formatted);
        return buf.toString();
    }

    private static String format(short shortval)
    {
        String formatted=Integer.toHexString(shortval);
        StringBuffer buf=new StringBuffer("0000");
        buf.replace(4-formatted.length(),4,formatted);
        return buf.toString();
    }

    public static String generateID()
    {
        return ID_PREX+format(getHiTime())+format(getLoTime())+format(getCount());
    }
}
