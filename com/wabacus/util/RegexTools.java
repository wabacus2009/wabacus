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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wabacus.exception.WabacusConfigLoadingException;

public class RegexTools
{
    public static boolean isMatch(String sourceString,String pattern)
    {
        if(sourceString==null||pattern==null) return false;
        Pattern p=Pattern.compile(pattern);
        Matcher m=p.matcher(sourceString);
        return m.matches();
    }
    
    public static Map<String,String> parseXmlTagAttribute(String propString)
    {
        if(propString==null||propString.trim().equals("")) return null;
        Map<String,String> mAttributes=new HashMap<String,String>();
        parseXmlTagAttribute(propString,mAttributes);
        return mAttributes;
    }
    
    public static String parseXmlTagAttribute(String propString,Map<String,String> mAttributes)
    {
        if(propString==null||propString.trim().equals("")) return "";
        //String ps3 = "(<" + ProviderTag + "[^<]+>)(.*|\\s*)</" + ProviderTag + ">";
        //String ps3 = "(<" + ProviderTag + ".+>)(.*|\\s*)</" + ProviderTag + ">";
        return getTagAttributes(propString,"([A-Za-z0-9_]+\\s*=\\s*\"[^\"]*\")","([A-Za-z0-9_]+)\\s*=\\s*\"(.*)\"",mAttributes);
    }

    public static String parseHtmlTagAttribute(String propString,Map<String,String> mAttributes)
    {
        if(propString==null||propString.trim().equals("")) return "";
        propString=Tools.standardHtmlTagProperties(propString);
        String restString=getTagAttributes(propString,"([A-Za-z0-9_]+\\s*=\\s*\"[^\"]*\")","([A-Za-z0-9_]+)\\s*=\\s*\"(.*)\"",mAttributes);
        restString=getTagAttributes(restString,"([A-Za-z0-9_]+\\s*=\\s*'[^']*')","([A-Za-z0-9_]+)\\s*=\\s*'(.*)'",mAttributes);
        return restString.trim();
    }
    
    public static String parseHtmlStyleValue(String style,Map<String,String> mAttributes)
    {
        if(style==null||style.trim().equals("")) return style;
        style=style.trim();
        if(!style.endsWith(";")) style=style+";";
        return getTagAttributes(style,"([A-Za-z0-9_-]+\\s*:\\s*[^;]*;)","([A-Za-z0-9_-]+)\\s*:\\s*(.*);",mAttributes);
    }
    
    public static String parseJsonValue(String jsonstring,Map<String,String> mAttributes)
    {
        if(jsonstring==null||jsonstring.trim().equals("")) return jsonstring;
        jsonstring=Tools.replaceCharacterInQuote(jsonstring.trim(),',',"$_COMMA_SIGN_$",true);
        if(jsonstring.startsWith("{")&&jsonstring.endsWith("}")) jsonstring=jsonstring.substring(1,jsonstring.length()-1);
        if(!jsonstring.endsWith(",")) jsonstring=jsonstring+",";
        String reststr=getTagAttributes(jsonstring,"([A-Za-z0-9_-]+\\s*:\\s*[^,]*,)","([A-Za-z0-9_-]+)\\s*:\\s*(.*),",mAttributes);
        Map<String,String> mTemp=new HashMap<String,String>();
        mTemp.putAll(mAttributes);
        mAttributes.clear();
        for(Entry<String,String> entryTmp:mTemp.entrySet())
        {
            mAttributes.put(entryTmp.getKey(),Tools.replaceAll(entryTmp.getValue(),"$_COMMA_SIGN_$",","));
        }
        return Tools.replaceAll(reststr,"$_COMMA_SIGN_$",",");
    }
    
    public static String getTagAttributes(String propString,String ps1,String ps2,Map<String,String> mAttributes)
    {
        if(propString==null||propString.trim().equals("")) return "";
        StringBuffer resultBuf=new StringBuffer();
        try
        {
            Pattern pattern1=Pattern.compile(ps1);
            Pattern pattern2=Pattern.compile(ps2);
            Matcher matcher1=pattern1.matcher(propString);
            int preEnd=0;
            //对每对参数取出参数名和参数值 
            while(matcher1.find())
            {
                int start=matcher1.start();
                if(start>preEnd)
                {
                    resultBuf.append(propString.substring(preEnd,start));
                }
                preEnd=matcher1.end();
                Matcher matcher2=pattern2.matcher(matcher1.group());
                while(matcher2.find())
                {
                    mAttributes.put(matcher2.group(1).trim(),matcher2.group(2));
                }
            }
            if(preEnd<propString.length())
            {
                resultBuf.append(propString.substring(preEnd,propString.length()));
            }
        }catch(Exception ex)
        {
            throw new WabacusConfigLoadingException("解析属性字符串"+propString+"失败");
        }
        return resultBuf.toString();
    }
    
    public static String replaceAll(String src,String pattern,boolean sensitive,String destvalue)
    {
        if(src==null||src.equals("")) return src;
        Pattern p=null;
        if(sensitive)
        {
            p=Pattern.compile(pattern);
        }else
        {
            p=Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);
        }
        Matcher m=p.matcher(src);
        src=m.replaceAll(destvalue);
        return src;
    }
    
    public static List<Map<String,Object>> getMatchObjectArray(String src,String pattern,
            boolean sensitive)
    {
        if(src==null||src.equals("")) return null;
        Pattern p=null;
        if(sensitive)
        {
            p=Pattern.compile(pattern);
        }else
        {
            p=Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);
        }
        List<Map<String,Object>> lstResults=new ArrayList<Map<String,Object>>();
        Matcher m=p.matcher(src);
        Map<String,Object> mTmp;
        while(m.find())
        {
            mTmp=new HashMap<String,Object>();
            mTmp.put("value",m.group());
            mTmp.put("startindex",m.start());
            mTmp.put("endindex",m.end());
            lstResults.add(mTmp);
        }
        return lstResults;
    }
}

