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
package com.wabacus.system.assistant;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.exception.WabacusRuntimeTerminateException;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.buttons.EditableReportSQLButtonDataBean;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportDeleteDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportExternalValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportParamBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSecretColValueBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.dataset.update.transaction.DefaultTransactionType;
import com.wabacus.system.intercept.AbsPageInterceptor;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableReportAssistant
{
    private static Log log=LogFactory.getLog(EditableReportAssistant.class);

    private final static EditableReportAssistant instance=new EditableReportAssistant();

    protected EditableReportAssistant()
    {}

    public static EditableReportAssistant getInstance()
    {
        return instance;
    }

    public String getInputBoxId(ColBean cbean)
    {
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return "";
        return cbean.getReportBean().getGuid()+"_wxcol_"+cbean.getProperty();
    }

    public String getInputBoxId(ReportBean rbean,String property)
    {
        if(property==null||property.trim().equals("")) return "";
        return rbean.getGuid()+"_"+property;
    }
    
    public String getColParamName(ColBean cbean)
    {
        return cbean.getProperty();
    }
    
    public String getColParamValue(ReportRequest rrequest,Map<String,String> mColParamsValue,ColBean cbean)
    {
        return getColParamValue(rrequest,cbean.getReportBean(),mColParamsValue,getColParamName(cbean));
    }
    
    public String getColParamValue(ReportRequest rrequest,ReportBean rbean,Map<String,String> mColParamsValue,String paramname)
    {
        if(mColParamsValue==null) return null;
        String paramvalue=null;
        if(mColParamsValue.containsKey(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX+paramname))
        {
            EditableReportSecretColValueBean secretColvalueBean=EditableReportSecretColValueBean.loadFromSession(rrequest,rbean);
            if(secretColvalueBean==null)
            {
                rrequest.getWResponse().setStatecode(Consts.STATECODE_FAILED);
                rrequest.getWResponse().getMessageCollector().warn("session过期，没有取到保存数据，请刷新后重试",null,true);
            }
            String colkey=mColParamsValue.get(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX+paramname);
            if(colkey==null||colkey.trim().equals("")) return null;
            if(!secretColvalueBean.containsColkey(colkey))
            {
                paramvalue=colkey;
            }else
            {
                paramvalue=secretColvalueBean.getParamValue(colkey);
            }
        }else
        {//直接从客户端
            paramvalue=mColParamsValue.get(paramname);
        }
        return paramvalue;
    }
    
    public String getColParamRealValue(ReportRequest rrequest,ReportBean rbean,String paramname,String paramvalue)
    {
        if(paramname==null||!paramname.startsWith(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX)) return paramvalue;
        EditableReportSecretColValueBean secretColvalueBean=EditableReportSecretColValueBean.loadFromSession(rrequest,rbean);
        if(secretColvalueBean==null)
        {
            throw new WabacusRuntimeException("session过期，无法获取到"+paramname+"参数的真正参数值");
        }
        if(secretColvalueBean.containsColkey(paramvalue))
        {
            paramvalue=secretColvalueBean.getParamValue(paramvalue);
        }
        return paramvalue;
    }
    
    public boolean isReadonlyAccessMode(IEditableReportType reportTypeObj)
    {
        ReportBean rbean=((AbsReportType)reportTypeObj).getReportBean();
        ReportRequest rrequest=((AbsReportType)reportTypeObj).getReportRequest();
        String isReadonlyAccessmode=rrequest.getStringAttribute(rbean.getId()+"_isReadonlyAccessmode","");
        if(isReadonlyAccessmode.equals(""))
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId()+"_ACCESSMODE",reportTypeObj.getDefaultAccessMode()).toLowerCase();
            if(accessmode.equals(Consts.READONLY_MODE)||rrequest.checkPermission(rbean.getId(),null,null,"readonly")||rbean.getSbean()==null)
            {
                isReadonlyAccessmode="true";//当前是以只读模式访问此编辑报表
            }else
            {
                EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
                if(ersqlbean==null||(ersqlbean.getDeletebean()==null&&ersqlbean.getInsertbean()==null&&ersqlbean.getUpdatebean()==null))
                {
                    isReadonlyAccessmode="true";
                }else
                {
                    isReadonlyAccessmode="false";
                }
            }
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode",isReadonlyAccessmode);
        }
        return Boolean.valueOf(isReadonlyAccessmode);
    }

    public int comareEditableReportSaveOrder(IEditableReportType editableReportObj1,IEditableReportType editableReportObj2)
    {
        if(editableReportObj1==null&&editableReportObj2==null) return 0;
        if(editableReportObj1==null) return -1;
        if(editableReportObj2==null) return 1;
        int saveorder1=editableReportObj1.getReportRequest().getIntAttribute(editableReportObj1.getReportBean().getId()+"_SAVE_ORDER",0);
        int saveorder2=editableReportObj2.getReportRequest().getIntAttribute(editableReportObj2.getReportBean().getId()+"_SAVE_ORDER",0);
        if(saveorder1==saveorder2) return 0;
        return saveorder1>saveorder2?1:-1;
    }
    
    public void doAllReportsSaveAction(ReportRequest rrequest)
    {
        String flag=rrequest.getStringAttribute("WX_HAS_SAVING_DATA_"+rrequest.getPagebean().getId(),"");
        if(flag.equals("true")) return;
        rrequest.setAttribute("WX_HAS_SAVING_DATA_"+rrequest.getPagebean().getId(),"true");
        Map<Object,Object> mAtts=new HashMap<Object,Object>();
        mAtts.putAll(rrequest.getAttributes());
        Object objKeyTmp,objValueTmp;
        String keyTmp,valueTmp;
        List<IEditableReportType> lstSavingReportObjs=new ArrayList<IEditableReportType>();
        String reportidTmp;
        List<String> lstReportidsTmp=new ArrayList<String>();//存放已经处理过的报表ID，以免重复初始化
        for(Entry<Object,Object> entryTmp:mAtts.entrySet())
        {
            objKeyTmp=entryTmp.getKey();
            objValueTmp=entryTmp.getValue();
            if(!(objKeyTmp instanceof String)||!(objValueTmp instanceof String)) continue;
            keyTmp=(String)objKeyTmp;
            if(keyTmp.endsWith("_INSERTDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_INSERTDATAS".length());
            }else if(keyTmp.endsWith("_UPDATEDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_UPDATEDATAS".length());
            }else if(keyTmp.endsWith("_DELETEDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_DELETEDATAS".length());
            }else if(keyTmp.endsWith("_CUSTOMIZEDATAS"))
            {
                reportidTmp=keyTmp.substring(0,keyTmp.length()-"_CUSTOMIZEDATAS".length());
            }else
            {
                continue;
            }
            if(reportidTmp.trim().equals("")||lstReportidsTmp.contains(reportidTmp.trim())) continue;
            lstReportidsTmp.add(reportidTmp.trim());
            ReportBean rbeanTmp=rrequest.getPagebean().getReportChild(reportidTmp,true);
            if(rbeanTmp==null) continue;
            valueTmp=(String)objValueTmp;
            if(valueTmp.trim().equals("")) continue;
            Object objTmp=rrequest.getComponentTypeObj(rbeanTmp,null,true);
            if(!(objTmp instanceof IEditableReportType)) continue;
            if(isReadonlyAccessMode((IEditableReportType)objTmp)) continue;
            lstSavingReportObjs.add((IEditableReportType)objTmp);
            initEditedParams(rrequest,rbeanTmp);
        }
        if(lstSavingReportObjs.size()>0)
        {
            if(lstSavingReportObjs.size()>1) Collections.sort(lstSavingReportObjs);
            EditableReportSqlBean ersqlbeanTmp;
            ReportBean rbeanTmp;
            CacheDataBean cdbTmp;
            for(IEditableReportType reportTypeObjTmp:lstSavingReportObjs)
            {
                rbeanTmp=reportTypeObjTmp.getReportBean();
                cdbTmp=rrequest.getCdb(rbeanTmp.getId());
                ersqlbeanTmp=(EditableReportSqlBean)rbeanTmp.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
                initEditExternalValues(rrequest,ersqlbeanTmp.getInsertbean(),cdbTmp,ersqlbeanTmp);
                initEditExternalValues(rrequest,ersqlbeanTmp.getUpdatebean(),cdbTmp,ersqlbeanTmp);
                initEditExternalValues(rrequest,ersqlbeanTmp.getDeletebean(),cdbTmp,ersqlbeanTmp);
            }
            rrequest.removeAttribute(rrequest.getPagebean().getId()+"_ALL_EDITPARAM_VALUES");//清除掉缓存中所有变量的值
            doSaveAction(rrequest,lstSavingReportObjs);
        }
    }

    private void initEditExternalValues(ReportRequest rrequest,AbsEditableReportEditDataBean editbean,CacheDataBean cdb,
            EditableReportSqlBean ersqlbean)
    {
        if(editbean==null) return;
        List<Map<String,String>> lstEditColDataTmp=cdb.getLstEditedData(editbean);
        if(lstEditColDataTmp==null||lstEditColDataTmp.size()==0) return;
        cdb.setLstEditedParamValues(editbean,getExternalValues(editbean,lstEditColDataTmp,rrequest));
    }
    
    private void initEditedParams(ReportRequest rrequest,ReportBean reportbean)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)reportbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        CacheDataBean cdb=rrequest.getCdb(reportbean.getId());
        boolean[] shouldDoSave=new boolean[4];
        SaveInfoDataBean sidbean=new SaveInfoDataBean();
        sidbean.setShouldDoSave(shouldDoSave);
        List<Map<String,String>> lstParamsValue=parseSaveDataStringToList(rrequest.getStringAttribute(reportbean.getId()+"_CUSTOMIZEDATAS",""));
        if(lstParamsValue!=null&&lstParamsValue.size()>0)
        {
            Map<String,String> mCustomizeData=lstParamsValue.get(0);
            cdb.getAttributes().put("WX_UPDATE_CUSTOMIZEDATAS",mCustomizeData);
            shouldDoSave[3]=true;
            if(mCustomizeData!=null&&mCustomizeData.containsKey("WX_UPDATETYPE"))
            {
                sidbean.setUpdatetype(mCustomizeData.get("WX_UPDATETYPE"));
            }else
            {
                sidbean.setUpdatetype("");
            }
        }else
        {
            shouldDoSave[3]=false;
        }
        shouldDoSave[0]=initEditedParams(rrequest.getStringAttribute(reportbean.getId()+"_INSERTDATAS",""),rrequest,reportbean,ersqlbean
                .getInsertbean());//初始化传过来的添加数据列表
        shouldDoSave[1]=initEditedParams(rrequest.getStringAttribute(reportbean.getId()+"_UPDATEDATAS",""),rrequest,reportbean,ersqlbean
                .getUpdatebean());
        shouldDoSave[2]=initEditedParams(rrequest.getStringAttribute(reportbean.getId()+"_DELETEDATAS",""),rrequest,reportbean,ersqlbean
                .getDeletebean());
        rrequest.setAttribute(reportbean.getId(),"SAVEINFO_DATABEAN",sidbean);
    }

    private boolean initEditedParams(String strParams,ReportRequest rrequest,ReportBean reportbean,AbsEditableReportEditDataBean editbean)
    {
        if(strParams.equals("")||editbean==null) return false;
        log.debug(strParams);
        List<Map<String,String>> lstParamsValue=parseSaveDataStringToList(strParams);
        if(lstParamsValue==null||lstParamsValue.size()==0) return false;
        CacheDataBean cdb=rrequest.getCdb(reportbean.getId());
        cdb.setLstEditedData(editbean,lstParamsValue);
        if(!(editbean instanceof EditableReportDeleteDataBean))
        {
            List<ColBean> lstCBeans=reportbean.getDbean().getLstCols();
            EditableReportColBean ecbeanTmp;
            for(Map<String,String> mParamValuesTmp:lstParamsValue)
            {
                for(ColBean cbTmp:lstCBeans)
                {
                    ecbeanTmp=(EditableReportColBean)cbTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
                    if(ecbeanTmp!=null&&ecbeanTmp.getServerValidateBean()!=null)
                    {//依次对每个配置了服务器端校验的列进行服务器端校验
                        ColBean cbDest=cbTmp.getUpdateColBeanDest(false);
                        if(cbDest==null) cbDest=cbTmp;
                        if(!ecbeanTmp.getServerValidateBean().validate(rrequest,mParamValuesTmp.get(cbDest.getProperty()),mParamValuesTmp,
                                new ArrayList<String>(),null)) return false;
                    }
                }
            }
        }
        return true;
    }

    public List<Map<String,String>> parseSaveDataStringToList(String strSavedata)
    {
        if(strSavedata==null||strSavedata.trim().equals("")) return null;
        List<String> lstRowDatas=Tools.parseStringToList(strSavedata.trim(),Consts_Private.SAVE_ROWDATA_SEPERATOR,false);
        List<Map<String,String>> lstResults=new ArrayList<Map<String,String>>();
        for(String rowdataTmp:lstRowDatas)
        {
            if(rowdataTmp==null||rowdataTmp.trim().equals("")) continue;
            Map<String,String> mRowData=new HashMap<String,String>();
            List<String> lstColsData=Tools.parseStringToList(rowdataTmp,Consts_Private.SAVE_COLDATA_SEPERATOR,false);
            String colnameTmp;
            String colvalueTmp;
            for(String coldataTmp:lstColsData)
            {
                if(coldataTmp==null||coldataTmp.trim().equals("")) continue;
                int idx=coldataTmp.indexOf(Consts_Private.SAVE_NAMEVALUE_SEPERATOR);
                if(idx<=0) continue;
                colnameTmp=coldataTmp.substring(0,idx).trim();
                colvalueTmp=coldataTmp.substring(idx+Consts_Private.SAVE_NAMEVALUE_SEPERATOR.length());
                if(colnameTmp.equals("")) continue;
                if("[W-X-N-U-L-L]".equals(colvalueTmp)) colvalueTmp=null;
                mRowData.put(colnameTmp,colvalueTmp);
            }
            if(mRowData.size()>0) lstResults.add(mRowData);
        }
        return lstResults;
    }

    private void doSaveAction(ReportRequest rrequest,List<IEditableReportType> lstSavingReportObjs)
    {
        if(lstSavingReportObjs==null||lstSavingReportObjs.size()==0) return;
        rrequest.setTransactionObj(new DefaultTransactionType());
        List<ReportBean> lstSaveReportBeans=new ArrayList<ReportBean>();
        for(IEditableReportType reportTypeObjTmp:lstSavingReportObjs)
        {
            lstSaveReportBeans.add(reportTypeObjTmp.getReportBean());
        }
        List<AbsPageInterceptor> lstPageInterceptors=rrequest.getLstPageInterceptors();
        if(lstPageInterceptors!=null&&lstPageInterceptors.size()>0)
        {//页面拦截器
            for(AbsPageInterceptor pageInterceptorObjTmp:lstPageInterceptors)
            {
                pageInterceptorObjTmp.doStartSave(rrequest,lstSaveReportBeans);
            }
        }
        List<AbsUpdateAction> lstAllUpdateActions=new ArrayList<AbsUpdateAction>();
        for(IEditableReportType reportTypeObjTmp:lstSavingReportObjs)
        {
            reportTypeObjTmp.collectEditActionGroupBeans(lstAllUpdateActions);
        }
        if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().beginTransaction(rrequest,lstAllUpdateActions);
        boolean hasInsertData=false,hasUpdateData=false,hasDeleteData=false;
        boolean isFailed=false,hasSaveReport=false;
        boolean shouldStopRefreshDisplay=false;
        int[] resultTmp=null;
        try
        {
            ReportBean rbeanTmp;
            for(IEditableReportType reportTypeObjTmp:lstSavingReportObjs)
            {//依次保存所有报表数据
                rbeanTmp=((AbsReportType)reportTypeObjTmp).getReportBean();
                resultTmp=reportTypeObjTmp.doSaveAction();
                if(resultTmp==null||resultTmp.length!=2||resultTmp[0]==IInterceptor.WX_RETURNVAL_SKIP) continue;
                if(resultTmp[0]==IInterceptor.WX_RETURNVAL_TERMINATE)
                {
                    rrequest.getTransactionWrapper().rollbackTransaction(rrequest,lstAllUpdateActions);
                    rrequest.getWResponse().terminateResponse(Consts.STATECODE_FAILED);
                    return;
                }
                rrequest.getWResponse().addUpdateReportGuid(rbeanTmp.getGuid());//将此报表的guid加到本次更新的报表guid列表中（因为加的过程中会判断重复，所以这里不用判断）
                if(resultTmp[0]==IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH) shouldStopRefreshDisplay=true;
                if(resultTmp[1]==IEditableReportType.IS_ADD_DATA)
                {
                    hasInsertData=true;
                }else if(resultTmp[1]==IEditableReportType.IS_UPDATE_DATA)
                {
                    hasUpdateData=true;
                }else if(resultTmp[1]==IEditableReportType.IS_ADD_UPDATE_DATA)
                {
                    hasInsertData=true;
                    hasUpdateData=true;
                }else if(resultTmp[1]==IEditableReportType.IS_DELETE_DATA)
                {
                    hasDeleteData=true;
                }
                hasSaveReport=true;
            }
            if(lstPageInterceptors!=null&&lstPageInterceptors.size()>0)
            {
                AbsPageInterceptor pageInterceptorObjTmp;
                for(int i=lstPageInterceptors.size()-1;i>=0;i--)
                {
                    pageInterceptorObjTmp=lstPageInterceptors.get(i);
                    pageInterceptorObjTmp.doEndSave(rrequest,lstSaveReportBeans);
                }
            }
            if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().commitTransaction(rrequest,lstAllUpdateActions);
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
                }
            }
            throw new WabacusRuntimeTerminateException();
        }catch(Exception e)
        {
            isFailed=true;
            if(rrequest.getTransactionWrapper()!=null) rrequest.getTransactionWrapper().rollbackTransaction(rrequest,lstAllUpdateActions);
            log.error("保存页面"+rrequest.getPagebean().getId()+"上的报表数据失败",e);
            if(resultTmp!=null&&resultTmp.length==2)
            {//保存时抛出异常时，没有执行上面的为hasInsertData,hasUpdateData,hasDeleteData的赋值操作，所以在这里赋值
                if(resultTmp[1]==1)
                {
                    hasInsertData=true;
                }else if(resultTmp[1]==2)
                {
                    hasUpdateData=true;
                }else if(resultTmp[1]==3)
                {
                    hasInsertData=true;
                    hasUpdateData=true;
                }else if(resultTmp[1]==4)
                {
                    hasDeleteData=true;
                }
            }
            if(!rrequest.isDisableAutoFailedPrompt()) promptFailedMessage(rrequest,hasInsertData,hasUpdateData,hasDeleteData);
        }finally
        {
            rrequest.setTransactionObj(null);
        }
        if(hasSaveReport&&!isFailed)
        {
            if(!rrequest.isDisableAutoSuccessPrompt())
            {
                promptSuccessMessage(rrequest,hasInsertData,hasUpdateData,hasDeleteData);
            }
            if(shouldStopRefreshDisplay)
            {
                rrequest.getWResponse().terminateResponse(Consts.STATECODE_NONREFRESHPAGE);
            }
        }else if(!hasSaveReport)
        {
            rrequest.getWResponse().getMessageCollector().warn(
                    rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${save.nodata.prompt}",
                            false)));
        }
    }

    private void promptFailedMessage(ReportRequest rrequest,boolean hasInsertData,boolean hasUpdateData,boolean hasDeleteData)
    {
        String errorprompt=null;
        if((hasInsertData&&hasDeleteData)||(hasUpdateData&&hasDeleteData))
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${operate.failed.prompt}",false));
        }else if(hasInsertData&&hasUpdateData)
        {//同时有增、改操作，即保存操作
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${save.failed.prompt}",false));
        }else if(hasInsertData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${insert.failed.prompt}",false));
        }else if(hasUpdateData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${update.failed.prompt}",false));
        }else if(hasDeleteData)
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${delete.failed.prompt}",false));
        }else
        {
            errorprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${operate.failed.prompt}",false));
        }
        rrequest.getWResponse().getMessageCollector().error(errorprompt,null,true);
    }

    private void promptSuccessMessage(ReportRequest rrequest,boolean hasInsertData,boolean hasUpdateData,boolean hasDeleteData)
    {
        String successprompt=null;
        if((hasInsertData&&hasDeleteData)||(hasUpdateData&&hasDeleteData))
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${operate.success.prompt}",false));
        }else if(hasInsertData&&hasUpdateData)
        {//同时有增、改操作，即保存操作
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${save.success.prompt}",false));
        }else if(hasInsertData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${insert.success.prompt}",false));
        }else if(hasUpdateData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${update.success.prompt}",false));
        }else if(hasDeleteData)
        {
            successprompt=rrequest.getI18NStringValue(Config.getInstance().getResourceString(rrequest,rrequest.getPagebean(),"${delete.success.prompt}",false));
        }else
        {
            return;
        }
        rrequest.getWResponse().getMessageCollector().success(successprompt);
    }
    
    public int processAfterSaveAction(ReportRequest rrequest,ReportBean rbean,String updatetype,int originalRtnVal)
    {
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
        if(ersqlbean.getAfterSaveAction()!=null&&ersqlbean.getAfterSaveAction().length>0)
        {
            String afterSaveActionMethod=ersqlbean.getAfterSaveActionMethod();
            if(!afterSaveActionMethod.equals(""))
            {
                StringBuffer paramsBuf=new StringBuffer();
                paramsBuf.append("{pageid:\""+rbean.getPageBean().getId()+"\"");
                paramsBuf.append(",reportid:\""+rbean.getId()+"\"");
                paramsBuf.append(",updatetype:\""+updatetype+"\"}");
                rrequest.getWResponse().addOnloadMethod(afterSaveActionMethod,paramsBuf.toString(),true);
            }
            if(ersqlbean.getAfterSaveAction().length==2&&"true".equals(ersqlbean.getAfterSaveAction()[1]))
            {
                return IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH;//中断报表数据的加载，则中断报表的后续显示
            }
        }
        return originalRtnVal;
    }
    
    public String getEditableMetaData(IEditableReportType editableReportTypeObj)
    {
        ReportBean rbean=((AbsReportType)editableReportTypeObj).getReportBean();
        StringBuilder resultBuf=new StringBuilder();
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(editableReportTypeObj))
        {
            resultBuf.append(" current_accessmode=\"").append(Consts.READONLY_MODE).append("\"");
        }else
        {
            resultBuf.append(" current_accessmode=\"").append(editableReportTypeObj.getRealAccessMode()).append("\"");
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)rbean.getSbean().getExtendConfigDataForReportType(EditableReportSqlBean.class);
            if(ersqlbean.getBeforeSaveAction()!=null&&!ersqlbean.getBeforeSaveAction().trim().equals(""))
            {
                resultBuf.append(" beforeSaveAction=\"{method:").append(ersqlbean.getBeforeSaveAction()).append("}\"");
            }
            if(ersqlbean.getDeletebean()!=null)
            {
                ReportRequest rrequest=((AbsReportType)editableReportTypeObj).getReportRequest();
                String deleteconfirmmess=ersqlbean.getDeletebean().getDeleteConfirmMessage(rrequest);
                if(deleteconfirmmess==null||deleteconfirmmess.trim().equals(""))
                {
                    deleteconfirmmess=Config.getInstance().getResourceString(null,null,"${delete.confirm.prompt}",true);
                }
                if(deleteconfirmmess!=null&&!deleteconfirmmess.trim().equals(""))
                {
                    resultBuf.append(" deleteconfirmmessage=\"").append(rrequest.getI18NStringValue(deleteconfirmmess)).append("\"");
                }
            }
            if(rbean.getDependParentId()!=null&&!rbean.getDependParentId().trim().equals(""))
            {
                if(ersqlbean.getInsertbean()!=null) addRefreshParentProperty(resultBuf,rbean,ersqlbean.getInsertbean(),"OnInsert");
                if(ersqlbean.getUpdatebean()!=null) addRefreshParentProperty(resultBuf,rbean,ersqlbean.getUpdatebean(),"OnUpdate");//修改时需要刷新主报表
                if(ersqlbean.getDeletebean()!=null) addRefreshParentProperty(resultBuf,rbean,ersqlbean.getDeletebean(),"OnDelete");
            }
            EditableReportBean erbean=(EditableReportBean)rbean.getExtendConfigDataForReportType(EditableReportBean.class);
            resultBuf.append(" checkdirtydata=\"").append(erbean==null||erbean.isCheckdirtydata()).append("\"");
            resultBuf.append(" savedatatype=\"").append(
                    (erbean==null||erbean.getSavedatatype()==null||erbean.getSavedatatype().trim().equals(""))?"":erbean.getSavedatatype()
                            .toLowerCase().trim()).append("\"");
        }
        EditableReportColBean ercbeanTmp;
        for(ColBean cbeanTmp:rbean.getDbean().getLstCols())
        {
            if(cbeanTmp.isControlCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbeanTmp.getExtendConfigDataForReportType(EditableReportColBean.class);
            if(ercbeanTmp==null) continue;
            if(!Tools.isEmpty(ercbeanTmp.getOngetvalueMethod()))
            {
                resultBuf.append(" "+cbeanTmp.getProperty()+"_ongetvaluemethods=\"{methods:"+ercbeanTmp.getOngetvalueMethod()+"}\"");
            }
            if(!Tools.isEmpty(ercbeanTmp.getOnsetvalueMethod()))
            {
                resultBuf.append(" "+cbeanTmp.getProperty()+"_onsetvaluemethods=\"{methods:"+ercbeanTmp.getOnsetvalueMethod()+"}\"");
            }
        }            
        return resultBuf.toString();
    }

    private void addRefreshParentProperty(StringBuilder resultBuf,ReportBean rbean,AbsEditableReportEditDataBean editBean,String propertySuffix)
    {
        String refreshedParentid=editBean.getRefreshParentidOnSave();
        if(refreshedParentid==null||refreshedParentid.trim().equals("")) return;
        ReportBean rbeanMaster=rbean.getPageBean().getReportChild(refreshedParentid,true);
        if(rbeanMaster==null)
        {
            throw new WabacusRuntimeException("ID为"+refreshedParentid+"的报表不存在");
        }
        if(!rbean.isMasterReportOfMe(rbeanMaster,true))
        {
            throw new WabacusRuntimeException("显示报表"+rbean.getPath()+"失败，ID为"+refreshedParentid+"的报表不是ID为"+rbean.getId()+"报表的主报表");
        }
        resultBuf.append(" refreshParentReportid"+propertySuffix+"=\"").append(refreshedParentid).append("\"");
        resultBuf.append(" refreshParentReportType"+propertySuffix+"=\"").append(editBean.isResetNavigateInfoOnRefreshParent()).append("\"");
    }

    public int doSaveReport(ReportRequest rrequest,ReportBean rbean,AbsEditableReportEditDataBean editbean)
    {
        CacheDataBean cdb=rrequest.getCdb(rbean.getId());
        List<Map<String,String>> lstRowData=cdb.getLstEditedData(editbean);
        List<Map<String,String>> lstParamValues=cdb.getLstEditedParamValues(editbean);
        Map<String,String> mRowData, mParamValues;//分别存放要操作的各列数据以及<params/>中定义的变量的数据
        boolean hasSaveData=false, hasNonRefreshReport=false;
        if(lstRowData==null||lstRowData.size()==0)
        {
            if(editbean instanceof EditableReportSQLButtonDataBean)
            {//如果是直接配置更新脚本的<button/>
                EditableReportSQLButtonDataBean buttonEditBean=(EditableReportSQLButtonDataBean)editbean;
                if(!buttonEditBean.isAutoReportdata()&&!buttonEditBean.isHasReportDataParams())
                {//如果这个<button/>不是自动取报表数据，且所有更新脚本不需要取@{param}的数据进行操作，则即使没有记录也要执行，所以如果有参数的话也要初始化参数
                    mParamValues=lstParamValues!=null&&lstParamValues.size()>0?lstParamValues.get(0):null;
                    int rtnVal;
                    if(rbean.getInterceptor()!=null)
                    {
                        rtnVal=rbean.getInterceptor().doSavePerRow(rrequest,rbean,null,mParamValues,editbean);
                    }else
                    {
                        rtnVal=doSaveRow(rrequest,rbean,null,mParamValues,editbean);
                    }
                    if(rtnVal==IInterceptor.WX_RETURNVAL_TERMINATE||rtnVal==IInterceptor.WX_RETURNVAL_SKIP) return rtnVal;
                    hasSaveData=true;
                    if(rtnVal==IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH) hasNonRefreshReport=true;
                }
            }
        }else
        {
            for(int i=0;i<lstRowData.size();i++)
            {
                mRowData=lstRowData.get(i);
                mParamValues=lstParamValues!=null&&lstParamValues.size()>i?lstParamValues.get(i):null;
                rrequest.setAttribute(rbean.getId(),"update_datarow_index",String.valueOf(i));
                int rtnVal;
                if(rbean.getInterceptor()!=null)
                {
                    rtnVal=rbean.getInterceptor().doSavePerRow(rrequest,rbean,mRowData,mParamValues,editbean);
                }else
                {
                    rtnVal=doSaveRow(rrequest,rbean,mRowData,mParamValues,editbean);
                }
                if(rtnVal==IInterceptor.WX_RETURNVAL_TERMINATE) return rtnVal;
                if(rtnVal==IInterceptor.WX_RETURNVAL_SKIP) continue;
                hasSaveData=true;
                if(rtnVal==IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH) hasNonRefreshReport=true;
            }
        }
        if(!hasSaveData) return IInterceptor.WX_RETURNVAL_SKIP;
        if(hasNonRefreshReport) return IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH;
        return IInterceptor.WX_RETURNVAL_SUCCESS;
    }

    public int doSaveRow(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,
            AbsEditableReportEditDataBean editbean)
    {
        List<AbsUpdateAction> lstAllExecuteEditActions=editbean.getLstAllExecuteEditActions(rrequest,mRowData,mParamValues);
        if(lstAllExecuteEditActions==null||lstAllExecuteEditActions.size()==0) return IInterceptor.WX_RETURNVAL_SKIP;//没有要执行的保存动作
        boolean hasSaveData=false, hasNonRefreshReport=false;
        for(AbsUpdateAction actionTmp:lstAllExecuteEditActions)
        {
            int rtnVal;
            if(rbean.getInterceptor()!=null)
            {
                rtnVal=rbean.getInterceptor().doSavePerAction(rrequest,rbean,mRowData,mParamValues,actionTmp,editbean);
            }else
            {
                rtnVal=doSavePerAction(rrequest,rbean,mRowData,mParamValues,actionTmp,editbean);
            }
            if(rtnVal==IInterceptor.WX_RETURNVAL_TERMINATE) return rtnVal;
            if(rtnVal==IInterceptor.WX_RETURNVAL_SKIP) continue;
            hasSaveData=true;
            if(rtnVal==IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH) hasNonRefreshReport=true;
        }
        if(!hasSaveData) return IInterceptor.WX_RETURNVAL_SKIP;
        if(hasNonRefreshReport) return IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH;
        return IInterceptor.WX_RETURNVAL_SUCCESS;
    }
    
    public int doSavePerAction(ReportRequest rrequest,ReportBean rbean,Map<String,String> mRowData,Map<String,String> mParamValues,
            AbsUpdateAction action,AbsEditableReportEditDataBean editbean)
    {
        try
        {
            action.updateData(rrequest,mRowData,mParamValues);
        }catch(SQLException e)
        {
            throw new WabacusRuntimeException("保存报表"+rbean.getPath()+"数据失败",e);
        }
        return IInterceptor.WX_RETURNVAL_SUCCESS;
    }
    
    public List<Map<String,String>> getExternalValues(AbsEditableReportEditDataBean editbean,List<Map<String,String>> lstColParamsValue,ReportRequest rrequest)
    {
        if(editbean.getLstExternalValues()==null||editbean.getLstExternalValues().size()==0) return null;
        List<Map<String,String>> lstExternalValues=new ArrayList<Map<String,String>>();
        if(lstColParamsValue==null||lstColParamsValue.size()==0)
        {
            if(editbean instanceof EditableReportSQLButtonDataBean)
            {//如果是直接配置更新脚本的<button/>
                EditableReportSQLButtonDataBean buttonEditBean=(EditableReportSQLButtonDataBean)editbean;
                if(!buttonEditBean.isAutoReportdata()&&!buttonEditBean.isHasReportDataParams())
                {//如果这个<button/>不是自动取报表数据，且所有脚本不需要取@{param}的数据进行操作，则即使没有记录也要执行，所以如果有参数的话也要初始化参数
                    lstExternalValues.add(initParamValuesForOneRowData(editbean,rrequest,null,0));
                }
            }
        }else
        {
            int i=0;
            for(Map<String,String> mRowDataTmp:lstColParamsValue)
            {//保存的每一条记录都要计算一次与它相应的所有在<params/>中定义的参数值
                lstExternalValues.add(initParamValuesForOneRowData(editbean,rrequest,mRowDataTmp,i++));
            }
        }
        return lstExternalValues.size()==0?null:lstExternalValues;
    }

    private Map<String,String> initParamValuesForOneRowData(AbsEditableReportEditDataBean editbean,ReportRequest rrequest,Map<String,String> mRowData,int rowidx)
    {
        Map<String,String> mExternalValue=new HashMap<String,String>();
        Map<String,String> mCustomizedValues=rrequest.getMCustomizeEditData(editbean.getOwner().getReportBean());//用户传过来的自定义保存数据
        Map<String,String> mAllParamValues=(Map<String,String>)rrequest.getAttribute(rrequest.getPagebean().getId()+"_ALL_EDITPARAM_VALUES");
        if(mAllParamValues==null)
        {
            mAllParamValues=new HashMap<String,String>();
            rrequest.setAttribute(rrequest.getPagebean().getId()+"_ALL_EDITPARAM_VALUES",mAllParamValues);
        }
        String valueTmp;
        for(EditableReportExternalValueBean paramBeanTmp:editbean.getLstExternalValues())
        {
            valueTmp=mAllParamValues.get(paramBeanTmp.getObjectId()+"_"+rowidx);
            if(valueTmp==null||valueTmp.equals(""))
            {
                valueTmp=paramBeanTmp.getParamValue(rrequest,mRowData,mCustomizedValues,mExternalValue);
                mAllParamValues.put(paramBeanTmp.getObjectId()+"_"+rowidx,valueTmp);
            }
            mExternalValue.put(paramBeanTmp.getName(),valueTmp);
        }
        return mExternalValue;
    }
    
    public String getParamValueOfOneRowData(AbsEditableReportEditDataBean editbean,EditableReportExternalValueBean paramBean,ReportRequest rrequest,
            int rowidx)
    {
        Map<String,String> mAllParamValues=(Map<String,String>)rrequest.getAttribute(rrequest.getPagebean().getId()+"_ALL_EDITPARAM_VALUES");
        if(mAllParamValues==null)
        {
            mAllParamValues=new HashMap<String,String>();
            rrequest.setAttribute(rrequest.getPagebean().getId()+"_ALL_EDITPARAM_VALUES",mAllParamValues);
        }
        String valueTmp=mAllParamValues.get(paramBean.getObjectId()+"_"+rowidx);
        if(valueTmp==null||valueTmp.equals(""))
        {
            List<Map<String,String>> lstAllRowsData=rrequest.getCdb(editbean.getOwner().getReportBean().getId()).getLstEditedData(editbean);
            valueTmp=paramBean.getParamValue(rrequest,lstAllRowsData!=null&&lstAllRowsData.size()>0?lstAllRowsData.get(0):null,rrequest
                    .getMCustomizeEditData(editbean.getOwner().getReportBean()),null);
            mAllParamValues.put(paramBean.getObjectId()+"_"+rowidx,valueTmp);
        }
        return valueTmp;
    }
    
    public String parseStandardEditSql(ReportBean rbean,String sql,List<EditableReportParamBean> lstDynParams,boolean isPreparedstatement,
            boolean includeQuote)
    {
        if(sql==null||sql.trim().equals("")) return "";
        sql=sql.trim();
        if(!includeQuote)
        {
            sql=Tools.replaceCharacterInQuote(sql,'{',"$_LEFTBRACKET_$",true);
            sql=Tools.replaceCharacterInQuote(sql,'}',"$_RIGHTBRACKET_$",true);
        }
        Map<String,EditableReportParamBean> mDynParamsAndPlaceHolder=new HashMap<String,EditableReportParamBean>();//存放每个动态参数对象及其对应的占位符，其中占位符为键
        sql=parseCertainTypeDynParamsInStandardSql(rbean,sql,mDynParamsAndPlaceHolder,"url",isPreparedstatement);
        sql=parseCertainTypeDynParamsInStandardSql(rbean,sql,mDynParamsAndPlaceHolder,"request",isPreparedstatement);
        sql=parseCertainTypeDynParamsInStandardSql(rbean,sql,mDynParamsAndPlaceHolder,"session",isPreparedstatement);
        sql=parseCertainTypeDynParamsInStandardSql(rbean,sql,mDynParamsAndPlaceHolder,"@",isPreparedstatement);
        sql=parseCertainTypeDynParamsInStandardSql(rbean,sql,mDynParamsAndPlaceHolder,"!",isPreparedstatement);//解析条件子句中的!{...}动态参数
        sql=parseCertainTypeDynParamsInStandardSql(rbean,sql,mDynParamsAndPlaceHolder,"#",isPreparedstatement);
        if(isPreparedstatement)
        {
            sql=convertPlaceHolderToRealParams(sql,mDynParamsAndPlaceHolder,lstDynParams);
        }else
        {
            for(Entry<String,EditableReportParamBean> entryTmp:mDynParamsAndPlaceHolder.entrySet())
            {
                entryTmp.getValue().setPlaceholder(entryTmp.getKey());//设置此参数对应的占位符
                lstDynParams.add(entryTmp.getValue());
            }
        }
        if(!includeQuote)
        {
            sql=Tools.replaceAll(sql,"$_LEFTBRACKET_$","{");
            sql=Tools.replaceAll(sql,"$_RIGHTBRACKET_$","}");
        }
        return sql;
    }
    
    private String parseCertainTypeDynParamsInStandardSql(ReportBean rbean,String sql,Map<String,EditableReportParamBean> mDynParamsAndPlaceHolder,
            String paramtype,boolean isPreparedstatement)
    {
        String strStart,strDynValue,strEnd,placeHolderTmp;
        EditableReportParamBean paramBeanTmp;
        int placeholderIdxTmp=10000;
        int idx=sql.indexOf(paramtype+"{");
        while(idx>=0)
        {
            strStart=sql.substring(0,idx).trim();
            strEnd=sql.substring(idx);
            idx=strEnd.indexOf("}");
            if(idx<0)
            {
                throw new WabacusConfigLoadingException("加载组件"+rbean.getPath()+"下的SQL语句"+sql+"失败，其中动态参数没有闭合的}");
            }
            strDynValue=strEnd.substring(0,idx+1);
            strEnd=strEnd.substring(idx+1).trim();//存放type{...}后面的部分
            paramBeanTmp=new EditableReportParamBean();
            paramBeanTmp.setParamname(strDynValue);
            if("url".equals(paramtype)) rbean.addParamNameFromURL(Tools.getRealKeyByDefine("url",strDynValue));
            if(isPreparedstatement)
            {
                if((strStart.endsWith("%")&&strStart.substring(0,strStart.length()-1).trim().toLowerCase().endsWith(" like"))
                        ||strStart.toLowerCase().endsWith(" like"))
                {
                    if(strStart.endsWith("%"))
                    {
                        strStart=strStart.substring(0,strStart.length()-1);
                        paramBeanTmp.setHasLeftPercent(true);
                    }
                    if(strEnd.startsWith("%"))
                    {
                        strEnd=strEnd.substring(1);
                        paramBeanTmp.setHasRightPercent(true);
                    }
                }
            }
            placeHolderTmp="[PLACE_HOLDER_"+paramtype+"_"+placeholderIdxTmp+"]";
            mDynParamsAndPlaceHolder.put(placeHolderTmp,paramBeanTmp);
            sql=strStart+placeHolderTmp+strEnd;
            idx=sql.indexOf(paramtype+"{");
            placeholderIdxTmp++;
        }
        return sql;
    }
    
    private String convertPlaceHolderToRealParams(String sql,Map<String,EditableReportParamBean> mDynParamsAndPlaceHolder,
            List<EditableReportParamBean> lstDynParams)
    {
        if(mDynParamsAndPlaceHolder==null||mDynParamsAndPlaceHolder.size()==0) return sql;
        int idxPlaceHolderStart=sql.indexOf("[PLACE_HOLDER_");
        String strStart=null;
        String strEnd=null;
        String placeHolderTmp;
        while(idxPlaceHolderStart>=0)
        {
            strStart=sql.substring(0,idxPlaceHolderStart);//占位符前面部分
            strEnd=sql.substring(idxPlaceHolderStart);
            int idxPlaceHolderEnd=strEnd.indexOf("]");
            placeHolderTmp=strEnd.substring(0,idxPlaceHolderEnd+1);
            strEnd=strEnd.substring(idxPlaceHolderEnd+1);
            lstDynParams.add(mDynParamsAndPlaceHolder.get(placeHolderTmp));
            sql=strStart+" ? "+strEnd;
            idxPlaceHolderStart=sql.indexOf("[PLACE_HOLDER_");
        }
        return sql;
    }

    private Map<String,Long> mAllAutoIncrementIdValues=new HashMap<String,Long>();
    
    public synchronized String getAutoIncrementIdValue(ReportRequest rrequest,ReportBean rbean,String datasource,String paramname)
    {
        if(paramname==null||paramname.trim().equals("")) return "-1";
        if(datasource==null) datasource="";
        String key=datasource+"__"+paramname;
        Long lid=mAllAutoIncrementIdValues.get(key);
        if(lid==null||lid.longValue()<0)
        {
            String realparamname=Tools.getRealKeyByDefine("increment",paramname);
            int idx=realparamname.indexOf(".");
            if(idx<0)
            {
                throw new WabacusRuntimeException("为报表"+rbean.getPath()+"配置的自动增长字段"+paramname+"不合法，没有指定表名和自动增长字段名，并用.分隔");
            }
            String tablename=realparamname.substring(0,idx).trim();
            String columnname=realparamname.substring(idx+1).trim();
            if(tablename.trim().equals("")||columnname.trim().equals(""))
            {
                throw new WabacusRuntimeException("为报表"+rbean.getPath()+"配置的自动增长字段"+paramname+"不合法，没有指定表名或自动增长字段名");
            }
            String sid="-1";
            Connection conn=rrequest.getConnection(datasource);
            Statement stmt=null;
            ResultSet rs=null;
            String sql="select max("+columnname+") from "+tablename;
            try
            {
                stmt=conn.createStatement();
                rs=stmt.executeQuery(sql);
                if(rs.next())
                {
                    sid=rs.getString(1);
                }
            }catch(SQLException e)
            {
                throw new WabacusRuntimeException("获取报表"+rbean.getPath()+"配置的自动增长字段"+realparamname+"失败",e);
            }finally
            {
                try
                {
                    if(rs!=null) rs.close();
                }catch(SQLException e)
                {
                    e.printStackTrace();
                }
                WabacusAssistant.getInstance().release(null,stmt);
            }
            lid=Long.valueOf(sid);
        }
        mAllAutoIncrementIdValues.put(key,++lid);
        return String.valueOf(lid);
    }
}
