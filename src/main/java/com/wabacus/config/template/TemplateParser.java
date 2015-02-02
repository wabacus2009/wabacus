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
package com.wabacus.config.template;

import java.util.ArrayList;
import java.util.List;

import com.wabacus.config.template.tags.AbsTagInTemplate;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.util.RegexTools;

public class TemplateParser
{
    public static TemplateBean parseTemplateByPath(String templatepath)
    {
        if(templatepath==null||templatepath.trim().equals("")) return null;
        String templateContent=WabacusAssistant.getInstance().readFileContentByPath(templatepath.trim());
        return parseTemplateByContent(templateContent);
    }

    public static TemplateBean parseTemplateByContent(String content)
    {
        if(content==null) return null;
        content=WabacusAssistant.getInstance().replaceSystemPlaceHolder(content);
        TemplateBean tplbean=new TemplateBean();
        tplbean.setContent(content);
        tplbean.setLstTagChildren(parseHtmlContent(null,content));
        if(tplbean.getLstTagChildren()!=null&&tplbean.getLstTagChildren().size()>0)
        {
            for(AbsTagInTemplate tagbeanTmp:tplbean.getLstTagChildren())
            {
                tagbeanTmp.parseTagValue();
            }
        }
        return tplbean;
    }

    public static List<AbsTagInTemplate> parseHtmlContent(AbsTagInTemplate parentTag,String content)
    {
        if(content==null||content.trim().equals("")) return null;
        int length=content.length();
        boolean isInTagStartPart=false;
        StringBuffer fullContentBuf=new StringBuffer();
        StringBuffer propBuf=null;
        AbsTagInTemplate tagbean=null;
        String endtag=null;//当前正在分析的标签的结束标签
        List<String> lstEndChildTags=null;
        List<AbsTagInTemplate> lstResults=new ArrayList<AbsTagInTemplate>();
        for(int i=0;i<length;i++)
        {
            if(fullContentBuf.length()>0)
            {
                if(isInTagStartPart)
                {
                    if(content.charAt(i)=='>')
                    {
                        isInTagStartPart=false;
                        fullContentBuf.append(">");
                        if(fullContentBuf.charAt(fullContentBuf.length()-2)=='/')
                        {//这个标签没有内容，即为<wx:tag.../>的形式
                            tagbean.setEndposition(i);
                            lstResults.add(tagbean);
                            fullContentBuf=new StringBuffer();
                            if(propBuf.length()>0&&propBuf.charAt(propBuf.length()-1)=='/')
                            {
                                propBuf.deleteCharAt(propBuf.length()-1);
                            }
                        }
                        if(propBuf.length()>0)
                        {//如果当前标签有属性
                            tagbean.setMTagAttributes(RegexTools.parseXmlTagAttribute(propBuf.toString()));
                        }
                        propBuf=null;
                    }else if(content.charAt(i)=='<')
                    {
                        throw new WabacusConfigLoadingException("模板中"+fullContentBuf.toString()
                                +"标签不合法");
                    }else
                    {
                        fullContentBuf.append(content.charAt(i));
                        propBuf.append(content.charAt(i));
                    }
                }else
                {
                    if(i+endtag.length()>length)
                    {
                        throw new WabacusConfigLoadingException("模板中标签不合法，没有找到结束标签"+endtag);
                    }
                    if((lstEndChildTags==null||lstEndChildTags.size()==0)
                            &&content.substring(i,i+endtag.length()).equals(endtag))
                    {
                        fullContentBuf.append(endtag);
                        tagbean.setTagContent(getTagContent(fullContentBuf.toString()));
                        tagbean.setEndposition(i+endtag.length()-1);
                        lstResults.add(tagbean);
                        fullContentBuf=new StringBuffer();
                    }else
                    {
                        fullContentBuf.append(content.charAt(i));
                        if(i+7<length)
                        {//说明有可能包括子标签，因为如果后面还没有7个字符，则不可能包括子标签，因为子标签至少包括如下字符：<wx:x/>
                            if(content.substring(i,i+4).equals("<wx:"))
                            {
                                String endTag=getEndTag(content.substring(i+4));
                                if(!endTag.equals(""))
                                {//当前标签是一个有效的子标签，且需要</endtag>结束标签（即它不是<tag/>形式）
                                    lstEndChildTags.add(0,endTag);
                                }
                            }else if(lstEndChildTags!=null&&lstEndChildTags.size()>0)
                            {
                                String endTag=lstEndChildTags.get(0);
                                if(i+endTag.length()>length)
                                {
                                    throw new WabacusConfigLoadingException("模板中标签不合法，没有找到结束标签"
                                            +endtag);
                                }
                                if(content.substring(i,i+endTag.length()).equals(endTag))
                                {//碰到了此子标签的结束标签
                                    lstEndChildTags.remove(0);
                                }
                            }
                        }
                    }
                }
            }else
            {
                if(i+7>=length)
                {//因为一个合法的标签，至少要包括<wx:x/>内容，因此后面的字符串如果小于7个字符，则不可能还有需要分析的标签。
                    break;
                }
                if(content.substring(i,i+4).equals("<wx:"))
                {
                    int startposition=i;
                    fullContentBuf.append("<wx:");
                    String tagname="";
                    for(i=i+4;i<length;i++)
                    {
                        if(content.charAt(i)==' ') break;
                        if(content.charAt(i)=='>')
                        {
                            i=i-1;
                            break;
                        }
                        if(content.charAt(i)=='/')
                        {
                            if(i<length-1&&content.charAt(i+1)=='>')
                            {//是<wx:tag/>形式，即此标签既没有属性，也没有内容
                                i=i-1;
                                break;
                            }else
                            {
                                tagname="";
                                break;
                            }
                        }
                        if(content.charAt(i)=='<'||content.charAt(i)=='\''||content.charAt(i)=='\"')
                        {
                            tagname="";
                            break;
                        }
                        tagname=tagname+content.charAt(i);
                    }
                    if(tagname.equals(""))
                    {//不是有效标签，则略过
                        fullContentBuf=new StringBuffer();
                    }else
                    {
                        fullContentBuf.append(tagname).append(" ");
                        endtag="</wx:"+tagname+">";
                        propBuf=new StringBuffer();
                        isInTagStartPart=true;
                        tagbean=AbsTagInTemplate.createTagObj(parentTag,tagname,true);
                        tagbean.setStartposition(startposition);
                        lstEndChildTags=new ArrayList<String>();
                    }
                }
            }

        }
        if(fullContentBuf!=null&&fullContentBuf.length()>0)
        {
            throw new WabacusConfigLoadingException("解析模板失败，标签配置不合法");
        }
        return lstResults;
    }

    private static String getTagContent(String tagFullContent)
    {
        if(tagFullContent==null||tagFullContent.trim().equals("")) return "";
        int startidx=tagFullContent.indexOf(">");
        int endidx=tagFullContent.lastIndexOf("</");
        if(startidx<=0||endidx<=0||startidx>endidx) return "";
        return tagFullContent.substring(startidx+1,endidx);
    }

    private static String getEndTag(String content)
    {
        if(content==null||content.trim().equals("")) return "";
        String tagname="";
        int i=0;
        for(;i<content.length();i++)
        {
            if(content.charAt(i)==' ') break;//标签名必须是紧跟在<wx:后面的，所以碰到空格或>说明标签名循环结束
            if(content.charAt(i)=='>')
            {
                i--;
                break;
            }
            if(content.charAt(i)=='/')
            {//可能当前标签是<wx:tag/>形式，即既没有属性，也没有内容，则它也没有结束标签
                return "";
            }
            if(content.charAt(i)=='<'||content.charAt(i)=='\''||content.charAt(i)=='\"')
            {
                return "";
            }
            tagname=tagname+content.charAt(i);
        }
        if(tagname.equals("")) return "";
        int j=i;
        for(;j<content.length();j++)
        {
            if(content.charAt(j)=='<')
            {
                throw new WabacusConfigLoadingException("解析模板失败，标签<wx:"+tagname+">配置不合法");
            }
            if(content.charAt(j)=='>')
            {
                if(content.charAt(j-1)=='/')
                {//当前标签是<tag .../>格式，则不需考虑它的结束标签
                    return "";
                }
                break;
            }
        }
        if(j==content.length())
        {
            throw new WabacusConfigLoadingException("解析模板失败，标签<wx:"+tagname+">配置不合法");
        }
        return "</wx:"+tagname+">";//根据标签名拼凑出其结束标签
    }    
}
