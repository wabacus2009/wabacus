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
import java.util.List;

import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableDetailReportType2 extends DetailReportType implements IEditableReportType
{
    protected EditableReportSqlBean ersqlbean=null;
    
    protected String realAccessMode;
    
    public EditableDetailReportType2(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            this.ersqlbean=(EditableReportSqlBean)((ReportBean)comCfgBean).getSbean().getExtendConfigDataForReportType(KEY);
        }
    }

    public final static String KEY=EditableDetailReportType2.class.getName();

    public void initUrl(IComponentConfigBean applicationConfigBean,ReportRequest rrequest)
    {
        super.initUrl(applicationConfigBean,rrequest);
        String accessmode=rrequest.getStringAttribute(applicationConfigBean.getId()+"_ACCESSMODE",getDefaultAccessMode()).toLowerCase();
        if(accessmode.equals(Consts.READONLY_MODE))
        {
            rrequest.addParamToUrl(applicationConfigBean.getId()+"_ACCESSMODE",Consts.READONLY_MODE,true);
        }
    }
    
    public void init()
    {
        super.init();
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            this.realAccessMode=Consts.READONLY_MODE;
        }else
        {
            this.realAccessMode="";
        }
    }

    public void initReportAfterDoStart()
    {
        super.initReportAfterDoStart();
        if(EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            String accessmode=rrequest.getStringAttribute(rbean.getId()+"_ACCESSMODE","").toLowerCase();
            if(accessmode.equals(Consts.READONLY_MODE)) setNewAccessMode(Consts.READONLY_MODE);
        }else
        {
            EditableReportAssistant.getInstance().doAllReportsSaveAction(rrequest);
        }
    }

    public void collectEditActionGroupBeans(List<AbsUpdateAction> lstAllUpdateActions)
    {
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null||(!sidbean.hasSavingData()&&!sidbean.hasDeleteData())) return;
        if(sidbean.hasSavingData())
        {
            lstAllUpdateActions.addAll(ersqlbean.getUpdatebean().getLsAllEditActions());
        }else if(sidbean.hasDeleteData())
        {
            lstAllUpdateActions.addAll(ersqlbean.getDeletebean().getLsAllEditActions());
        }
    }
    
    public int[] doSaveAction() throws SQLException
    {
        int[] result=new int[] { IInterceptor.WX_RETURNVAL_SKIP, 0 };
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null) return result;
        AbsEditableReportEditDataBean editbean;
        if(sidbean.hasSavingData())
        {
            editbean=ersqlbean.getUpdatebean();
        }else if(sidbean.hasDeleteData())
        {
            editbean=ersqlbean.getDeletebean();
        }else
        {
            return result;
        }
        if(rbean.getInterceptor()!=null)
        {
            result[0]=rbean.getInterceptor().doSave(this.rrequest,this.rbean,editbean);
        }else
        {
            result[0]=EditableReportAssistant.getInstance().doSaveReport(this.rrequest,this.rbean,editbean);
        }
        if(result[0]==IInterceptor.WX_RETURNVAL_TERMINATE||result[0]==IInterceptor.WX_RETURNVAL_SKIP) return result;
        if(sidbean.hasSavingData())
        {
            result[1]=IEditableReportType.IS_UPDATE_DATA;
        }else
        {
            result[1]=IEditableReportType.IS_DELETE_DATA;
        }
        result[0]=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,sidbean.hasDeleteData()?"delete":"update",result[0]);
        return result;
    }

    public String getDefaultAccessMode()
    {
        return "";
    }

    public String getRealAccessMode()
    {
        return this.realAccessMode;
    }
    
    public void setNewAccessMode(String newaccessmode)
    {
        rrequest.setAttribute(rbean.getId()+"_ACCESSMODE",newaccessmode);//把URL中初始化到rrequest的attributes中的值也换掉
        rrequest.setAttribute(rbean.getId(),"CURRENT_ACCESSMODE",newaccessmode);
        rrequest.addParamToUrl(rbean.getId()+"_ACCESSMODE",newaccessmode,true);
        if(Consts.READONLY_MODE.equals(newaccessmode))
        {
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode","true");
        }else
        {
            rrequest.getAttributes().remove(rbean.getId()+"_isReadonlyAccessmode");
        }
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        return resultBuf.toString();
    }
    
    protected void initInputBox(StringBuilder resultBuf)
    {
        super.initInputBox(resultBuf);
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return ;
        if(ersqlbean.getUpdatebean()==null||Consts.READONLY_MODE.equals(this.realAccessMode)) return;
        EditableReportColBean ercbeanTmp=null;
        for(ColBean cbean:rbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) continue;
            if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0) continue;
            if(cbean.isNonValueCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercbeanTmp==null||!ercbeanTmp.isEditableForUpdate()) continue;
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
//            EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
//            {//没有通过updatecol属性引用别的列进行更新
//                col_editvalue=getColOriginalValue(dataObj,ercbean.getUpdateCbean());
        String col_editvalue=getColOriginalValue(dataObj,cbean);
        return EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,ersqlbean.getUpdatebean(),cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String showHiddenCol(ColBean cbean,Object colDataObj)
    {
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.showHiddenCol(cbean,colDataObj);
        return "<td style=\"display:none\" "+getTdPropsForColNameAndValue(cbean,(EditableReportColDataBean)colDataObj)+"></td>";//不能用<td/>，而必须是<td></td>格式，否则在IE上取子节点会有问题
    }
    
    protected String getColValueTdPropertiesAndContent(ColBean cbean,AbsReportDataPojo dataObj,Object colDataObj,StringBuffer tdPropsBuf)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getColValueTdPropertiesAndContent(cbean,dataObj,colDataObj,tdPropsBuf);
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        tdPropsBuf.append(" ").append(getTdPropsForColNameAndValue(cbean,ercdatabean));
        if(mColPositions.get(cbean.getColid()).getDisplaymode()<=0)
        {
            tdPropsBuf.append(" style=\"display:none;\"");
            return "";
        }
        String col_displayvalue=getColDisplayValue(ercdatabean,cbean,tdPropsBuf,dataObj);
        ColDisplayData colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,dataObj,0,dataObj.getColValuestyleproperty(cbean.getProperty()),
                col_displayvalue);
        if(colDisplayData.getColdataByInterceptor()!=null&&colDisplayData.getColdataByInterceptor().isReadonly())
        {
            tdPropsBuf.delete(0,tdPropsBuf.length());
            col_displayvalue=dataObj.getColStringValue(cbean);
        }else
        {
            col_displayvalue=colDisplayData.getValue();
        }
        if(col_displayvalue==null||col_displayvalue.trim().equals("null")||col_displayvalue.trim().equals("")) col_displayvalue="&nbsp;";
        tdPropsBuf.append(getDetailTdValuestyleproperty(cbean,colDisplayData.getStyleproperty()));
        return col_displayvalue;
    }
    
    private String getTdPropsForColNameAndValue(ColBean cbean,EditableReportColDataBean ercdatabean)
    {
        if(cbean.isNonValueCol()) return "";
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td\" ");//有输入框的为此<td/>设置一id，以便客户端校验时用上
        resultBuf.append(" value_name=\""+ercdatabean.getValuename()+"\"");
        resultBuf.append(" oldvalue=\""+Tools.htmlEncode(ercdatabean.getOldvalue())+"\"");
        resultBuf.append(" value=\""+Tools.htmlEncode(ercdatabean.getValue())+"\"");
        if(cbean.getUpdateColBeanSrc(false)!=null)
        {
            resultBuf.append(" updatecolSrc=\"").append(cbean.getUpdateColBeanSrc(false).getProperty()).append("\"");
        }else if(cbean.getUpdateColBeanDest(false)!=null)
        {
            resultBuf.append(" updatecolDest=\"").append(cbean.getUpdateColBeanDest(false).getProperty()).append("\"");
        }
        return resultBuf.toString();
    }

    public String getColOriginalValue(AbsReportDataPojo object,ColBean cbean)
    {
        String colproperty=cbean.getProperty();
        if(!cbean.isNonFromDbCol()) colproperty=colproperty+"_old";
        String oldvalue=ReportAssistant.getInstance().getPropertyValueAsString(object,colproperty,cbean.getDatatypeObj());
        if(oldvalue==null||oldvalue.equals("null"))
        {
            oldvalue="";
        }
        return oldvalue;
    }

    private String getColDisplayValue(EditableReportColDataBean ercdatabean,ColBean cbean,StringBuffer tdPropsBuf,AbsReportDataPojo dataObj)
    {
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        String col_displayvalue;
        if(ercbean==null||!ercdatabean.isEditable()||this.realAccessMode.equals(Consts.READONLY_MODE))
        {
            col_displayvalue=dataObj.getColStringValue(cbean);//当前列不可编辑，或者本次是只读访问模式
            if(ercbean!=null) col_displayvalue=ercbean.getRealColDisplayValue(rrequest,dataObj,col_displayvalue);
        }else if(ercbean.getInputbox().isDisplayOnClick())
        {
            if(ercdatabean.isNeedDefaultValue())
            {
                col_displayvalue=ercdatabean.getDefaultvalue();
            }else
            {
                col_displayvalue=dataObj.getColStringValue(cbean);
                if(col_displayvalue==null||col_displayvalue.equals("null")) col_displayvalue="";
            }
            col_displayvalue=ercbean.getRealColDisplayValue(rrequest,dataObj,col_displayvalue);
            if(!cbean.checkReadonlyPermission(rrequest))
            {
                String beforedesc=ercbean.getInputbox().getBeforedescription(rrequest);
                String afterdesc=ercbean.getInputbox().getAfterdescription(rrequest);
                if(beforedesc!=null&&!beforedesc.trim().equals("")||afterdesc!=null&&!afterdesc.trim().equals(""))
                {//点击时填充才需要在单元格中显示描述信息，如果是显示时就显示输入框，则在显示输入框时就会自动显示描述信息
                    String preContent="", postContent="";
                    if(beforedesc!=null&&!beforedesc.trim().equals("")) preContent=beforedesc;
                    if(afterdesc!=null&&!afterdesc.trim().equals("")) postContent=afterdesc;
                    col_displayvalue=preContent+"<span tagtype='COL_CONTENT_WRAP'>"+col_displayvalue+"</span>"+postContent;

                }
                tdPropsBuf.append(" onclick=\"try{fillInputBoxOnClick(event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }
        }else
        {
            col_displayvalue=ercbean.getInputbox().getDisplayStringValue(rrequest,ercdatabean.getValue(),
                    "style=\"text-align:"+ercbean.getTextalign()+";\"",cbean.checkReadonlyPermission(rrequest));
        }
        return col_displayvalue;
    }

    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(Consts.READONLY_MODE.equals(this.realAccessMode)) return false;
        if(buttonType instanceof SaveButton&&ersqlbean.getUpdatebean()!=null) return true;
        if(buttonType instanceof DeleteButton&&ersqlbean.getDeletebean()!=null&&this.lstReportData!=null&&this.lstReportData.size()>0) return true;
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
        if(sqlbean!=null)
        {
            EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
            if(ersqlbean!=null)
            {
                if(ersqlbean.getInsertbean()!=null)
                {
                    throw new WabacusConfigLoadingException("不能在editabledetail2类型报表中配置添加功能，如果需要添加功能，可以在editabledetail中配置");
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
        SqlBean sqlbean=reportbean.getSbean();
        if(sqlbean==null) return 1;
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null) return 1;
        processEditableButtons(reportbean,ersqlbean);

        EditableReportColBean ercolbeanTmp;
        for(ColBean cbean:reportbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) continue;
            ercolbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            String align=Tools.getPropertyValueByName("align",cbean.getValuestyleproperty(null,true),true);
            if(align==null||align.trim().equals("")) align="left";
            ercolbeanTmp.setTextalign(align);
        }
        return 1;
    }

    private void processEditableButtons(ReportBean reportbean,EditableReportSqlBean ersqlbean)
    {
        if(ersqlbean.getDeletebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
        if(ersqlbean.getUpdatebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,SaveButton.class,Consts.SAVE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(SaveButton.class);
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
        return Consts_Private.REPORT_FAMILY_EDITABLEDETAIL2;
    }
}
