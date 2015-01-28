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
package com.wabacus.config.typeprompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.TextBox;
import com.wabacus.system.inputbox.option.TypepromptOptionBean;

public class TypePromptBean implements Cloneable
{
    public final static String MATCHCONDITION_PLACEHOLDER="{#matchcondition#}";
    
    public final static DefaultTypePromptOptionMatcher DEFAULT_OPTION_MATCHER=new DefaultTypePromptOptionMatcher();
    
    private int resultcount=15;

    private int resultspanwidth=-1;//显示结果的<span/>的宽度，如果为-1，则与相应输入框保持同一宽度。

    private int resultspanMaxheight;
    
    private int timeout=5;//显示结果的<span/>是否自动消失，如果配置为>0的数，表示timeout秒后自动消息，否则不自动消失

    private boolean casesensitive;
    
    private boolean showtitle;//是否需要显示标题行，此属性不用配置，由各选项是否都配置了不为空的title属性来自动决定
    
    private String callbackmethod;
    
    private List<TypePromptColBean> lstPColBeans;

    private List<TypepromptOptionBean> lstOptionBeans;//配置的所有选项<option/>信息
    
    private ITypePromptOptionMatcher typePromptMatcherObj;
    
    private String clientMatcherMethodName;
    
    public int getResultcount()
    {
        return resultcount;
    }

    public void setResultcount(int resultcount)
    {
        if(resultcount<=0) resultcount=Integer.MAX_VALUE;
        this.resultcount=resultcount;
    }

    public int getResultspanwidth()
    {
        return resultspanwidth;
    }

    public void setResultspanwidth(int resultspanwidth)
    {
        this.resultspanwidth=resultspanwidth;
    }

    public int getResultspanMaxheight()
    {
        return resultspanMaxheight;
    }

    public void setResultspanMaxheight(int resultspanMaxheight)
    {
        this.resultspanMaxheight=resultspanMaxheight;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout=timeout;
    }

    public boolean isCasesensitive()
    {
        return casesensitive;
    }

    public void setCasesensitive(boolean casesensitive)
    {
        this.casesensitive=casesensitive;
    }

    public boolean isShowtitle()
    {
        return showtitle;
    }

    public void setShowtitle(boolean showtitle)
    {
        this.showtitle=showtitle;
    }

    public String getCallbackmethod()
    {
        return callbackmethod;
    }

    public void setCallbackmethod(String callbackmethod)
    {
        this.callbackmethod=callbackmethod;
    }

    public List<TypePromptColBean> getLstPColBeans()
    {
        return lstPColBeans;
    }

    public void setLstPColBeans(List<TypePromptColBean> lstPColBeans)
    {
        this.lstPColBeans=lstPColBeans;
    }

    public List<TypepromptOptionBean> getLstOptionBeans()
    {
        return lstOptionBeans;
    }

    public void setLstOptionBeans(List<TypepromptOptionBean> lstOptionBeans)
    {
        this.lstOptionBeans=lstOptionBeans;
    }

    public ITypePromptOptionMatcher getTypePromptMatcherObj()
    {
        return typePromptMatcherObj;
    }

    public void setTypePromptMatcherObj(ITypePromptOptionMatcher typePromptMatcherObj)
    {
        this.typePromptMatcherObj=typePromptMatcherObj;
    }

    public String getClientMatcherMethodName()
    {
        return clientMatcherMethodName;
    }

    public void setClientMatcherMethodName(String clientMatcherMethodName)
    {
        this.clientMatcherMethodName=clientMatcherMethodName;
    }

    public List<Map<String,String>> getLstRuntimeOptionsData(ReportRequest rrequest,ReportBean rbean,String txtValue)
    {
        if((txtValue==null||txtValue.trim().equals(""))) return null;
        List<Map<String,String>> lstResults=new ArrayList<Map<String,String>>();
        List<Map<String,String>> lstOptionsTmp;
        for(TypepromptOptionBean obean:this.lstOptionBeans)
        {
            lstOptionsTmp=obean.getLstRuntimeOptions(rrequest,txtValue);
            if(lstOptionsTmp!=null) lstResults.addAll(lstOptionsTmp);
        }
        if(rbean.getInterceptor()!=null)
        {
            lstResults=(List<Map<String,String>>)rbean.getInterceptor().afterLoadData(rrequest,rbean,this,lstResults);
        }
        return lstResults;
    }
    
    public Object clone(TextBox boxNew)
    {
        try
        {
            TypePromptBean tpbNew=(TypePromptBean)super.clone();
            if(lstPColBeans!=null)
            {
                List<TypePromptColBean> lstPColBeansNew=new ArrayList<TypePromptColBean>();
                for(int i=0;i<lstPColBeans.size();i++)
                {
                    lstPColBeansNew.add((TypePromptColBean)lstPColBeans.get(i).clone());
                }
                tpbNew.setLstPColBeans(lstPColBeansNew);
            }
            if(this.lstOptionBeans!=null)
            {
                List<TypepromptOptionBean> lstOptionsNew=new ArrayList<TypepromptOptionBean>();
                for(TypepromptOptionBean optionBeanTmp:this.lstOptionBeans)
                {
                    lstOptionsNew.add(optionBeanTmp.clone(boxNew));
                }
                tpbNew.lstOptionBeans=lstOptionsNew;
            }
            return tpbNew;
        }catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
