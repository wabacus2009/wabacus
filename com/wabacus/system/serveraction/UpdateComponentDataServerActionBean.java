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
package com.wabacus.system.serveraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.ServerSQLActionButton;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class UpdateComponentDataServerActionBean
{
    private ReportRequest rrequest;

    private String componentid;

    private boolean shouldRefreshPage;

    private String callbackMethod;

    private String serverClassName;

    private List<Map<String,String>> lstParams;//客户端传给被调用服务器端的参数
    
    private Map<String,String> mCustomizedParams;

    private IServerAction serverActionObj;
    
    public UpdateComponentDataServerActionBean(ReportRequest rrequest)
    {
        this.rrequest=rrequest;
    }

    public String getComponentid()
    {
        return componentid;
    }

    public void setComponentid(String componentid)
    {
        this.componentid=componentid;
    }

    public boolean isShouldRefreshPage()
    {
        return shouldRefreshPage;
    }

    public void setShouldRefreshPage(boolean shouldRefreshPage)
    {
        this.shouldRefreshPage=shouldRefreshPage;
    }

    public String getCallbackMethod()
    {
        return callbackMethod;
    }

    public void setCallbackMethod(String callbackMethod)
    {
        this.callbackMethod=callbackMethod;
    }

    public String getServerClassName()
    {
        return serverClassName;
    }

    public void setServerClassName(String serverClassName)
    {
        this.serverClassName=serverClassName;
    }

    private void setServerActionObj(IServerAction serverActionObj)
    {
        this.serverActionObj=serverActionObj;
    }
    
    public void setLstParams(List<Map<String,String>> lstParams)
    {
        this.lstParams=lstParams;
    }

    public List<Map<String,String>> getLstParams()
    {
        return lstParams;
    }

    public Map<String,String> getMCustomizedParams()
    {
        return mCustomizedParams;
    }

    public void setMCustomizedParams(Map<String,String> customizedParams)
    {
        mCustomizedParams=customizedParams;
    }

    public static void initServerActionBean(ReportRequest rrequest)
    {
        String componetid=rrequest.getStringAttribute("WX_SERVERACTION_COMPONENTID","");
        String serverClassName=rrequest.getStringAttribute("WX_SERVERACTION_SERVERCLASS","");
        if(componetid.equals("")||serverClassName.equals("")) return;
        IComponentConfigBean ccbean;
        if(rrequest.getPagebean().getId().equals(componetid))
        {
            ccbean=rrequest.getPagebean();
        }else
        {
            ccbean=rrequest.getPagebean().getChildComponentBean(componetid,true);
        }
        if(ccbean==null) throw new WabacusRuntimeException("没有配置id为"+componetid+"的组件");
        UpdateComponentDataServerActionBean serverActionBean=new UpdateComponentDataServerActionBean(rrequest);
        rrequest.setServerActionBean(serverActionBean);
        serverActionBean.setServerClassName(serverClassName);
        serverActionBean.setComponentid(componetid);
        String params=rrequest.getStringAttribute("WX_SERVERACTION_PARAMS","");
        if(!params.equals(""))
        {
            List<Map<String,String>> lstParams=EditableReportAssistant.getInstance().parseSaveDataStringToList(params);
            if(lstParams!=null&&lstParams.size()>0)
            {
                if(ccbean instanceof ReportBean)
                {
                    lstParams=getRealReportDataParams(rrequest,(ReportBean)ccbean,lstParams);
                }
                serverActionBean.setLstParams(lstParams);
            }
        }
        String customizedparams=rrequest.getStringAttribute("WX_SERVERACTION_CUSTOMIZEDPARAMS","");
        if(!customizedparams.trim().equals(""))
        {
            List<Map<String,String>> lstCustomizedParams=EditableReportAssistant.getInstance().parseSaveDataStringToList(customizedparams);
            if(lstCustomizedParams!=null&&lstCustomizedParams.size()>0)
            {
                serverActionBean.setMCustomizedParams(lstCustomizedParams.get(0));
            }
        }
        serverActionBean.setCallbackMethod(rrequest.getStringAttribute("WX_SERVERACTION_CALLBACKMETHOD",""));
        serverActionBean.setShouldRefreshPage(rrequest.getStringAttribute("WX_SERVERACTION_SHOULDREFRESH","true").equalsIgnoreCase("true"));
        if(Tools.isDefineKey("button",serverClassName))
        {//如果是配置的ServerSQLActionButton按钮
            serverActionBean.setServerActionObj(getSqlActionButtonObj(ccbean,serverClassName));
        }else
        {
            Object obj=null;
            try
            {
                obj=ConfigLoadManager.currentDynClassLoader.loadClassByCurrentLoader(serverClassName.trim()).newInstance();
            }catch(InstantiationException e)
            {
                throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"无法实例化",e);
            }catch(IllegalAccessException e)
            {
                throw new WabacusRuntimeException("调用的服务器端类"+serverClassName+"无法访问",e);
            }
            if(!(obj instanceof IServerAction))
            {
                throw new WabacusRuntimeException("调用的服务器端类"+obj.getClass().getName()+"没有实现"+IServerAction.class.getName()+"接口");
            }
            serverActionBean.setServerActionObj((IServerAction)obj);
        }
    }

    private static List<Map<String,String>> getRealReportDataParams(ReportRequest rrequest,ReportBean rbean,List<Map<String,String>> lstParams)
    {
        if(lstParams==null||lstParams.size()==0) return lstParams;
        List<Map<String,String>> lstResult=new ArrayList<Map<String,String>>();
        for(Map<String,String> mParamsTmp:lstParams)
        {
            if(mParamsTmp==null||mParamsTmp.size()==0) continue;
            Map<String,String> mParamsNew=new HashMap<String,String>();
            String paramnameTmp;
            String paramvalueTmp;
            for(Entry<String,String> entryTmp:mParamsTmp.entrySet())
            {
                paramnameTmp=entryTmp.getKey();
                paramvalueTmp=EditableReportAssistant.getInstance().getColParamRealValue(rrequest,rbean,paramnameTmp,entryTmp.getValue());
                if(paramnameTmp.startsWith(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX))
                {
                    paramnameTmp=paramnameTmp.substring(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX.length());
                }
                //System.out.println("||"+paramnameTmp+"="+paramvalueTmp);
                mParamsNew.put(paramnameTmp,paramvalueTmp);
            }
            lstResult.add(mParamsNew);
        }
        return lstResult;
    }

    private static ServerSQLActionButton getSqlActionButtonObj(IComponentConfigBean ccbean,String serverClassName)
    {
        if(!Tools.isDefineKey("button",serverClassName)) return null;
        String buttonname=Tools.getRealKeyByDefine("button",serverClassName);
        if(ccbean.getButtonsBean()==null)
        {
            throw new WabacusRuntimeException("组件"+ccbean.getPath()+"没有配置name为"+buttonname+"的按钮");
        }
        AbsButtonType buttonObj=ccbean.getButtonsBean().getButtonByName(buttonname);
        if(buttonObj==null)
        {
            throw new WabacusRuntimeException("组件"+ccbean.getPath()+"没有配置name为"+buttonname+"的按钮");
        }
        if(!(buttonObj instanceof ServerSQLActionButton))
        {
            throw new WabacusRuntimeException("组件"+ccbean.getPath()+"配置name为"+buttonname+"的按钮不是"+ServerSQLActionButton.class.getName()+"类型");
        }
        return (ServerSQLActionButton)buttonObj;
    }
    
    public void executeServerAction(IComponentConfigBean ccbean)
    {
        if(!ccbean.getId().equals(this.componentid.trim())) return;
        String returnVal=this.serverActionObj.executeSeverAction(rrequest,ccbean,this.lstParams,this.mCustomizedParams);
        if(this.callbackMethod!=null&&!this.callbackMethod.trim().equals(""))
        {
            StringBuffer paramsBuf=new StringBuffer("{");
            paramsBuf.append("pageid:\""+rrequest.getPagebean().getId()+"\"");
            paramsBuf.append(",componentid:\""+ccbean.getId()+"\"");
            paramsBuf.append(",returnValue:\"").append(returnVal).append("\"");
            paramsBuf.append("}");
            rrequest.getWResponse().addOnloadMethod(this.callbackMethod.trim(),paramsBuf.toString(),true);
        }
        if(!this.isShouldRefreshPage())
        {
            rrequest.getWResponse().setStatecode(Consts.STATECODE_NONREFRESHPAGE);
        }
        rrequest.setServerActionBean(null);
    }    
}
