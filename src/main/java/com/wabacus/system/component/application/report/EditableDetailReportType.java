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
package com.wabacus.system.component.application.report;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.CancelButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.ResetButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.buttons.UpdateButton;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportInsertDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportUpdateDataBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.inputbox.CustomizedBox;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableDetailReportType extends DetailReportType implements IEditableReportType
{
    protected EditableReportSqlBean ersqlbean=null;
    
    protected String realAccessMode;
    
    public final static String KEY=EditableDetailReportType.class.getName();

    private static Log log=LogFactory.getLog(EditableDetailReportType.class);

    private final static List<String> LST_ALL_ACCESSMODE=new ArrayList<String>();

    static
    {
        LST_ALL_ACCESSMODE.add(Consts.ADD_MODE);
        LST_ALL_ACCESSMODE.add(Consts.READ_MODE);
        LST_ALL_ACCESSMODE.add(Consts.UPDATE_MODE);
        LST_ALL_ACCESSMODE.add(Consts.READONLY_MODE);
    }

    public EditableDetailReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            this.ersqlbean=(EditableReportSqlBean)((ReportBean)comCfgBean).getSbean().getExtendConfigDataForReportType(KEY);
        }
    }
    
    public List<String> getLstAllAccessModes()
    {
        return LST_ALL_ACCESSMODE;
    }

    protected boolean getDefaultShowContextMenu()
    {
        return true;
    }

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        super.initUrl(applicationConfigBean,rrequest);
        String accessmode=rrequest.getStringAttribute(applicationConfigBean.getId()+"_ACCESSMODE",getDefaultAccessMode()).toLowerCase();
        //        /**
        //         * 其它访问模式不保留到URL中，因为它们可以通过“添加”，“修改”等按钮切换进来，而readonly访问模式不能与其它模式切换，因此当用户想以readonly方式访问，则后续都是以这个访问。
        //         */
        //        }
        if(!accessmode.equals(""))
        {
            rrequest.addParamToUrl(applicationConfigBean.getId()+"_ACCESSMODE",accessmode,true);
        }
        String referedReportIdByEditablelist=rrequest.getStringAttribute("WX_REFEREDREPORTID","");
        if(referedReportIdByEditablelist.equals(applicationConfigBean.getId()))
        {
            rrequest.addParamToUrl("WX_REFEREDREPORTID","rrequest{WX_REFEREDREPORTID}",true);
            rrequest.addParamToUrl("WX_SRCPAGEID","rrequest{WX_SRCPAGEID}",true);
            rrequest.addParamToUrl("WX_SRCREPORTID","rrequest{WX_SRCREPORTID}",true);
            rrequest.addParamToUrl("WX_EDITTYPE","rrequest{WX_EDITTYPE}",true);
        }
    }
    
    protected void initReportBeforeDoStart()
    {
        super.initReportBeforeDoStart();
        String referedReportIdByEditablelist=rrequest.getStringAttribute("WX_REFEREDREPORTID","");
        if(referedReportIdByEditablelist.equals(rbean.getId()))
        {
            rrequest.authorize(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts_Private.CANCEL_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");
            String edittype=rrequest.getStringAttribute("WX_EDITTYPE","");
            if(edittype.equals(Consts.UPDATE_MODE))
            {
                rrequest.authorize(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts_Private.ADD_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");//添加按钮将不显示出来
                rrequest.authorize(rbean.getId(),Consts.BUTTON_PART,"type{"+Consts_Private.DELETE_BUTTON+"}",Consts.PERMISSION_TYPE_DISPLAY,"false");
            }
        }
    }

    public void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        String accessmode=null;
        if(!EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            EditableReportAssistant.getInstance().doAllReportsSaveAction(rrequest);
            SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
            if(sidbean!=null&&(sidbean.hasSavingData()||sidbean.hasDeleteData()))
            {
                accessmode=getDefaultAccessMode();
                if(accessmode==null) accessmode="";
            }
        }
        if(accessmode==null)
        {
            accessmode=rrequest.getStringAttribute(rbean.getId()+"_ACCESSMODE",getDefaultAccessMode()).toLowerCase();
            if(!getLstAllAccessModes().contains(accessmode)) accessmode=getDefaultAccessMode();//如果传入的状态不是此报表类型能接受的，则采用默认状态访问报表
        }
        setNewAccessMode(accessmode);
    }

    public void collectEditActionGroupBeans(List<AbsUpdateAction> lstAllUpdateActions)
    {
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null) return;
        if(sidbean.hasDeleteData()) lstAllUpdateActions.addAll(ersqlbean.getDeletebean().getLsAllEditActions());
        if(sidbean.hasSavingData())
        {
            boolean[] shouldDoSave=sidbean.getShouldDoSave();
            if(shouldDoSave[0]||(shouldDoSave[3]&&Consts.ADD_MODE.equals(sidbean.getUpdatetype())))
            {
                lstAllUpdateActions.addAll(ersqlbean.getInsertbean().getLsAllEditActions());
            }else if(shouldDoSave[1]||(shouldDoSave[3]&&Consts.UPDATE_MODE.equals(sidbean.getUpdatetype())))
            {
                lstAllUpdateActions.addAll(ersqlbean.getUpdatebean().getLsAllEditActions());
            }
        }
    }
    
    public int[] doSaveAction() throws SQLException
    {
        int[] result=new int[] { IInterceptor.WX_RETURNVAL_SKIP, 0 };
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null) return result;
        if(sidbean.hasDeleteData())
        {
            result[0]=updateDBData(ersqlbean.getDeletebean());
            result[1]=IEditableReportType.IS_DELETE_DATA;//只有删除操作
        }else if(sidbean.hasSavingData())
        {
            boolean[] shouldDoSave=sidbean.getShouldDoSave();
            if(shouldDoSave[0]||(shouldDoSave[3]&&Consts.ADD_MODE.equals(sidbean.getUpdatetype())))
            {
                result[0]=updateDBData(ersqlbean.getInsertbean());
                result[1]=IEditableReportType.IS_ADD_DATA;
            }else if(shouldDoSave[1]||(shouldDoSave[3]&&Consts.UPDATE_MODE.equals(sidbean.getUpdatetype())))
            {
                result[0]=updateDBData(ersqlbean.getUpdatebean());
                result[1]=IEditableReportType.IS_UPDATE_DATA;
            }
        }
        return result;
    }

    private int updateDBData(AbsEditableReportEditDataBean editbean) throws SQLException
    {
        int rtnVal=IInterceptor.WX_RETURNVAL_SKIP;
        if(editbean==null) return rtnVal;
        if(rbean.getInterceptor()!=null)
        {
            rtnVal=rbean.getInterceptor().doSave(this.rrequest,this.rbean,editbean);
        }else
        {
            rtnVal=EditableReportAssistant.getInstance().doSaveReport(this.rrequest,this.rbean,editbean);
        }
        if(rtnVal==IInterceptor.WX_RETURNVAL_TERMINATE||rtnVal==IInterceptor.WX_RETURNVAL_SKIP) return rtnVal;
        String referedReportIdByEditablelist=rrequest.getStringAttribute("WX_REFEREDREPORTID","");
        boolean shouldClosePopupPage=!"false".equalsIgnoreCase((String)rrequest.getAttribute(rbean.getId(),"closePopupPage"));
        if(referedReportIdByEditablelist.equals(rbean.getId()))
        {//如果当前报表是被其它editablelist报表引用访问，则保存完后需要关闭掉弹出窗口，并刷新父报表的显示
            String srcpageid=rrequest.getStringAttribute("WX_SRCPAGEID","");
            String srcreportid=rrequest.getStringAttribute("WX_SRCREPORTID","");
            String srcedittype=rrequest.getStringAttribute("WX_EDITTYPE","");
            StringBuffer paramsBuf=new StringBuffer();
            paramsBuf.append("{pageid:\""+srcpageid+"\"");
            paramsBuf.append(",reportid:\""+srcreportid+"\"");
            paramsBuf.append(",edittype:\""+srcedittype+"\"");
            paramsBuf.append(",closeme:\""+shouldClosePopupPage+"\"}");
            rrequest.getWResponse().addOnloadMethod("closeMeAndRefreshParentReport",paramsBuf.toString(),true);
        }
        if((!referedReportIdByEditablelist.equals(rbean.getId())||!shouldClosePopupPage)
                &&(editbean instanceof EditableReportInsertDataBean&&((EditableReportInsertDataBean)editbean).getMUpdateConditions()!=null&&((EditableReportInsertDataBean)editbean)
                        .getMUpdateConditions().size()>0))
        {
            Map<String,String> mRowData=this.cacheDataBean.getLstEditedData(editbean).get(0);
            Map<String,String> mParamValues=null;
            if(this.cacheDataBean.getLstEditedParamValues(editbean)!=null&&this.cacheDataBean.getLstEditedParamValues(editbean).size()>0)
            {
                mParamValues=this.cacheDataBean.getLstEditedParamValues(editbean).get(0);
            }
            String paramvalue;
            ColBean cbeanTmp;
            Set<Entry<String,String>> entrySetTmp=((EditableReportInsertDataBean)editbean).getMUpdateConditions().entrySet();
            for(Entry<String,String> entry:entrySetTmp)
            {
                paramvalue=entry.getValue();
                if(Tools.isDefineKey("@",paramvalue))
                {
                    paramvalue=Tools.getRealKeyByDefine("@",paramvalue);
                    cbeanTmp=rbean.getDbean().getColBeanByColProperty(paramvalue);
                    if(cbeanTmp==null) throw new WabacusRuntimeException("报表"+rbean.getPath()+"中不存在"+paramvalue+"的<col/>");
                    paramvalue=EditableReportAssistant.getInstance().getColParamValue(rrequest,mRowData,cbeanTmp);
                    if(paramvalue==null) log.warn("没有从新增的记录中获取到property为"+entry.getValue()+"的<col/>的数据");
                }else if(Tools.isDefineKey("#",paramvalue)&&mParamValues!=null)
                {//从<params/>中取值
                    paramvalue=mParamValues.get(Tools.getRealKeyByDefine("#",paramvalue));
                }else if(WabacusAssistant.getInstance().isGetRequestContextValue(paramvalue))
                {
                    paramvalue=WabacusAssistant.getInstance().getRequestContextStringValue(rrequest,paramvalue,"");
                }
                if(paramvalue==null) paramvalue="";
                rrequest.setAttribute(entry.getKey(),paramvalue);
            }
        }
        String updatetype=null;
        if(editbean instanceof EditableReportInsertDataBean)
        {
            updatetype="add";
        }else if(editbean instanceof EditableReportUpdateDataBean)
        {
            updatetype="update";
        }else
        {//当前是做“删除”操作
            updatetype="delete";
        }
        rtnVal=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,updatetype,rtnVal);
        if(referedReportIdByEditablelist.equals(rbean.getId())&&shouldClosePopupPage) rtnVal=IInterceptor.WX_RETURNVAL_SUCCESS_NOTREFRESH;
        return rtnVal;
    }

    public String getDefaultAccessMode()
    {
        return Consts.READ_MODE;
    }
    
    public String getRealAccessMode()
    {
        return this.realAccessMode;
    }
    
    public void setNewAccessMode(String newaccessmode)
    {
        if(newaccessmode==null||newaccessmode.trim().equals("")) return;
        if(!this.getLstAllAccessModes().contains(newaccessmode))
        {
            log.warn("设置报表"+rbean.getPath()+"的新访问模式为"+newaccessmode+"失败，此报表对应的报表类型不支持此访问模式");
            return;
        }
        rrequest.setAttribute(rbean.getId(),"CURRENT_ACCESSMODE",newaccessmode);
        rrequest.addParamToUrl(rbean.getId()+"_ACCESSMODE",newaccessmode,true);
        rrequest.setAttribute(rbean.getId()+"_ACCESSMODE",newaccessmode);
        if(Consts.READONLY_MODE.equals(newaccessmode))
        {
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode","true");
        }else
        {
            rrequest.getAttributes().remove(rbean.getId()+"_isReadonlyAccessmode");
        }
    }

    private boolean hasLoadedData=false;
    
    public  boolean isLoadedReportData()
    {
        return this.hasLoadedData;
    }
    
    public void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        super.setHasLoadedDataFlag(hasLoadedDataFlag);
        this.hasLoadedData=hasLoadedDataFlag;
    }
    
    protected void initLoadReportData()
    {
        super.initLoadReportData();
        if(this.rbean.isSlaveReportDependsonDetailReport()&&rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE)
        {//如果当前报表是依赖于细览报表，且当前是显示在页面上
            AbsReportType parentReportTypeObj=(AbsReportType)rrequest.getComponentTypeObj(this.rbean.getDependParentId(),null,false);
            if(parentReportTypeObj!=null&&parentReportTypeObj instanceof EditableDetailReportType &&parentReportTypeObj.getParentContainerType()!=null)
            {
                String parentRealAccessMode=((EditableDetailReportType)parentReportTypeObj).getRealAccessMode();
                if(parentRealAccessMode==Consts.ADD_MODE)
                {
                    setNewAccessMode(Consts.ADD_MODE);
                }else if(rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode()).equals(Consts.ADD_MODE))
                {
                    setNewAccessMode(getDefaultAccessMode());
                }
            }
        }
    }
    
    public void loadReportData(boolean shouldInvokePostaction)
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        initLoadReportData();//不管要不要加载数据，先初始化一下，因为如果本报表是依赖细览报表的从报表，则要为后面的刷新在URL中准备数据，因为后面可能只刷新此报表
        SqlBean sqlbean=rbean.getSbean();
        if(sqlbean.getLstDatasetBeans()!=null&&sqlbean.getLstDatasetBeans().size()>0
                &&!rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode()).equals(Consts.ADD_MODE))
        {
            super.loadReportData(false);
        }
        if(!this.isLazyDisplayData())
        {
            if(!EditableReportAssistant.getInstance().isReadonlyAccessMode(this)&&(lstReportData==null||lstReportData.size()==0))
            {
                if(ersqlbean!=null&&ersqlbean.getInsertbean()!=null)
                {
                    setNewAccessMode(Consts.ADD_MODE);
                    AbsReportDataPojo dataObj=ReportAssistant.getInstance().getPojoClassInstance(rrequest,rbean,rbean.getPojoClassObj());
                    dataObj.format();
                    dataObj.setWx_belongto_datasetid("[default_empty_dataobject]");//标识是一条默认的对象，以免它的从报表去根据此空对象去加载出所有数据
                    this.lstReportData=new ArrayList();
                    this.lstReportData.add(dataObj);
                }else
                {
                }
            }
            if(shouldInvokePostaction) doLoadReportDataPostAction();
        }
        initRealAccessMode();
    }

    protected void initRealAccessMode()
    {
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            realAccessMode=Consts.READONLY_MODE;
        }else
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId(),"CURRENT_ACCESSMODE",getDefaultAccessMode());
            if(accessmode.equals(Consts.ADD_MODE))
            {
                if(ersqlbean.getInsertbean()==null)
                {
                    throw new WabacusRuntimeException("报表"+rbean.getPath()+"没有配置<insert/>，不能进行添加操作");
                }
                realAccessMode=Consts.ADD_MODE;
                rrequest.getWResponse().addOnloadMethod("addEditableDetailReportFoSaving","{reportguid:\""+this.rbean.getGuid()+"\"}",true);
            }else if(accessmode.equals(Consts.UPDATE_MODE))
            {
                if(ersqlbean.getUpdatebean()==null)
                {//没有配置<update/>
                    realAccessMode=Consts.READ_MODE;
                }else
                {
                    realAccessMode=Consts.UPDATE_MODE;
                }
            }else
            {
                realAccessMode=Consts.READ_MODE;
            }
        }
    }
    
    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        return resultBuf.toString();
    }
    
    protected void initInputBox(StringBuilder resultBuf)
    {
        super.initInputBox(resultBuf);
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return ;
        if(Consts.READONLY_MODE.equals(this.realAccessMode)) return;
        if(this.realAccessMode.equals(Consts.ADD_MODE)&&ersqlbean.getInsertbean()==null) return;
        if(this.realAccessMode.equals(Consts.UPDATE_MODE)&&ersqlbean.getUpdatebean()==null) return;
        EditableReportColBean ercbeanTmp=null;
        for(ColBean cbean:rbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) continue;
            if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0) continue;
            if(cbean.isNonValueCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercbeanTmp==null) continue;
            if(this.realAccessMode.equals(Consts.UPDATE_MODE)&&!ercbeanTmp.isEditableForUpdate()) continue;
            if(this.realAccessMode.equals(Consts.ADD_MODE)&&!ercbeanTmp.isEditableForInsert()) continue;
            resultBuf.append(ercbeanTmp.getInputbox().initDisplay(rrequest));
        }
    }

    protected String getDataTdClassName()
    {
        return "cls-data-td-editdetail";
    }
    
    protected Object initDisplayCol(ColBean cbean,AbsReportDataPojo dataObj)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return super.initDisplayCol(cbean,dataObj);
        if(cbean.isNonValueCol()) return null;
//        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype()))
//            EditableReportColBean ercolbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
//            {//取到通过updatecol引用的隐藏列数据
//                col_editvalue=getColOriginalValue(dataObj,ercolbean.getUpdateCbean());
        AbsEditableReportEditDataBean editbean=this.realAccessMode.equals(Consts.ADD_MODE)?ersqlbean.getInsertbean():ersqlbean.getUpdatebean();
        String col_editvalue=getColOriginalValue(dataObj,cbean);
        if(col_editvalue==null) col_editvalue="";
        return EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,editbean,cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String showHiddenCol(ColBean cbeanHidden,Object colDataObj)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbeanHidden.getDisplaytype(true))) return "";
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.showHiddenCol(cbeanHidden,colDataObj);
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        StringBuilder resultBuf=new StringBuilder();
        if(cbeanHidden.getUpdateColBeanSrc(false)!=null)
        {
            resultBuf.append(this.showTempHiddenCol(cbeanHidden,ercdatabean,true));
        }else
        {
            resultBuf.append("<font name=\"font_").append(rbean.getGuid()).append("\"");
            resultBuf.append(" id=\"font_").append(rbean.getGuid()).append("\"");
            resultBuf.append(" value_name=\"").append(ercdatabean.getValuename()).append("\"");
            resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
            if(!this.realAccessMode.equals(Consts.ADD_MODE))
            {
                resultBuf.append(" oldvalue=\"").append(Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
            }
            resultBuf.append(" style=\"display:none\">");
        }
        resultBuf.append("</font>");//不能用<font/>，而必须是<font></font>格式，否则在IE上取子节点会有问题
        return resultBuf.toString();
    }
    
    protected String getColValueTdPropertiesAndContent(ColBean cbean,AbsReportDataPojo dataObj,Object colDataObj,StringBuffer tdPropsBuf)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        EditableReportColBean ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbeanTmp==null) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        if(!(colDataObj instanceof EditableReportColDataBean))
        {
            return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        }
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        StringBuilder resultBuf=new StringBuilder();
        if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {//当前列不参与本次显示
            tdPropsBuf.append(" style=\"display:none;\" ");
            resultBuf.append(showTempHiddenCol(cbean,ercdatabean,true));
        }else
        {
            String col_displayvalue=getColDisplayValue(cbean,ercbeanTmp,dataObj,ercdatabean,null);
            ColDisplayData colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,dataObj,0,dataObj.getColValuestyleproperty(cbean
                    .getProperty()),col_displayvalue);
            if(colDisplayData.getColdataByInterceptor()!=null&&colDisplayData.getColdataByInterceptor().isReadonly())
            {
                col_displayvalue=dataObj.getColStringValue(cbean);
            }else
            {
                col_displayvalue=colDisplayData.getValue();
            }
            if(col_displayvalue==null||col_displayvalue.trim().equals("")) col_displayvalue="&nbsp;";
            resultBuf.append(getDisplayedColFontValue(cbean,ercbeanTmp,ercdatabean));
            resultBuf.append(col_displayvalue);
            tdPropsBuf.append(this.getDetailTdValuestyleproperty(cbean,colDisplayData.getStyleproperty()));
        }
        resultBuf.append("</font>");
        return resultBuf.toString();
    }
    
    public String showColData(ColBean cbean,boolean showpart,String dynstyleproperty)
    {
        if(!showpart||rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return super.showColData(cbean,showpart,dynstyleproperty);
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) return "";
        if(cbean.isNonValueCol()) return "";
        AbsReportDataPojo dataObj=this.lstReportData!=null&&this.lstReportData.size()>0?this.lstReportData.get(0):null;
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbean==null) return super.showColData(cbean,showpart,dynstyleproperty);
        Object colDataObj;
        EditableReportColDataBean ercdatabean;
        StringBuilder resultBuf=new StringBuilder();
        if(rrequest.getStringAttribute(rbean.getGuid()+"_showHiddenCols","").equals(""))
        {//还没有显示删除数据时需要用到的displaytype为hidden的<col/>
            rrequest.setAttribute(rbean.getGuid()+"_showHiddenCols","true");
            for(ColBean cbeanTemp:rbean.getDbean().getLstCols())
            {//显示所有隐藏列的<font/>
                if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbeanTemp.getDisplaytype(true)))
                {
                    colDataObj=this.initDisplayCol(cbeanTemp,dataObj);
                    resultBuf.append(showHiddenCol(cbeanTemp,colDataObj));
                }
            }
        }
        colDataObj=this.initDisplayCol(cbean,dataObj);
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.showColData(cbean,showpart,dynstyleproperty);
        ercdatabean=(EditableReportColDataBean)colDataObj;
        if(this.mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {
            return this.showTempHiddenCol(cbean,ercdatabean,true)+"</font>";
        }
        resultBuf.append(getDisplayedColFontValue(cbean,ercbean,ercdatabean));
        if(!(ercbean.getInputbox() instanceof CustomizedBox)||this.isReadonlyCol(cbean))
        {
            String col_displayvalue=getColDisplayValue(cbean,ercbean,dataObj,ercdatabean,dynstyleproperty);
            if(col_displayvalue==null) col_displayvalue="";
            ColDisplayData colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,dataObj,0,dataObj==null?cbean.getValuestyleproperty(
                    rrequest,false):dataObj.getColValuestyleproperty(cbean.getProperty()),col_displayvalue);
            col_displayvalue=colDisplayData.getValue();
            if(col_displayvalue==null||col_displayvalue.trim().equals("")) col_displayvalue="&nbsp;";
            resultBuf.append(col_displayvalue).append("</font>");
        }else
        {//如果是开发人员自己显示输入框
            EditableReportBean erbean=(EditableReportBean)rbean.getExtendConfigDataForReportType(KEY);
            if(erbean==null)
            {
                erbean=new EditableReportBean(rbean);
                rbean.setExtendConfigDataForReportType(KEY,erbean);
            }
            if(Tools.isEmpty(erbean.getConfigSavedatatype())) erbean.setSavedatatype("all");
        }
        return resultBuf.toString();
    }
    
    private String showTempHiddenCol(ColBean cbean,EditableReportColDataBean ercdatabean,boolean isRealHidden)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("<font id=\"font_").append(rbean.getGuid()).append("\" name=\"font_").append(rbean.getGuid()).append("\" ");
        ColBean cbUpdateSrc=cbean.getUpdateColBeanSrc(false);
        if(cbUpdateSrc!=null)
        {
            resultBuf.append(" updatecolSrc=\"").append(cbUpdateSrc.getProperty()).append("\"");
        }else
        {
            if(cbean.getUpdateColBeanDest(false)!=null)
            {
                resultBuf.append(" updatecolDest=\"").append(cbean.getUpdateColBeanDest(false).getProperty()).append("\"");
            }
        }
        resultBuf.append(" value_name=\"").append(ercdatabean.getValuename()).append("\"");
        resultBuf.append(" value=\"").append(ercdatabean==null?"":Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
        if(!this.realAccessMode.equals(Consts.ADD_MODE))
        {
            resultBuf.append(" oldvalue=\"").append(ercdatabean==null?"":Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
        }
        if(isRealHidden) resultBuf.append(" style=\"display:none\"");
        resultBuf.append(">");
        return resultBuf.toString();
    }
    
    private String getDisplayedColFontValue(ColBean cbean,EditableReportColBean ercbean,EditableReportColDataBean ercdatabean)
    {
        if(!this.realAccessMode.equals(Consts.ADD_MODE)&&!this.realAccessMode.equals(Consts.UPDATE_MODE))
        {
            return showTempHiddenCol(cbean,ercdatabean,false);
        }
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("<font id=\"font_").append(rbean.getGuid()).append("\" name=\"font_").append(rbean.getGuid()).append("\" ");
        resultBuf.append(" inputboxid=\""+ercbean.getInputBoxId()+"\"");//后面根据此ID取相应的<span/>，如果取到了就是可编辑列，没有取到就是不可编辑列。
        resultBuf.append(" value_name=\"").append(ercdatabean.getValuename()).append("\"");
        resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean==null?"":ercdatabean.getValue())).append("\"");
        if(!this.realAccessMode.equals(Consts.ADD_MODE))
        {//当前不是添加模式
            resultBuf.append(" oldvalue=\"").append(ercdatabean==null?"":Tools.htmlEncode(ercdatabean.getOldvalue())).append("\"");
        }
        if(cbean.getUpdateColBeanDest(false)!=null)
        {
            resultBuf.append(" updatecolDest=\"").append(cbean.getUpdateColBeanDest(false).getProperty()).append("\"");
        }
        resultBuf.append(">");
        return resultBuf.toString();
    }
    
    private String getColDisplayValue(ColBean cbean,EditableReportColBean ercbean,AbsReportDataPojo dataObj,EditableReportColDataBean ercdatabean,String dynstyleproperty)
    {
        String col_displayvalue=null;
        if(ercbean==null) return dataObj.getColStringValue(cbean);
        if((this.realAccessMode.equals(Consts.ADD_MODE)&&ercbean.isEditableForInsert())
                ||(this.realAccessMode.equals(Consts.UPDATE_MODE)&&ercbean.isEditableForUpdate()))
        {
            col_displayvalue=ercbean.getInputbox().getDisplayStringValue(rrequest,ercdatabean.getValue(),dynstyleproperty,
                    cbean.checkReadonlyPermission(rrequest));
        }else
        {
            col_displayvalue=dataObj.getColStringValue(cbean);
        }
        return col_displayvalue;
    }
    
    public String getColOriginalValue(AbsReportDataPojo dataObj,ColBean cbean)
    {
        if(cbean==null||dataObj==null) return "";
        return dataObj.getColStringValue(cbean);
    }

    public boolean isReadonlyCol(ColBean cbean)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return true;
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) return true;
        if(cbean.isNonValueCol()) return true;
        if(!this.realAccessMode.equals(Consts.ADD_MODE)&&!this.realAccessMode.equals(Consts.UPDATE_MODE)) return true;
        if(ersqlbean==null) return true;
        if(cbean.checkReadonlyPermission(rrequest)) return true;
        EditableReportColBean ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbeanTmp==null) return true;
        if(this.realAccessMode.equals(Consts.ADD_MODE))
        {
           return !ercbeanTmp.isEditableForInsert();
        }else
        {
            return !ercbeanTmp.isEditableForUpdate();
        }
    }
    
    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;//当前是以只读方式访问报表
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            if(rbean.getSbean().getLstDatasetBeans()!=null&&rbean.getSbean().getLstDatasetBeans().size()>0&&this.realAccessMode.equals(Consts.READ_MODE))
            {
                return true;
            }
        }else if(buttonType instanceof UpdateButton)
        {
            if(ersqlbean.getUpdatebean()==null) return false;
            if(rbean.getSbean().getLstDatasetBeans()!=null&&rbean.getSbean().getLstDatasetBeans().size()>0&&this.realAccessMode.equals(Consts.READ_MODE)
                    &&this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                return true;
            }
        }else if(buttonType instanceof DeleteButton)
        {
            if(ersqlbean.getDeletebean()==null) return false;
            if(this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                if(rbean.getSbean().getLstDatasetBeans()!=null&&rbean.getSbean().getLstDatasetBeans().size()>0&&this.realAccessMode.equals(Consts.READ_MODE))
                {
                    return true;
                }
                if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE))
                {
                    return true;
                }
            }
        }else if(buttonType instanceof SaveButton)
        {
            if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE)&&this.lstReportData!=null&&this.lstReportData.size()>0)
            {
                return true;
            }
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE))
            {
                return true;
            }
        }else if(buttonType instanceof CancelButton||buttonType instanceof ResetButton)
        {
            if(ersqlbean.getUpdatebean()!=null&&this.realAccessMode.equals(Consts.UPDATE_MODE))
            {
                return true;
            }
            if(ersqlbean.getInsertbean()!=null&&this.realAccessMode.equals(Consts.ADD_MODE))
            {
                return true;
            }
        }
        return false;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        ComponentConfigLoadManager.loadEditableColConfig(colbean,lstEleColBeans.get(0),KEY);
        return 1;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        super.afterSqlLoading(sqlbean,lstEleSqlBeans);
        ComponentConfigLoadManager.loadEditableSqlConfig(sqlbean,lstEleSqlBeans,KEY);
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean!=null&&ersqlbean.getInsertbean()!=null)
        {
            XmlElementBean eleInsertBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"insert");
            String condition=eleInsertBean.attributeValue("condition");
            if(condition!=null)
            {//如果在<insert/>中配置了condition，说明添加数据后需要以update/read模式显示当前添加的数据
                condition=condition.trim();
                if(condition.equals(""))
                {
                    ersqlbean.getInsertbean().setMUpdateConditions(null);
                }else
                {
                    Map<String,String> mCondtions=new HashMap<String,String>();
                    List<String> lstConditions=Tools.parseStringToList(condition,";",false);
                    for(String contemp:lstConditions)
                    {
                        if(contemp==null||contemp.trim().equals("")) continue;
                        contemp=contemp.trim();
                        int idxEqual=contemp.indexOf("=");
                        if(idxEqual<=0)
                        {
                            throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置的<insert/>的condition属性"+condition+"不合法");
                        }
                        String conname=contemp.substring(0,idxEqual).trim();
                        String colprop=contemp.substring(idxEqual+1).trim();
                        if(conname.equals(""))
                        {
                            throw new WabacusConfigLoadingException("报表"+sqlbean.getReportBean().getPath()+"配置的<insert/>的condition属性"+condition
                                    +"不合法，没有指定要赋值的查询条件name属性");
                        }
                        mCondtions.put(conname,colprop);
                    }
                    ersqlbean.getInsertbean().setMUpdateConditions(mCondtions);
                }
            }
        }
        return 1;
    }

    public int afterReportLoading(ReportBean reportbean,List<XmlElementBean> lstEleReportBeans)
    {
        super.afterReportLoading(reportbean,lstEleReportBeans);
        ComponentConfigLoadManager.loadEditableReportConfig(reportbean,lstEleReportBeans,KEY);
        return 1;
    }
    
    public int doPostLoad(ReportBean reportbean)
    {
        super.doPostLoad(reportbean);
        ComponentConfigLoadManager.doEditableReportTypePostLoad(reportbean,KEY);
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)reportbean.getSbean().getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null) return 1;
        processEditableButtons(ersqlbean);
        if(ersqlbean.getInsertbean()!=null&&ersqlbean.getInsertbean().getMUpdateConditions()!=null)
        {
            for(Entry<String,String> entryTmp:ersqlbean.getInsertbean().getMUpdateConditions().entrySet())
            {
                if(Tools.isDefineKey("url",entryTmp.getValue()))
                {
                    reportbean.addParamNameFromURL(Tools.getRealKeyByDefine("url",entryTmp.getValue()));
                }
            }
        }
        return 1;
    }

    private void processEditableButtons(EditableReportSqlBean ersqlbean)
    {
        ReportBean reportbean=ersqlbean.getOwner().getReportBean();
        if(ersqlbean.getInsertbean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,AddButton.class,Consts.ADD_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(AddButton.class);
        }
        if(ersqlbean.getUpdatebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,UpdateButton.class,Consts.MODIFY_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(UpdateButton.class);
        }
        if(ersqlbean.getDeletebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
        if(ersqlbean.getInsertbean()!=null||ersqlbean.getUpdatebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,SaveButton.class,Consts.SAVE_BUTTON_DEFAULT);
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,CancelButton.class,Consts.CANCEL_BUTTON_DEFAULT);
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,ResetButton.class,Consts.RESET_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(SaveButton.class);
            reportbean.getButtonsBean().removeAllCertainTypeButtons(CancelButton.class);
            reportbean.getButtonsBean().removeAllCertainTypeButtons(ResetButton.class);
        }
    }
    
    public int doPostLoadFinally(ReportBean reportbean)
    {
        ComponentConfigLoadManager.doEditableReportTypePostLoadFinally(reportbean,KEY);
        return super.doPostLoadFinally(reportbean);
    }
    
    public int compareTo(IEditableReportType otherEditableReportObj)
    {
        return EditableReportAssistant.getInstance().comareEditableReportSaveOrder(this,otherEditableReportObj);
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_EDITABLEDETAIL;
    }
}
