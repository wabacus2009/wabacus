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
package com.wabacus.system.buttons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.wabacus.config.Config;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeTerminateException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.component.application.report.EditableDetailReportType;
import com.wabacus.system.component.application.report.EditableDetailReportType2;
import com.wabacus.system.component.application.report.EditableListReportType;
import com.wabacus.system.component.application.report.EditableListReportType2;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.IEditableReportEditGroupOwnerBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.transaction.DefaultTransactionType;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.system.serveraction.IServerAction;
import com.wabacus.util.Consts;
import com.wabacus.util.Logger;
import com.wabacus.util.Tools;

public class ServerSQLActionButton extends WabacusButton implements IServerAction,IEditableReportEditGroupOwnerBean
{
    private boolean shouldRefreshPage;

    private String beforeCallbackMethod;

    private String afterCallbackMethod;

    private String conditions;

    private String successprompt;//执行操作成功后的前台提示信息

    private String failedprompt;

    private boolean isAutoReportdata=true;

    private EditableReportSQLButtonDataBean editDataBean;//<button/>效果与<delete/>类似，都不会影响到列的可编辑性

    public ServerSQLActionButton(IComponentConfigBean ccbean)
    {
        super(ccbean);
    }

    public String showButton(ReportRequest rrequest,String dynclickevent)
    {
        if(editDataBean==null||Tools.isEmpty(editDataBean.getLsAllEditActions())) return "";
        return super.showButton(rrequest,getMyClickEvent(rrequest));
    }

    public String showButton(ReportRequest rrequest,String dynclickevent,String button)
    {
        if(editDataBean==null||Tools.isEmpty(editDataBean.getLsAllEditActions())) return "";
        return super.showButton(rrequest,getMyClickEvent(rrequest),button);
    }

    public String showMenu(ReportRequest rrequest,String dynclickevent)
    {
        if(editDataBean==null||Tools.isEmpty(editDataBean.getLsAllEditActions())) return "";
        return super.showMenu(rrequest,getMyClickEvent(rrequest));
    }

    private String getMyClickEvent(ReportRequest rrequest)
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append("invokeServerActionForReportData('"+this.ccbean.getPageBean().getId()+"','"+this.ccbean.getId()+"','button");
        if(this.isAutoReportdata) resultBuf.append("_autoreportdata");
        resultBuf.append("{"+this.getName()+"}',");
        resultBuf.append(this.isAutoReportdata?this.conditions:"null");
        resultBuf.append(",null,"+this.shouldRefreshPage+","+this.beforeCallbackMethod+","+this.afterCallbackMethod+")");
        return resultBuf.toString();
    }

    public String executeSeverAction(ReportRequest rrequest,IComponentConfigBean ccbean,List<Map<String,String>> lstData,
            Map<String,String> mCustomizedData)
    {
        if(editDataBean==null) return "0";
        ReportBean rbean=(ReportBean)this.ccbean;
        CacheDataBean cdb=rrequest.getCdb(this.ccbean.getId());
        cdb.setLstEditedData(this.editDataBean,lstData);
        cdb.getAttributes().put("WX_UPDATE_CUSTOMIZEDATAS",mCustomizedData);//对于用户自定义的数据，都会存放在一个Map中，键为参数名；值为参数值
        cdb.setLstEditedParamValues(this.editDataBean,EditableReportAssistant.getInstance().getExternalValues(this.editDataBean,lstData,rrequest));
        rrequest.setTransactionObj(new DefaultTransactionType());
        List<ReportBean> lstSaveReportBeans=new ArrayList<ReportBean>();
        lstSaveReportBeans.add(rbean);
        List<AbsPageInterceptor> lstPageInterceptors=rrequest.getLstPageInterceptors();
        if(lstPageInterceptors!=null&&lstPageInterceptors.size()>0)
        {
            for(AbsPageInterceptor pageInterceptorObjTmp:lstPageInterceptors)
            {
                pageInterceptorObjTmp.doStartSave(rrequest,lstSaveReportBeans);
            }
        }
        List<AbsUpdateAction> lstAllUpdateActions=new ArrayList<AbsUpdateAction>();
        lstAllUpdateActions.addAll(this.editDataBean.getLsAllEditActions());
        if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().beginTransaction(rrequest,lstAllUpdateActions);
        Exception exception=null;
        try
        {
            int rtnVal;
            if(rbean.getInterceptor()!=null)
            {
                rtnVal=rbean.getInterceptor().doSave(rrequest,rbean,this.editDataBean);
            }else
            {
                rtnVal=EditableReportAssistant.getInstance().doSaveReport(rrequest,rbean,this.editDataBean);
            }
            if(rtnVal==IInterceptor.WX_RETURNVAL_TERMINATE||rtnVal==IInterceptor.WX_RETURNVAL_SKIP)
            {
                if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().rollbackTransaction(rrequest,lstAllUpdateActions);
            }else
            {
                if(lstPageInterceptors!=null&&lstPageInterceptors.size()>0)
                {
                    AbsPageInterceptor pageInterceptorObjTmp;
                    for(int i=lstPageInterceptors.size()-1;i>=0;i--)
                    {//这个调用顺序与调用doStartSave()方法相反
                        pageInterceptorObjTmp=lstPageInterceptors.get(i);
                        pageInterceptorObjTmp.doEndSave(rrequest,lstSaveReportBeans);
                    }
                }
                if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().commitTransaction(rrequest,lstAllUpdateActions);
                if(this.successprompt!=null&&!this.successprompt.trim().equals(""))
                {
                    rrequest.getWResponse().getMessageCollector().success(rrequest.getI18NStringValue(this.successprompt));
                }
            }
        }catch(WabacusRuntimeTerminateException wrwe)
        {
            if(rrequest.getTransactionWrapper()!=null)
            {
                if(rrequest.getWResponse().getStatecode()!=Consts.STATECODE_FAILED)
                {
                    rrequest.getTransactionWrapper().commitTransaction(rrequest,lstAllUpdateActions);
                }else
                {
                    rrequest.getTransactionWrapper().rollbackTransaction(rrequest,lstAllUpdateActions);
                    exception=wrwe;
                }
            }
            throw new WabacusRuntimeTerminateException();
        }catch(Exception e)
        {
            exception=e;
            if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().rollbackTransaction(rrequest,lstAllUpdateActions);
        }finally
        {
            rrequest.setTransactionObj(null);
        }
        if(exception!=null)
        {
            Logger.error("执行报表"+ccbean.getPath()+"下的按钮"+this.name+"配置的脚本失败",exception,this.getClass());
            rrequest.getWResponse().getMessageCollector().error(rrequest.getI18NStringValue(this.failedprompt),null,true);
            return "-1";
        }else
        {
            return "1";
        }
    }

    public String executeServerAction(HttpServletRequest request,HttpServletResponse response,List<Map<String,String>> lstData)
    {
        return "";
    }

    public void loadExtendConfig(XmlElementBean eleButtonBean)
    {
        super.loadExtendConfig(eleButtonBean);
        if(!(this.ccbean instanceof ReportBean))
        {
            throw new WabacusConfigLoadingException("组件"+this.ccbean.getPath()+"不是报表，不能配置执行脚本的按钮");
        }
        this.beforeCallbackMethod=eleButtonBean.attributeValue("beforecallbackmethod");
        if(this.beforeCallbackMethod!=null&&this.beforeCallbackMethod.trim().equals("")) this.beforeCallbackMethod=null;
        this.afterCallbackMethod=eleButtonBean.attributeValue("aftercallbackmethod");
        if(this.afterCallbackMethod!=null&&this.afterCallbackMethod.trim().equals("")) this.afterCallbackMethod=null;
        String refreshpage=eleButtonBean.attributeValue("refreshpage");
        refreshpage=refreshpage==null||refreshpage.trim().equals("")?"true":refreshpage.toLowerCase().trim();
        this.shouldRefreshPage=refreshpage.equals("true");
        String autoreportdata=eleButtonBean.attributeValue("autoreportdata");
        autoreportdata=autoreportdata==null?"":autoreportdata.toLowerCase().trim();
        this.isAutoReportdata=!autoreportdata.equals("false");
        if(this.isAutoReportdata&&Config.getInstance().getReportType(((ReportBean)this.ccbean).getType()) instanceof AbsListReportType)
        {
            this.conditions=eleButtonBean.attributeValue("conditions");
            this.conditions=this.conditions==null?"":this.conditions.trim();
            if(this.conditions.equals(""))
            {
                this.conditions="{name:'SELECTEDROW',value:true}";//对于数据自动列表报表，默认取选中行的记录
            }else
            {
                if((!this.conditions.startsWith("{")||!this.conditions.endsWith("}"))
                        &&(!this.conditions.startsWith("[")||!this.conditions.endsWith("]")))
                {
                    this.conditions="{"+this.conditions+"}";
                }
            }
        }
        String successmess=eleButtonBean.attributeValue("successprompt");
        String failedmess=eleButtonBean.attributeValue("failedprompt");
        if(successmess!=null)
        {
            this.successprompt=Config.getInstance().getResourceString(null,this.ccbean.getPageBean(),successmess,false).trim();
        }else
        {
            this.successprompt="";
        }
        if(failedmess!=null)
        {
            this.failedprompt=Config.getInstance().getResourceString(null,this.ccbean.getPageBean(),failedmess,false).trim();
        }else
        {
            this.failedprompt="";
        }
        EditableReportSQLButtonDataBean sqlButtonDataBean=new EditableReportSQLButtonDataBean(this);
        sqlButtonDataBean.setAutoReportdata(isAutoReportdata);
        this.editDataBean=(EditableReportSQLButtonDataBean)ComponentConfigLoadManager.loadEditConfig(this,sqlButtonDataBean,eleButtonBean);
    }

    public ReportBean getReportBean()
    {
        return (ReportBean)this.ccbean;
    }

    private boolean hasDoPostLoad;

    public void doPostLoad()
    {
        if(hasDoPostLoad) return;
        hasDoPostLoad=true;
        String reportTypeKey=null;
        if(this.isAutoReportdata)
        {
            if(!(Config.getInstance().getReportType(((ReportBean)this.ccbean).getType()) instanceof IEditableReportType))
            {
                throw new WabacusConfigLoadingException("组件"+this.ccbean.getPath()
                        +"不是可编辑报表类型，在用<button/>直接配置更新脚本时不能采用自动获取报表数据传到后台的方式，需要将<button/>的autoreportdata配置为false");
            }
            IEditableReportType reportTypeObj=(IEditableReportType)Config.getInstance().getReportType(((ReportBean)this.ccbean).getType());
            if(reportTypeObj instanceof EditableDetailReportType)
            {
                reportTypeKey=EditableDetailReportType.KEY;
            }else if(reportTypeObj instanceof EditableDetailReportType2)
            {
                reportTypeKey=EditableDetailReportType2.KEY;
            }else if(reportTypeObj instanceof EditableListReportType)
            {
                reportTypeKey=EditableListReportType.KEY;
            }else if(reportTypeObj instanceof EditableListReportType2)
            {
                reportTypeKey=EditableListReportType2.KEY;
            }else
            {
                throw new WabacusConfigLoadingException("加载报表"+this.ccbean.getPath()+"上的更新数据的按钮失败，无效的可编辑报表类型");
            }
        }
        if(this.editDataBean.parseActionscripts(reportTypeKey)<=0) this.editDataBean=null;
    }

    private boolean hasDoPostLoadFinally;

    public void doPostLoadFinally()
    {
        if(hasDoPostLoadFinally) return;
        hasDoPostLoadFinally=true;
        if(this.editDataBean!=null) this.editDataBean.doPostLoadFinally();
    }

    public AbsButtonType clone(IComponentConfigBean ccbean)
    {
        ServerSQLActionButton buttonNew=(ServerSQLActionButton)super.clone(ccbean);
        if(this.editDataBean!=null)
        {
            buttonNew.editDataBean=(EditableReportSQLButtonDataBean)this.editDataBean.clone(buttonNew);
        }
        return buttonNew;
    }
}
