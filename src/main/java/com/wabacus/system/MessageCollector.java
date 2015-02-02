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
package com.wabacus.system;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeTerminateException;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;

public class MessageCollector
{
    public final static int PROMPT_MODE_ALONE=0;
    
    public final static int PROMPT_MODE_MERGEBEFORE=1;
    
    public final static int PROMPT_MODE_MERGEAFTER=2;
    
    public final static int PROMPT_MODE_MERGEALL=3;
    
    private WabacusResponse wresponse;
    
    private List<Message> lstJsErrorMessages;//存放通过error()提示到前台的信息列表
    
    private List<Message> lstJsWarnMessages;
    
    private List<Message> lstJsAlertMessages;
    
    private List<Message> lstJsSuccessMessages;
    
    private String confirmmessage;
    
    private String confirmkey;//确认提示的KEY
    
    private String confirmurl;
    
    public MessageCollector(WabacusResponse wresponse)
    {
        lstJsAlertMessages=new ArrayList<Message>();
        lstJsSuccessMessages=new ArrayList<Message>();
        lstJsWarnMessages=new ArrayList<Message>();
        lstJsErrorMessages=new ArrayList<Message>();
        this.wresponse=wresponse;
    }

    public void alert(String alertinfo)
    {
        alert(alertinfo,null,PROMPT_MODE_MERGEALL,false);
    }
    
    public void alert(String alertinfo,String popuparams,boolean isTerminate)
    {
        alert(alertinfo,popuparams,PROMPT_MODE_MERGEALL,isTerminate);
    }
    
    public void alert(String alertinfo,String popuparams,int promptmode,boolean isTerminate)
    {
        if(!Tools.isEmpty(alertinfo))
        {
            addMessage(alertinfo,this.lstJsAlertMessages,popuparams,promptmode);
        }
        if(isTerminate) throw new WabacusRuntimeTerminateException();
    }
    
    public void success(String successinfo)
    {
        success(successinfo,null,PROMPT_MODE_MERGEALL,false);
    }
    
    public void success(String successinfo,String popuparams,boolean isTerminate)
    {
        success(successinfo,popuparams,PROMPT_MODE_MERGEALL,isTerminate);
    }
    
    public void success(String successinfo,String popuparams,int promptmode,boolean isTerminate)
    {
        if(!Tools.isEmpty(successinfo))
        {
            addMessage(successinfo,this.lstJsSuccessMessages,popuparams,promptmode);
        }
        if(isTerminate) throw new WabacusRuntimeTerminateException();
    }
    
    public void warn(String warnInfo)
    {
        warn(warnInfo,null,PROMPT_MODE_MERGEALL,false);
    }
    
    public void warn(String warninfo,String popuparams,boolean isTerminate)
    {
        warn(warninfo,popuparams,PROMPT_MODE_MERGEALL,isTerminate);
    }
    
    public void warn(String warninfo,String popuparams,int promptmode,boolean isTerminate)
    {
        if(!Tools.isEmpty(warninfo))
        {
            addMessage(warninfo,this.lstJsWarnMessages,popuparams,promptmode);
        }
        if(isTerminate) throw new WabacusRuntimeTerminateException();
    }
    
    public void error(String errorInfo)
    {
        error(errorInfo,null,PROMPT_MODE_MERGEALL,false);
    }
    
    public void error(String errorInfo,String popuparams,boolean isTerminate)
    {
        error(errorInfo,popuparams,PROMPT_MODE_MERGEALL,isTerminate);
    }

    public void error(String errorInfo,String popuparams,int promptmode,boolean isTerminate)
    {
        if(!Tools.isEmpty(errorInfo))
        {
            addMessage(errorInfo,this.lstJsErrorMessages,popuparams,promptmode);
        }
        wresponse.setStatecode(Consts.STATECODE_FAILED);
        if(isTerminate) throw new WabacusRuntimeException();
    }
    
    private void addMessage(String message,List<Message> lstOldMessages,String popuparams,int promptmode)
    {
        if(Tools.isEmpty(message)) return;
        Message oldMessObj=lstOldMessages!=null&&lstOldMessages.size()>0?lstOldMessages.get(lstOldMessages.size()-1):null;
        if(promptmode==PROMPT_MODE_ALONE||promptmode==PROMPT_MODE_MERGEAFTER||oldMessObj==null||oldMessObj.getPromptMode()==PROMPT_MODE_ALONE
                ||oldMessObj.getPromptMode()==PROMPT_MODE_MERGEBEFORE)
        {
            Message messObj=new Message();
            messObj.setPopupparams(popuparams);
            messObj.setPromptMode(promptmode);
            messObj.addMessage(message);
            lstOldMessages.add(messObj);
        }else
        {
            oldMessObj.addMessage(message);
            if(popuparams!=null) oldMessObj.setPopupparams(popuparams);
        }

    }
    
    public void confirm(String key,String message)
    {
        HttpServletRequest request=this.wresponse.getRRequest().getRequest();
        if(request==null||message==null||message.trim().equals("")||key==null||key.trim().equals("")) return;
        String url=request.getRequestURI();
        String sign="?";
        Enumeration enumer=request.getParameterNames();
        while(enumer.hasMoreElements())
        {
            String name=(String)enumer.nextElement();
            if(name==null||name.trim().equals("")) continue;
            String[] values=request.getParameterValues(name);
            if(values==null) continue;
            for(int i=0;i<values.length;i++)
            {
                url+=sign+name+"="+values[i];
                sign="&";
            }
        }
        this.confirmmessage=message;
        this.confirmkey=key;
        this.confirmurl=url;
        throw new WabacusRuntimeTerminateException();
    }

    public String promptMessageFirstTime(String defaulterrorprompt)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(promptMessageFirstTime(this.lstJsAlertMessages,"wx_alert",null));
        resultBuf.append(promptMessageFirstTime(this.lstJsSuccessMessages,"wx_success",null));
        resultBuf.append(promptMessageFirstTime(this.lstJsWarnMessages,"wx_warn",null));
        resultBuf.append(promptMessageFirstTime(this.lstJsErrorMessages,"wx_error",defaulterrorprompt));
        return resultBuf.toString();
    }
    
    private String promptMessageFirstTime(List<Message> lstMessages,String promptcode,String defaultmessage)
    {
        if(lstMessages==null||lstMessages.size()==0) return "";
        StringBuilder resultBuf=new StringBuilder();
        String messesTmp;
        for(Message messObjTmp:lstMessages)
        {
            if(messObjTmp==null) continue;
            messesTmp=messObjTmp.getMessages("<br/>");
            if(Tools.isEmpty(messesTmp)) continue;
            resultBuf.append(promptcode+"('"+messesTmp+"',"+messObjTmp.getPopupparams()+");");
        }
        if(resultBuf.length()==0&&!Tools.isEmpty(defaultmessage)) resultBuf.append(promptcode+"('"+defaultmessage+"');");
        return resultBuf.toString();
    }
    
    public String promptMessageInNonPage(String defaulterrorprompt)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(promptMessageInNonPage(this.lstJsAlertMessages,null));
        resultBuf.append(promptMessageInNonPage(this.lstJsSuccessMessages,null));
        resultBuf.append(promptMessageInNonPage(this.lstJsWarnMessages,null));
        resultBuf.append(promptMessageInNonPage(this.lstJsErrorMessages,defaulterrorprompt));
        return resultBuf.toString();
    }
    
    private String promptMessageInNonPage(List<Message> lstMessages,String defaultmessage)
    {
        if(lstMessages==null||lstMessages.size()==0) return "";
        StringBuilder resultBuf=new StringBuilder();
        String messesTmp;
        for(Message messObjTmp:lstMessages)
        {
            if(messObjTmp==null) continue;
            messesTmp=messObjTmp.getMessages("<br/>");
            if(!Tools.isEmpty(messesTmp)) resultBuf.append(messesTmp);//所有Message对象中的信息都直接输出显示，因为这里不是弹出窗口
        }
        if(resultBuf.length()==0&&!Tools.isEmpty(defaultmessage)) resultBuf.append(defaultmessage);
        return resultBuf.toString();
    }
    
    public String promptMessageByRefreshJs(String defaulterrorprompt)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(promptMessageByRefreshJs(this.lstJsAlertMessages,"alert",null));
        resultBuf.append(promptMessageByRefreshJs(this.lstJsSuccessMessages,"success",null));
        resultBuf.append(promptMessageByRefreshJs(this.lstJsWarnMessages,"warn",null));
        resultBuf.append(promptMessageByRefreshJs(this.lstJsErrorMessages,"error",defaulterrorprompt));
        return resultBuf.toString();
    }
    
    private String promptMessageByRefreshJs(List<Message> lstMessages,String paramname,String defaultmessage)
    {
        if(lstMessages==null||lstMessages.size()==0) return "";
        StringBuilder resultBuf=new StringBuilder();
        String messesTmp;
        for(Message messObjTmp:lstMessages)
        {
            if(messObjTmp==null) continue;
            messesTmp=messObjTmp.getMessages("<br/>");
            if(Tools.isEmpty(messesTmp)) continue;
            resultBuf.append("{message:'"+messesTmp+"',popupparams:"+messObjTmp.getPopupparams()+"},");
        }
        if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==',') resultBuf.deleteCharAt(resultBuf.length()-1);
        if(resultBuf.length()==0&&!Tools.isEmpty(defaultmessage)) resultBuf.append("{message:'"+defaultmessage+"'}");
        return resultBuf.length()>0?paramname+":["+resultBuf.toString()+"],":"";
    }

    public String getConfirmmessage()
    {
        return confirmmessage;
    }

    public String getConfirmkey()
    {
        return confirmkey;
    }

    public String getConfirmurl()
    {
        return confirmurl;
    }

    private class Message
    {
        private List<String> lstMessages;
        
        private String popupparams;
        
        private int promptMode;//本组信息提示模式

        public String getPopupparams()
        {
            if(Tools.isEmpty(popupparams)) return "null";
            if(!popupparams.startsWith("{")||!popupparams.endsWith("}"))
            {
                popupparams="{"+popupparams+"}";
            }
            return popupparams;
        }

        public String getMessages(String seperator)
        {
            if(lstMessages==null||lstMessages.size()==0) return "";
            if(Tools.isEmpty(seperator)) seperator=" ";
            String resultStr="";
            for(String messTmp:lstMessages)
            {
                resultStr+=messTmp+seperator;
            }
            if(resultStr.endsWith(seperator)) resultStr=resultStr.substring(0,resultStr.length()-seperator.length());
            return resultStr;
        }

        public void setPopupparams(String popupparams)
        {
            this.popupparams=popupparams;
        }

        public int getPromptMode()
        {
            return promptMode;
        }

        public void setPromptMode(int promptMode)
        {
            this.promptMode=promptMode;
        }
        
        private void addMessage(String message)
        {
            if(this.lstMessages==null) this.lstMessages=new ArrayList<String>();
            if(Tools.isEmpty(message)||this.lstMessages.contains(message)) return;
            this.lstMessages.add(message);
        }
    }
}
