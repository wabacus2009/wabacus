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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.map.ObjectMapper;

import com.wabacus.config.Config;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class ServerValidateBean extends AbsValidateBean
{
    private List<ServerValidateMethod> lstValidateMethods;
    
    private String servervalidateCallback;

    public ServerValidateBean(AbsInputBox owner)
    {
        super(owner);
    }

    public String getServervalidateCallback()
    {
        return servervalidateCallback;
    }

    public void setServervalidateCallback(String servervalidateCallback)
    {
        this.servervalidateCallback=servervalidateCallback;
    }

    public List<ServerValidateMethod> getLstValidateMethods()
    {
        return lstValidateMethods;
    }

    protected String getJsonStringQuote()
    {
        return "\"";
    }
    
    public void addServerValidateMethod(String methodname,String configParams)
    {
        if(methodname==null||methodname.trim().equals("")) return;
        if(lstValidateMethods==null) lstValidateMethods=new ArrayList<ServerValidateMethod>();
        Object[] validateObjsArr=getServerValidateClassMethod(methodname);
        this.lstValidateMethods.add(new ServerValidateMethod((Class)validateObjsArr[0],(Method)validateObjsArr[1]));
        parseConfigValidateMethodParams(methodname,configParams);
    }

    private Object[] getServerValidateClassMethod(String methodname)
    {
        Object[] validateObjsArr=getServerValidateClassMethod(methodname,owner.getOwner().getReportBean().getLstServerValidateClasses());
        if(validateObjsArr==null) validateObjsArr=getServerValidateClassMethod(methodname,Config.getInstance().getLstServerValidateClasses());
        if(validateObjsArr==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+this.owner.getOwner().getReportBean().getPath()+"失败，在所有服务器端校验类中没有找到服务器端校验方法："+methodname
                    +"(ReportRequest,String,Map,List)的定义");
        }
        return validateObjsArr;
    }
    
    private Object[] getServerValidateClassMethod(String methodname,List<Class> lstServerValidateClasses)
    {
        if(Tools.isEmpty(methodname)||Tools.isEmpty(lstServerValidateClasses)) return null;
        Method mTmp;
        for(Class cTmp:lstServerValidateClasses)
        {
            try
            {
                mTmp=cTmp.getMethod(methodname,new Class[] { ReportRequest.class, String.class, Map.class, List.class });
            }catch(NoSuchMethodException nse)
            {
                continue;
            }
            if(mTmp!=null) return new Object[] { cTmp, mTmp };
        }
        return null;
    }

    public boolean validate(ReportRequest rrequest,String boxvalue,Map<String,String> mOtherValues,List<String> lstErrorMessages,
            StringBuilder oblurErrorPromptParamsBuf)
    {
        if(this.lstValidateMethods==null||this.lstValidateMethods.size()==0) return true;
        if(oblurErrorPromptParamsBuf!=null&&AbsInputBox.VALIDATE_TYPE_ONSUBMIT.equals(validatetype)||oblurErrorPromptParamsBuf==null&&AbsInputBox.VALIDATE_TYPE_ONBLUR.equals(validatetype))
        {
            return true;
        }
        boolean isSuccess=true;
        ParamBean errormessParamBeanTmp, methodParamsBeanTmp;
        Map<String,Object> mParamValues=new HashMap<String,Object>();
        mParamValues.putAll(mOtherValues);
        for(ServerValidateMethod validateMethodTmp:this.lstValidateMethods)
        {
            lstErrorMessages.clear();//清除掉以前校验方法的校验出错信息
            errormessParamBeanTmp=mParamBeans!=null?mParamBeans.get(validateMethodTmp.getValidateMethod().getName()+"_errormessage"):null;
            if(errormessParamBeanTmp!=null)
            {
                String errormess=errormessParamBeanTmp.getRealParamValue(rrequest);
                if(!Tools.isEmpty(errormess))
                {
                    errormess=Tools.replaceAll(errormess,"#label#",owner.getOwner().getLabel(rrequest));
                    errormess=Tools.replaceAll(errormess,"#data#",boxvalue);
                    lstErrorMessages.add(errormess);
                }
            }
            methodParamsBeanTmp=mParamBeans!=null?mParamBeans.get(validateMethodTmp.getValidateMethod().getName()+"_methodparams"):null;
            if(methodParamsBeanTmp!=null)
            {
                String methodParamsJson=methodParamsBeanTmp.getRealParamValue(rrequest);
                if(!Tools.isEmpty(methodParamsJson))
                {
                    ObjectMapper mapper=new ObjectMapper();
                    try
                    {
                        mParamValues.put(validateMethodTmp.getValidateMethod().getName()+"_methodparams",mapper.readValue(methodParamsJson,methodParamsJson
                                .trim().startsWith("[")?List.class:Map.class));
                    }catch(Exception e)
                    {
                        throw new WabacusRuntimeException("对报表"+owner.getOwner().getReportBean().getPath()+"的输入框"+owner.getOwner().getInputBoxId()
                                +"的数据"+boxvalue+"进行服务器端校验时失败，配置的json参数"+methodParamsJson+"不是有效格式的json字符串，无法转换成JAVA对象",e);
                    }
                }
            }
            if(!validateMethodTmp.validate(rrequest,boxvalue,mParamValues,lstErrorMessages))
            {
                isSuccess=false;
                ParamBean tiparamsBeanTmp=mParamBeans!=null?mParamBeans.get(validateMethodTmp.getValidateMethod().getName()+"_tiparams"):null;
                if(tiparamsBeanTmp!=null&&oblurErrorPromptParamsBuf!=null)
                {
                    String tiparamValues=tiparamsBeanTmp.getRealParamValue(rrequest);
                    if(!Tools.isEmpty(tiparamValues)) oblurErrorPromptParamsBuf.append(tiparamValues);
                }
                break;
            }
        }
        if(oblurErrorPromptParamsBuf==null&&!isSuccess)
        {
            StringBuilder errormsgBuf=new StringBuilder();
            for(String errormsgTmp:lstErrorMessages)
            {
                if(errormsgTmp==null||errormsgTmp.trim().equals("")) continue;
                errormsgBuf.append(errormsgTmp).append(";");
            }
            if(errormsgBuf.length()>0&&errormsgBuf.charAt(errormsgBuf.length()-1)==';') errormsgBuf.deleteCharAt(errormsgBuf.length()-1);
            if(this.servervalidateCallback!=null&&!this.servervalidateCallback.trim().equals(""))
            {//需要执行客户端回调函数
                StringBuilder paramsBuf=new StringBuilder();
                paramsBuf.append("{inputboxid:\"").append(this.owner.getOwner().getInputBoxId()).append("\"");
                paramsBuf.append(",value:\"").append(boxvalue).append("\"");
                paramsBuf.append(",errormess:\"").append(errormsgBuf.toString()).append("\"");
                paramsBuf.append(",validatetype:\"onsubmit\"");
                paramsBuf.append(",isSuccess:false");
                paramsBuf.append(",serverDataObj:{");
                if(rrequest.getMServerValidateDatas()!=null&&rrequest.getMServerValidateDatas().size()>0)
                {
                    for(Entry<String,String> entryTmp:rrequest.getMServerValidateDatas().entrySet())
                    {
                        paramsBuf.append(entryTmp.getKey()+":\""+entryTmp.getValue()+"\",");
                    }
                    if(paramsBuf.charAt(paramsBuf.length()-1)==',') paramsBuf.deleteCharAt(paramsBuf.length()-1);
                }
                paramsBuf.append("}}");
                rrequest.getWResponse().addOnloadMethod(this.servervalidateCallback,paramsBuf.toString(),true,Consts.STATECODE_FAILED);
            }
            rrequest.getWResponse().setStatecode(Consts.STATECODE_FAILED);
            if(errormsgBuf.toString().trim().equals(""))
            {
                rrequest.getWResponse().terminateResponse();
            }else
            {
                rrequest.getWResponse().getMessageCollector().warn(errormsgBuf.toString(),null,true);
            }
        }
        return isSuccess;
    }

    private class ServerValidateMethod
    {
        private Class validateClass;

        private Method validateMethod;

        public ServerValidateMethod(Class validateClass,Method validateMethod)
        {
            this.validateClass=validateClass;
            this.validateMethod=validateMethod;
        }

        public Method getValidateMethod()
        {
            return validateMethod;
        }

        public boolean validate(ReportRequest rrequest,String boxvalue,Map<String,Object> mParamValues,List<String> lstErrorMessages)
        {
            try
            {
                return (Boolean)this.validateMethod.invoke(this.validateClass,new Object[] { rrequest, boxvalue, mParamValues, lstErrorMessages });
            }catch(Exception e)
            {
                throw new WabacusRuntimeException("对报表"+owner.getOwner().getReportBean().getPath()+"的输入框"+owner.getOwner().getInputBoxId()+"的数据"
                        +boxvalue+"进行服务器端校验时失败",e);
            }
        }
    }
}
