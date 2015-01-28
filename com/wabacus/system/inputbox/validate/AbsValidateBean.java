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
package com.wabacus.system.inputbox.validate;

import java.util.HashMap;
import java.util.Map;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.util.Tools;

public abstract class AbsValidateBean
{
    protected AbsInputBox owner;
    
    protected Map<String,ParamBean> mParamBeans;
    
    protected String validatetype;
    
    public AbsValidateBean(AbsInputBox owner)
    {
        this.owner=owner;
    }
    
    public void setValidatetype(String validatetype)
    {
        this.validatetype=validatetype;
    }
    
    public void parseConfigValidateMethodParams(String validateMethodName,String configMethodParams)
    {
        if(Tools.isEmpty(configMethodParams)) return;
        configMethodParams=configMethodParams.trim();
        String errormess=null;
        if(configMethodParams.indexOf("errormess:{")<0&&configMethodParams.indexOf("methodparams:{")<0&&configMethodParams.indexOf("tiparams:{")<0)
        {
            errormess=configMethodParams.trim();
        }else
        {
            errormess=getParamvalueFromConfigMethodParams(configMethodParams,"errormess",new String[]{"methodparams","tiparams"});
        }
        if(!Tools.isEmpty(errormess)) addParamBean(validateMethodName+"_errormessage",errormess,true);
        String jsmethodparams=getParamvalueFromConfigMethodParams(configMethodParams,"methodparams",new String[]{"errormess","tiparams"});
        if(!Tools.isEmpty(jsmethodparams))
        {
            addParamBean(validateMethodName+"_methodparams",parseJsParamsJsonString("{"+jsmethodparams+"}",null),false);
        }
        String tiparams=getParamvalueFromConfigMethodParams(configMethodParams,"tiparams",new String[]{"errormess","methodparams"});
        if(!Tools.isEmpty(tiparams)) addParamBean(validateMethodName+"_tiparams",parseJsParamsJsonString("{"+tiparams+"}","'"),false);
    }
    
    private String getParamvalueFromConfigMethodParams(String configMethodParams,String paramname,String[] otherParamnames)
    {
        if(Tools.isEmpty(configMethodParams)||configMethodParams.indexOf(paramname+":{")<0) return null;
        String paramvalue=configMethodParams.substring(configMethodParams.indexOf(paramname+":{")+(paramname+":{").length()).trim();
        int idxNearest=-1;//存放最近的下一个参数下标
        for(int i=0;i<otherParamnames.length;i++)
        {
            int idxTmp=paramvalue.indexOf(otherParamnames[i]+":{");
            if(idxNearest<0||(idxTmp>=0&&idxTmp<idxNearest))
            {
                idxNearest=idxTmp;
            }
        }
        if(idxNearest>0)
        {
            paramvalue=paramvalue.substring(0,idxNearest).trim();
            if(!paramvalue.endsWith(","))
            {
                throw new WabacusConfigLoadingException("加载报表"+this.owner.getOwner().getReportBean().getPath()+"的输入框"
                        +this.owner.getOwner().getInputBoxId()+"上的校验信息"+configMethodParams+"失败，参数格式无效");
            }
            paramvalue=paramvalue.substring(0,paramvalue.length()-1).trim();
        }
        if(!paramvalue.endsWith("}"))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.owner.getOwner().getReportBean().getPath()+"的输入框"
                    +this.owner.getOwner().getInputBoxId()+"上的校验信息"+configMethodParams+"失败，参数格式无效");
        }
        return paramvalue.substring(0,paramvalue.length()-1).trim();
    }
    
    protected String parseJsParamsJsonString(String jsparams,String jsonStringQuote)
    {
        if(Tools.isEmpty(jsparams)) return jsparams;
        if((!jsparams.startsWith("{")||!jsparams.endsWith("}"))&&(!jsparams.startsWith("[")||!jsparams.endsWith("]")))
        {
            jsparams="{"+jsparams+"}";
        }
        StringBuilder resultBuf=new StringBuilder();
        int idx=jsparams.indexOf("\\'");
        while(idx>=0)
        {
            resultBuf.append(jsparams.substring(0,idx).trim()+"\\'");
            jsparams=jsparams.substring(idx+2).trim();
            idx=jsparams.indexOf("\\'");
        }
        resultBuf.append(jsparams);
        jsparams=resultBuf.toString();
        if(Tools.isEmpty(jsonStringQuote)) jsonStringQuote=getJsonStringQuote();//没有显式指定，则在这里获取
        jsparams=Tools.replaceAll(jsparams,"{\\'","{"+jsonStringQuote);
        jsparams=Tools.replaceAll(jsparams,"\\'}",jsonStringQuote+"}");
        jsparams=Tools.replaceAll(jsparams,"\\':",jsonStringQuote+":");
        jsparams=Tools.replaceAll(jsparams,":\\'",":"+jsonStringQuote);
        jsparams=Tools.replaceAll(jsparams,",\\'",","+jsonStringQuote);
        jsparams=Tools.replaceAll(jsparams,"\\',",jsonStringQuote+",");
        return jsparams;
    }
    
    protected abstract String getJsonStringQuote();
    
    public void addParamBean(String paramName,String paramValue,boolean isStringType)
    {
        if(Tools.isEmpty(paramName)||Tools.isEmpty(paramValue)) return;
        if(mParamBeans==null) mParamBeans=new HashMap<String,ParamBean>();
        ParamBean paramBean=new ParamBean();
        paramBean.setParamname(paramName);
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(owner.getOwner().getReportBean().getPageBean(),paramValue);
        paramBean.setParamvalue((String)objArr[0]);
        paramBean.setMDynParamvalueParts((Map<String,String>)objArr[1]);
        paramBean.setStringType(isStringType);
        this.mParamBeans.put(paramName,paramBean);
    }

    protected class ParamBean
    {
        protected String paramname;

        protected String paramvalue;

        protected Map<String,String> mDynParamvalueParts;

        protected boolean isStringType=true;
        
        public String getParamname()
        {
            return paramname;
        }

        public void setParamname(String paramname)
        {
            this.paramname=paramname;
        }

        public String getParamvalue()
        {
            return paramvalue;
        }

        public void setParamvalue(String paramvalue)
        {
            this.paramvalue=paramvalue;
        }

        public Map<String,String> getMDynParamvalueParts()
        {
            return mDynParamvalueParts;
        }

        public void setMDynParamvalueParts(Map<String,String> dynParamvalueParts)
        {
            mDynParamvalueParts=dynParamvalueParts;
        }

        public boolean isStringType()
        {
            return isStringType;
        }

        public void setStringType(boolean isStringType)
        {
            this.isStringType=isStringType;
        }

        public String getRealParamValue(ReportRequest rrequest)
        {
            return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.paramvalue,this.mDynParamvalueParts,"").trim();
        }
    }
}
