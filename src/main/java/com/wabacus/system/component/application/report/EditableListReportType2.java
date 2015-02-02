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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.ConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadAssistant;
import com.wabacus.config.component.ComponentConfigLoadManager;
import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.SqlBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.exception.WabacusRuntimeException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.buttons.AbsButtonType;
import com.wabacus.system.buttons.AddButton;
import com.wabacus.system.buttons.DeleteButton;
import com.wabacus.system.buttons.SaveButton;
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.configbean.ColDisplayData;
import com.wabacus.system.component.application.report.configbean.editablereport.AbsEditableReportEditDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.AbsSelectBox;
import com.wabacus.system.inputbox.PasswordBox;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.system.intercept.RowDataBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableListReportType2 extends UltraListReportType implements IEditableReportType
{
    public final static String KEY=EditableListReportType2.class.getName();

    private final static Log log=LogFactory.getLog(EditableListReportType2.class);
    
    protected EditableListReportDisplayBean elrdbean=null;
    
    protected EditableReportSqlBean ersqlbean=null;
    
    private int display_rowcount;
    
    protected String realAccessMode;
    
    public EditableListReportType2(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
        if(comCfgBean!=null)
        {
            this.elrdbean=(EditableListReportDisplayBean)((ReportBean)comCfgBean).getDbean().getExtendConfigDataForReportType(KEY);
            this.ersqlbean=(EditableReportSqlBean)((ReportBean)comCfgBean).getSbean().getExtendConfigDataForReportType(KEY);
        }
    }

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
        boolean[] shouldDoSave=sidbean.getShouldDoSave();
        if(shouldDoSave.length!=4) return;
        if(sidbean.hasSavingData())
        {
            if(shouldDoSave[0]) lstAllUpdateActions.addAll(ersqlbean.getInsertbean().getLsAllEditActions());
            if(shouldDoSave[1]) lstAllUpdateActions.addAll(ersqlbean.getUpdatebean().getLsAllEditActions());//有修改数据需要保存
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
        boolean[] shouldDoSave=sidbean.getShouldDoSave();
        if(shouldDoSave.length!=4) return result;
        if(sidbean.hasSavingData())
        {
            if(shouldDoSave[0])
            {
                result[0]=doSaveAction(ersqlbean.getInsertbean());
                if(result[0]==IInterceptor.WX_RETURNVAL_TERMINATE||result[0]==IInterceptor.WX_RETURNVAL_SKIP) return result;
                result[1]=IEditableReportType.IS_ADD_DATA;
            }
            if(shouldDoSave[1])
            {
                result[0]=doSaveAction(ersqlbean.getUpdatebean());
                if(result[0]==IInterceptor.WX_RETURNVAL_TERMINATE||result[0]==IInterceptor.WX_RETURNVAL_SKIP) return result;
                result[1]=IEditableReportType.IS_UPDATE_DATA;
            }
            if(shouldDoSave[0]&&shouldDoSave[1]) result[1]=IEditableReportType.IS_ADD_UPDATE_DATA;
        }else if(sidbean.hasDeleteData())
        {
            result[0]=doSaveAction(ersqlbean.getDeletebean());
            if(result[0]==IInterceptor.WX_RETURNVAL_TERMINATE||result[0]==IInterceptor.WX_RETURNVAL_SKIP) return result;
            result[1]=IEditableReportType.IS_DELETE_DATA;
        }else
        {
            return result;
        }
        result[0]=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,sidbean.hasDeleteData()?"delete":"",result[0]);
        return result;
    }

    private int doSaveAction(AbsEditableReportEditDataBean editbean)
    {
        if(rbean.getInterceptor()!=null)
        {
            return rbean.getInterceptor().doSave(this.rrequest,this.rbean,editbean);
        }else
        {
            return EditableReportAssistant.getInstance().doSaveReport(this.rrequest,this.rbean,editbean);
        }
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
        rrequest.setAttribute(rbean.getId()+"_ACCESSMODE",newaccessmode);
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

    protected boolean isLazyDisplayData()
    {
        boolean isLazyLoad=super.isLazyDisplayData();
        int[] maxminrownums=getMaxMinRownums();//获取到指定的最大小记录数
        if(maxminrownums[1]>0)
        {
            if(isLazyLoad)
            {
                log.warn("报表"+this.rbean.getPath()+"配置了最小记录数，因此将它指定为延迟加载无效");
            }
            return false;
        }
        return isLazyLoad;
    }
    
    private boolean hasLoadedData=false;
    
    public void setHasLoadedDataFlag(boolean hasLoadedDataFlag)
    {
        super.setHasLoadedDataFlag(hasLoadedDataFlag);
        this.hasLoadedData=hasLoadedDataFlag;
    }
    
    public void loadReportData(boolean shouldInvokePostaction)
    {
        if(this.hasLoadedData) return;
        this.hasLoadedData=true;
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||EditableReportAssistant.getInstance().isReadonlyAccessMode(this))
        {
            super.loadReportData(shouldInvokePostaction);
            return;
        }
        super.loadReportData(false);
        if(lstReportData==null) lstReportData=new ArrayList();
        int rowcount=this.cacheDataBean.getRecordcount();
        int[] maxminrownums=getMaxMinRownums();
        int maxrownum=maxminrownums[0];
        int minrownum=maxminrownums[1];
        if(maxrownum>0&&maxrownum<this.cacheDataBean.getRecordcount())
        {//如果配置了最大记录数，且小于总记录数，则用最大记录数来更新页码信息
            rowcount=maxrownum;
            if(this.cacheDataBean.getPagesize()>0)
            {
                if(this.cacheDataBean.getRefreshNavigateInfoType()<=0)
                {
                    this.cacheDataBean.setRecordcount(rowcount);
                    this.cacheDataBean.setPagecount(ReportAssistant.getInstance().calPageCount(this.cacheDataBean.getPagesize(),rowcount));
                }
            }else
            {
                this.cacheDataBean.setPagecount(1);
            }
        }else if(minrownum>0&&minrownum>this.cacheDataBean.getRecordcount())
        {
            rowcount=minrownum;
            if(this.cacheDataBean.getPagesize()>0)
            {//如果是分页显示报表，则用新记录数重新更新页数信息
                if(this.cacheDataBean.getRefreshNavigateInfoType()<=0)
                {
                    this.cacheDataBean.setPagecount(ReportAssistant.getInstance().calPageCount(this.cacheDataBean.getPagesize(),rowcount));
                }
            }else
            {
                this.cacheDataBean.setPagecount(1);
            }
        }

        if(this.cacheDataBean.getPagesize()<0)
        {
            display_rowcount=rowcount;
        }else
        {
            if(this.cacheDataBean.getFinalPageno()*this.cacheDataBean.getPagesize()>rowcount)
            {//本页是最后一页，且最后一页显示的记录小于页大小。
                display_rowcount=rowcount-(this.cacheDataBean.getFinalPageno()-1)*this.cacheDataBean.getPagesize();
            }else
            {
                display_rowcount=this.cacheDataBean.getPagesize();
            }
        }
        if(!this.isLazyDisplayData()&&shouldInvokePostaction) doLoadReportDataPostAction();
    }

    private int[] getMaxMinRownums()
    {
        int[] rownums=new int[2];
        rownums[0]=elrdbean.getMaxrownum();
        rownums[1]=elrdbean.getMinrownum();
        boolean hasDynMaxminrownums=false;
        String maxrownum=(String)this.cacheDataBean.getAttributes().get("maxrownum");
        if(maxrownum!=null&&!maxrownum.trim().equals(""))
        {
            rownums[0]=Integer.parseInt(maxrownum.trim());
            hasDynMaxminrownums=true;
        }
        String minrownum=(String)this.cacheDataBean.getAttributes().get("minrownum");
        if(minrownum!=null&&!minrownum.trim().equals(""))
        {
            rownums[1]=Integer.parseInt(minrownum.trim());
            hasDynMaxminrownums=true;
        }
        if(hasDynMaxminrownums) validateAndCalMaxMinRownum(ersqlbean,rownums);
        return rownums;
    }

    protected int getMinRownum()
    {
        return getMaxMinRownums()[1];
    }
    
    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        if(!this.realAccessMode.equals(Consts.READONLY_MODE))
        {//不是只读访问模式
            if(this.ersqlbean.getInsertbean()!=null)
            {
                if(this.ersqlbean.getInsertbean().getAddposition()!=null)
                {
                    resultBuf.append(" addposition=\"").append(this.ersqlbean.getInsertbean().getAddposition()).append("\"");
                }
                if(this.ersqlbean.getInsertbean().getCallbackmethod()!=null)
                {
                    resultBuf.append(" addCallbackMethod=\"{method:").append(this.ersqlbean.getInsertbean().getCallbackmethod().trim()).append("}\"");
                }
            }
            if(this.ersqlbean.getUpdatebean()!=null)
            {
                resultBuf.append(" isEnableCrossPageEdit=\"").append(this.ersqlbean.getUpdatebean().isEnableCrossPageEdit()).append("\"");
            }
            resultBuf.append(createColInfosForAddRow());
        }
        return resultBuf.toString();
    }

    private String createColInfosForAddRow()
    {
        if(ersqlbean==null||ersqlbean.getInsertbean()==null) return "";
        AbsInputBox boxObjTmp;
        StringBuilder colsBuf=new StringBuilder();
        colsBuf.append("{");
        int currentTotalRowcount=this.cacheDataBean.getRecordcount();
        int[] maxminrownums=getMaxMinRownums();
        int maxrownum=maxminrownums[0];
        int minrownum=maxminrownums[1];
        if(maxrownum>0&&minrownum>0&&minrownum>currentTotalRowcount)
        {
            /*
             * 计算本页显示了多少条添加新行的记录，方便当用户在客户端添加新记录行时，判断是否已经超出了最大记录数
             */
            if(this.cacheDataBean.getPagesize()>0)
            {
                /***********************************************************
                 * 计算到本页为止，应该显示的总记录行数，包括已有记录的行和添加新记录的行
                 */
                int tmp=this.cacheDataBean.getFinalPageno()*this.cacheDataBean.getPagesize();
                if(tmp>minrownum) tmp=minrownum;//本页是最后一页，且不用显示pagesize条记录。
                /***********************************************************/

                tmp=tmp-this.cacheDataBean.getRecordcount();
                if(tmp>0)
                {
                    if(tmp>=this.cacheDataBean.getPagesize())
                    {
                        if(this.cacheDataBean.getFinalPageno()<this.cacheDataBean.getPagecount())
                        {
                            tmp=this.cacheDataBean.getPagesize();
                        }else
                        {//最后一页的话，计算出此页实际显示的添加新记录的行数
                            tmp=minrownum-((this.cacheDataBean.getFinalPageno()-1)*this.cacheDataBean.getPagesize());
                        }
                    }
                    currentTotalRowcount=currentTotalRowcount+tmp;
                }
            }else
            {//不分页显示的报表
                currentTotalRowcount=minrownum;
            }
        }
        colsBuf.append("currentRecordCount:").append(currentTotalRowcount);
        colsBuf.append(",maxRecordCount:").append(maxrownum);
        if(ersqlbean.getInsertbean().getInsertstyle()!=null&&!ersqlbean.getInsertbean().getInsertstyle().trim().equals(""))
        {
            colsBuf.append(",insertstyle:\"").append(ersqlbean.getInsertbean().getInsertstyle()).append("\"");
        }
        colsBuf.append(",cols:[");
        int displaymodeTmp;
        boolean isReadonlyCol;
        EditableReportColBean ercbeanTmp;
        for(ColBean cbean:this.getLstDisplayColBeans())
        {
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            colsBuf.append("{");
            displaymodeTmp=this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean,true);
            if(displaymodeTmp<=0) colsBuf.append("hidden:\"true\",");
            if(ercbeanTmp==null||displaymodeTmp<=0||cbean.isControlCol()||!ercbeanTmp.isEditableForInsert()||cbean.checkReadonlyPermission(rrequest))
            {
                isReadonlyCol=true;
            }else
            {
                isReadonlyCol=false;
            }
            String wrapstart=this.getColDisplayValueWrapStart(cbean,true,isReadonlyCol,true);
            if(!wrapstart.trim().equals(""))
            {
                colsBuf.append("colWrapStart:\"").append(wrapstart).append("\",");
                colsBuf.append("colWrapEnd:\"").append(this.getColDisplayValueWrapEnd(cbean,true,isReadonlyCol,true)).append("\",");
            }
            if(cbean.isRowSelectCol())
            {
                if(Consts.ROWSELECT_RADIOBOX.equalsIgnoreCase(alrbean.getRowSelectType())||Consts.ROWSELECT_SINGLE_RADIOBOX.equalsIgnoreCase(alrbean.getRowSelectType()))
                {
                    colsBuf.append("coltype:\"ROWSELECTED-RADIOBOX\"");
                }else
                {//行选中方式是通过checkbox方式
                    colsBuf.append("coltype:\"ROWSELECTED-CHECKBOX\"");
                }
                colsBuf.append("},");
                continue;
            }else if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")||cbean.isNonValueCol()||cbean.isSequenceCol()||cbean.isRoworderCol())
            {
                colsBuf.append("coltype:\"EMPTY\"},");
                continue;
            }
            colsBuf.append("boxid:\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("\"");
            boxObjTmp=null;
            String defaultvalue=null,defaultvaluelabel=null;
            if(cbean.getUpdateColBeanSrc(false)!=null)
            {
                EditableReportColBean ercbeanTmp2=(EditableReportColBean)cbean.getUpdateColBeanSrc(false).getExtendConfigDataForReportType(KEY);
                defaultvalue=ercbeanTmp2.getInputbox().getDefaultvalue(rrequest);
                colsBuf.append(",updatecolSrc:\""+ercbeanTmp.getUpdatecolSrc()+"\"");
            }else if(cbean.getUpdateColBeanDest(false)!=null)
            {
                boxObjTmp=ercbeanTmp.getInputbox();
                defaultvalue=boxObjTmp.getDefaultlabel(rrequest);
                colsBuf.append(",updatecolDest:\""+ercbeanTmp.getUpdatecolDest()+"\"");
            }else if(ercbeanTmp!=null&&ercbeanTmp.getInputbox()!=null)
            {
                boxObjTmp=ercbeanTmp.getInputbox();
                defaultvalue=boxObjTmp.getDefaultvalue(rrequest);
                defaultvaluelabel=boxObjTmp.getDefaultlabel(rrequest);
            }
            colsBuf.append(",value_name:\"");
            if(displaymodeTmp<0)
            {
                colsBuf.append(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX);
            }else if(boxObjTmp instanceof PasswordBox&&defaultvalue!=null&&!defaultvalue.trim().equals(""))
            {
                if(((PasswordBox)boxObjTmp).getEncodelength()>0)
                {//需要在前台编码显示
                    colsBuf.append(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX);
                    String encodevalue=this.currentSecretColValuesBean.getUniqueEncodeString(((PasswordBox)boxObjTmp).getEncodelength());
                    this.currentSecretColValuesBean.addParamValue(encodevalue,defaultvalue);
                    defaultvalue=encodevalue;
                    defaultvaluelabel=encodevalue;
                }
            }
            colsBuf.append(EditableReportAssistant.getInstance().getColParamName(cbean)).append("\"");
            if(defaultvalue!=null)
            {
                colsBuf.append(",defaultvalue:\"").append(defaultvalue).append("\"");
                colsBuf.append(",defaultvaluelabel:\"").append(defaultvaluelabel).append("\"");
            }
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true)))
            {
                colsBuf.append("},");
                continue;
            }
            if(!ercbeanTmp.isEditableForInsert()||cbean.checkReadonlyPermission(rrequest))
            {
                colsBuf.append(",coltype:\"NONE-EDITABLE\"},");
                continue;
            }
            colsBuf.append(",coltype:\"EDITABLE\"");
            colsBuf.append("},");
        }
        if(colsBuf.length()>0&&colsBuf.charAt(colsBuf.length()-1)==',') colsBuf.deleteCharAt(colsBuf.length()-1);
        colsBuf.append("]}");
        return " newRowCols=\""+Tools.jsParamEncode(colsBuf.toString())+"\"";
    }

    protected boolean isFixedLayoutTable()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return false;
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return super.isFixedLayoutTable();
        return true;//可编辑报表类型
    }

    protected void initInputBox(StringBuilder resultBuf)
    {
        super.initInputBox(resultBuf);
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return;
        if(ersqlbean.getInsertbean()==null&&ersqlbean.getUpdatebean()==null) return ;
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return ;
        EditableReportColBean ercbeanTmp;
        for(ColBean cbean:this.getLstDisplayColBeans())
        {
            if(this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean,true)<=0) continue;
            if(cbean.isSequenceCol()||cbean.isControlCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercbeanTmp==null) continue;
            if(ercbeanTmp.isEditableForInsert()||ercbeanTmp.isEditableForUpdate())
            {
                resultBuf.append(ercbeanTmp.getInputbox().initDisplay(rrequest));
            }
        }
    }
    
    protected int[] getDisplayRowInfo()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||this.realAccessMode.equals(Consts.READONLY_MODE))
        {
            return super.getDisplayRowInfo();
        }
        return new int[]{0,this.display_rowcount};
    }

    protected String getDataTdClassName()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder())) 
        {
            return super.getDataTdClassName();
        }
        return "cls-data-td-editlist";
    }

    protected Object initDisplayCol(ColBean cbean,AbsReportDataPojo rowDataObjTmp)
    {
        if(cbean.isSequenceCol()||cbean.isControlCol()) return super.initDisplayCol(cbean,rowDataObjTmp);
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return super.initDisplayCol(cbean,rowDataObjTmp);
        AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrcbean==null) return super.initDisplayCol(cbean,rowDataObjTmp);
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(ercbean==null) return super.initDisplayCol(cbean,rowDataObjTmp);
        String col_editvalue=getColOriginalValue(rowDataObjTmp,cbean);
        if(col_editvalue==null) col_editvalue="";
        return EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,ersqlbean.getUpdatebean(),cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String getTdPropertiesForCol(ColBean cbean,Object colDataObj,int rowidx,boolean isCommonRowGroupCol)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getTdPropertiesForCol(cbean,colDataObj,rowidx,isCommonRowGroupCol);
        StringBuilder resultBuf=new StringBuilder();
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        String tdSuperProperties=super.getTdPropertiesForCol(cbean,ercdatabean,rowidx,isCommonRowGroupCol);
        resultBuf.append(tdSuperProperties);
        resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx+"\" ");//有输入框的为此<td/>设置一id，以便客户端校验时用上。
        if(tdSuperProperties.indexOf(" value_name=")<0)
        {
            resultBuf.append(" value_name=\""+ercdatabean.getValuename()+"\"");
        }
        if(tdSuperProperties.indexOf(" value=")<0)
        {//上面没有显示value属性
            resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean.getValue())).append("\"");
        }
        if(tdSuperProperties.indexOf(" oldvalue=")<0)
        {
            resultBuf.append(" oldvalue=\""+Tools.htmlEncode(ercdatabean.getOldvalue())+"\"");
        }
        if(cbean.getUpdateColBeanDest(false)!=null)
        {
            resultBuf.append(" updatecolDest=\"").append(cbean.getUpdateColBeanDest(false).getProperty()).append("\"");
        }else if(cbean.getUpdateColBeanSrc(false)!=null)
        {
            resultBuf.append(" updatecolSrc=\"").append(cbean.getUpdateColBeanSrc(false).getProperty()).append("\"");
        }
        return resultBuf.toString();
    }
   
    protected String showDataRowInAddMode(List<ColBean> lstColBeans,int rowidx)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||this.realAccessMode.equals(Consts.READONLY_MODE))
        {
            return super.showDataRowInAddMode(lstColBeans,rowidx);
        }
        StringBuffer resultBuf=new StringBuffer();
        boolean isReadonlyByRowInterceptor=false;
        RowDataBean rowInterceptorObjTmp=null;
        String trstylepropertyTmp=this.rbean.getDbean().getValuestyleproperty(rrequest,false);
        if(this.rbean.getInterceptor()!=null)
        {
            rowInterceptorObjTmp=new RowDataBean(this,trstylepropertyTmp,lstColBeans,null,rowidx,this.cacheDataBean.getTotalColCount());
            this.rbean.getInterceptor().beforeDisplayReportDataPerRow(this.rrequest,this.rbean,rowInterceptorObjTmp);
            if(rowInterceptorObjTmp.getInsertDisplayRowHtml()!=null) resultBuf.append(rowInterceptorObjTmp.getInsertDisplayRowHtml());
            if(!rowInterceptorObjTmp.isShouldDisplayThisRow())
            {
                this.global_rowindex++;
                return resultBuf.toString();
            }
            trstylepropertyTmp=rowInterceptorObjTmp.getRowstyleproperty();
            isReadonlyByRowInterceptor=rowInterceptorObjTmp.isReadonly();
        }
        EditableReportColDataBean ercdatabeanTmp;
        String col_displayvalue;
        StringBuffer tdPropsBuf=null;
        ColDisplayData colDisplayData;
        resultBuf.append(showDataRowTrStart(rowInterceptorObjTmp,trstylepropertyTmp,rowidx,false));
        resultBuf.append(" EDIT_TYPE=\"add\">");//表示当前行是新增的
        int colDisplayModeTmp;
        boolean isReadonlyByColInterceptor;
        for(ColBean cbean:lstColBeans)
        {
            //if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype())) continue; //永久隐藏列
            tdPropsBuf=new StringBuffer();
            boolean isEditableCol=false;
            resultBuf.append("<td class='"+getDataTdClassName()+"'");
            if(cbean.getUpdateColBeanDest(false)!=null)
            {
                resultBuf.append(" updatecolDest=\"").append(cbean.getUpdateColBeanDest(false).getProperty()).append("\"");
            }else if(cbean.getUpdateColBeanSrc(false)!=null)
            {
                resultBuf.append(" updatecolSrc=\"").append(cbean.getUpdateColBeanSrc(false).getProperty()).append("\"");
            }
            colDisplayModeTmp=this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean,true);
            if(colDisplayModeTmp<0)
            {
                resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx).append("\"");
                resultBuf.append(" value_name=\"").append(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX).append(
                        EditableReportAssistant.getInstance().getColParamName(cbean)).append("\"");
                resultBuf.append(" style=\"display:none;\"></td>");
                continue;
            }
            ercdatabeanTmp=EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,ersqlbean.getInsertbean(),cbean,"",
                    this.currentSecretColValuesBean);
            if(!ercdatabeanTmp.isEditable())
            {
                if(colDisplayModeTmp==0)
                {//当前列不参与本次显示
                    resultBuf.append(" style=\"display:none;\"></td>");
                    continue;
                }
                col_displayvalue=getNonEditableColDisplayValueInAddMode(cbean,rowInterceptorObjTmp,rowidx);
            }else
            {
                resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx+"\" ");
                resultBuf.append(" value_name=\"").append(ercdatabeanTmp.getValuename()).append("\"");
                resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabeanTmp.getValue())).append("\"");
                if(isReadonlyByRowInterceptor)
                {
                    col_displayvalue=getNonEditableColDisplayValueInAddMode(cbean,rowInterceptorObjTmp,rowidx);
                }else if(colDisplayModeTmp>0)
                {
                    col_displayvalue=showInputBoxForCol(tdPropsBuf,(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY),rowidx,null,
                            cbean,ercdatabeanTmp);
                    isEditableCol=true;
                }else
                {
                    resultBuf.append(" style=\"display:none;\"></td>");
                    continue;
                }
            }
            colDisplayData=ColDisplayData.getColDataFromInterceptor(this,cbean,null,rowidx,cbean.getValuestyleproperty(rrequest,false),col_displayvalue);
            isReadonlyByColInterceptor=colDisplayData.getColdataByInterceptor()!=null&&colDisplayData.getColdataByInterceptor().isReadonly();
            if(isReadonlyByColInterceptor&&isEditableCol)
            {
                col_displayvalue=getNonEditableColDisplayValueInAddMode(cbean,rowInterceptorObjTmp,rowidx);
                tdPropsBuf.delete(0,tdPropsBuf.length());
            }else
            {
                col_displayvalue=colDisplayData.getValue();
            }
            resultBuf.append(" ").append(tdPropsBuf.toString());
            resultBuf.append(" ").append(colDisplayData.getStyleproperty()).append(">");
            resultBuf.append(this.getColDisplayValueWrapStart(cbean,false,isReadonlyByColInterceptor||!isEditableCol,false));
            resultBuf.append(col_displayvalue);
            resultBuf.append(this.getColDisplayValueWrapEnd(cbean,false,isReadonlyByColInterceptor||!isEditableCol,false));
            resultBuf.append("</td>");
        }
        resultBuf.append("</tr>");
        this.global_rowindex++;
        this.global_sequence++;
        return resultBuf.toString();
    }

    private String getNonEditableColDisplayValueInAddMode(ColBean cbean,RowDataBean rowInterceptorObj,int rowidx)
    {
        String col_displayvalue=null;
        if(cbean.isSequenceCol()||cbean.isRowSelectCol())
        {//注意，如果是行排序列，则不显示相应的行排序输入框或按钮，因为新增的行没有行排序功能，所以这里只处理这两种列
            col_displayvalue=super.getColDisplayValue(cbean,null,rowInterceptorObj,null,null,rowidx,true);
        }else
        {
            col_displayvalue="&nbsp;";
        }
        return col_displayvalue;
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
    
    protected String getColDisplayValue(ColBean cbean,AbsReportDataPojo dataObj,RowDataBean rowDataByInterceptor,StringBuffer tdPropBuf,Object colDataObj,int rowidx,boolean isReadonlyByInterceptor)
    {
        String col_displayvalue;
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        if(isReadonlyCol(cbean,colDataObj,isReadonlyByInterceptor))
        {
            col_displayvalue=super.getColDisplayValue(cbean,dataObj,rowDataByInterceptor,tdPropBuf,colDataObj,rowidx,isReadonlyByInterceptor);
            if(ercbean!=null) col_displayvalue=ercbean.getRealColDisplayValue(rrequest,dataObj,col_displayvalue);
        }else
        {
            col_displayvalue=showInputBoxForCol(tdPropBuf,ercbean,rowidx,dataObj,cbean,(EditableReportColDataBean)colDataObj);
        }
        return col_displayvalue;
    }

    protected String showInputBoxForCol(StringBuffer tdPropBuf,EditableReportColBean ercbean,int rowindex,AbsReportDataPojo dataObj,ColBean cbean,
            EditableReportColDataBean ercdatabean)
    {
        String col_displayvalue;
        boolean isReadonlyPermission=cbean.checkReadonlyPermission(rrequest);
        if(ercbean.getInputbox().isDisplayOnClick())
        {
            if(!isReadonlyPermission)
            {
                tdPropBuf.append(" onclick=\"try{fillInputBoxOnClick(event);}catch(e){logErrorsAsJsFileLoad(e);}\"");
            }
            if(ercdatabean.isNeedDefaultValue())
            {//如果当前是显示输入框的默认值
                col_displayvalue=ercdatabean.getDefaultvalue();
            }else
            {
                col_displayvalue=dataObj.getColStringValue(cbean);
                if(col_displayvalue==null||col_displayvalue.equals("null")) col_displayvalue="";
            }
            if(ercbean!=null) col_displayvalue=ercbean.getRealColDisplayValue(rrequest,dataObj,col_displayvalue);
        }else
        {
            rrequest.getAttributes().put("DYN_INPUTBOX_ID",ercbean.getInputBoxId()+"__"+rowindex);
            col_displayvalue=ercbean.getInputbox().getDisplayStringValue(rrequest,ercdatabean.getValue(),
                    "style=\"text-align:"+ercbean.getTextalign()+";\"",isReadonlyPermission);
            rrequest.getAttributes().remove("DYN_INPUTBOX_ID");
        }
        return col_displayvalue;
    }

    protected String getColDisplayValueWrapStart(ColBean cbean,boolean isInProperty,boolean isReadonly,boolean ignoreFillmode)
    {
        if(isReadonly) return super.getColDisplayValueWrapStart(cbean,isInProperty,isReadonly,ignoreFillmode);
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.getColDisplayValueWrapStart(cbean,isInProperty,isReadonly,ignoreFillmode));
        String startTag="<";
        String endTag=">";
        if(isInProperty)
        {
            startTag="&lt;";
            endTag="&gt;";
        }
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        String beforedesc=ercbean.getInputbox().getBeforedescription(rrequest);
        if(beforedesc!=null&&!beforedesc.trim().equals("")&&(ercbean.getInputbox().isDisplayOnClick()||ignoreFillmode))
        {
            if(isInProperty)
            {
                beforedesc=Tools.replaceAll(beforedesc,"<","&lt;");
                beforedesc=Tools.replaceAll(beforedesc,">","&gt;");
            }
            resultBuf.append(beforedesc);//有前缀描述信息
        }
        resultBuf.append(startTag+"span tagtype='COL_CONTENT_WRAP'"+endTag);
        return resultBuf.toString();
    }

    protected String getColDisplayValueWrapEnd(ColBean cbean,boolean isInProperty,boolean isReadonly,boolean ignoreFillmode)
    {
        if(isReadonly) return super.getColDisplayValueWrapEnd(cbean,isInProperty,isReadonly,ignoreFillmode);
        StringBuffer resultBuf=new StringBuffer();
        String startTag="<";
        String endTag=">";
        if(isInProperty)
        {
            startTag="&lt;";
            endTag="&gt;";
        }
        resultBuf.append(startTag).append("/span").append(endTag);
        EditableReportColBean ercbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
        String afterdesc=ercbean.getInputbox().getAfterdescription(rrequest);
        if(afterdesc!=null&&!afterdesc.trim().equals("")&&(ercbean.getInputbox().isDisplayOnClick()||ignoreFillmode))
        {
            if(isInProperty)
            {
                afterdesc=Tools.replaceAll(afterdesc,"<","&lt;");
                afterdesc=Tools.replaceAll(afterdesc,">","&gt;");
            }
            resultBuf.append(afterdesc);
        }
        resultBuf.append(super.getColDisplayValueWrapEnd(cbean,isInProperty,isReadonly,ignoreFillmode));
        return resultBuf.toString();
    }

    protected boolean isReadonlyCol(ColBean cbean,Object colDataObj,boolean isReadonlyByInterceptor)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return true;
        if(!(colDataObj instanceof EditableReportColDataBean)||isReadonlyByInterceptor) return true;
        if(!((EditableReportColDataBean)colDataObj).isEditable()||this.realAccessMode.equals(Consts.READONLY_MODE)) return true;
        if(cbean.checkReadonlyPermission(rrequest)) return true;
        return false;
    }
    
    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            int[] maxminrownums=getMaxMinRownums();//获取到指定的最大小记录数
            if(maxminrownums[0]>0&&maxminrownums[0]==maxminrownums[1]) return false;
            return true;
        }else if(buttonType instanceof DeleteButton)
        {
            if(ersqlbean.getDeletebean()==null) return false;
            return true;
        }else if(buttonType instanceof SaveButton)
        {
            if(ersqlbean.getUpdatebean()!=null||ersqlbean.getInsertbean()!=null) return true;
        }
        return false;
    }

    public int afterColLoading(ColBean colbean,List<XmlElementBean> lstEleColBeans)
    {
        super.afterColLoading(colbean,lstEleColBeans);
        ComponentConfigLoadManager.loadEditableColConfig(colbean,lstEleColBeans.get(0),KEY);
        return 1;
    }

    public int afterDisplayLoading(DisplayBean disbean,List<XmlElementBean> lstEleDisplayBeans)
    {
        super.afterDisplayLoading(disbean,lstEleDisplayBeans);
        EditableListReportDisplayBean elrdbean=(EditableListReportDisplayBean)disbean.getExtendConfigDataForReportType(KEY);
        if(elrdbean==null)
        {
            elrdbean=new EditableListReportDisplayBean(disbean);
            disbean.setExtendConfigDataForReportType(KEY,elrdbean);
        }
        Map<String,String> mJoinedAttributes=ConfigLoadAssistant.getInstance().assembleAllAttributes(lstEleDisplayBeans,
                new String[] { "maxrownum", "minrownum" });
        String maxrownum=mJoinedAttributes.get("maxrownum");
        if(maxrownum!=null)
        {
            if(maxrownum.trim().equals(""))
            {
                elrdbean.setMaxrownum(-1);
            }else
            {
                elrdbean.setMaxrownum(Integer.parseInt(maxrownum.trim()));
            }
        }
        String minrownum=mJoinedAttributes.get("minrownum");
        if(minrownum!=null)
        {
            if(minrownum.trim().equals(""))
            {
                elrdbean.setMinrownum(0);
            }else
            {
                elrdbean.setMinrownum(Integer.parseInt(minrownum.trim()));
            }
        }
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
            String insertstyle=eleInsertBean.attributeValue("style");
            if(insertstyle!=null) ersqlbean.getInsertbean().setInsertstyle(insertstyle.toLowerCase().trim());
            String addposition=eleInsertBean.attributeValue("addposition");
            if(addposition!=null)
            {
                ersqlbean.getInsertbean().setAddposition(addposition.trim());
            }
            String callbackmethod=eleInsertBean.attributeValue("callbackmethod");
            if(callbackmethod!=null) ersqlbean.getInsertbean().setCallbackmethod(callbackmethod.trim());
        }
        if(ersqlbean!=null&&ersqlbean.getUpdatebean()!=null)
        {
            XmlElementBean eleUpdateBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"update");
            String crosspage=eleUpdateBean.attributeValue("crosspage");
            if(crosspage!=null) ersqlbean.getUpdatebean().setEnableCrossPageEdit(crosspage.toLowerCase().trim().equals("true"));
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
        processEditableButtons(ersqlbean);
        AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(ersqlbean.getInsertbean()!=null||ersqlbean.getDeletebean()!=null)
        {
            if(alrbean.getRowSelectType()==null||alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_NONE))
            {//默认为multiply
                alrbean.setRowSelectType(Consts.ROWSELECT_MULTIPLE);
            }
        }
//        {//如果允许跨页编辑，则必须要允许跨页行选中功能
//            {//默认为multiply
//            alrbean.setSelectRowCrossPages(true);
        if((ersqlbean.getInsertbean()!=null||ersqlbean.getUpdatebean()!=null)&&(alrbean.getFixedcols(null)>0||alrbean.getFixedrows()>0))
        {//如果配置了<insert/>或<update/>，且配置为冻结行或列标题
            throw new WabacusConfigLoadingException("加载报表"+reportbean.getPath()
                    +"失败，editablelist2/listform报表类型如果配置了编辑功能时，不允许冻结其行或列标题，可以考虑采用editablelist报表类型进行编辑");
        }
        /*
         * 校验在<display/>中配置的maxrownum/minrownum的合法性
         */
        EditableListReportDisplayBean elrdbean=(EditableListReportDisplayBean)reportbean.getDbean().getExtendConfigDataForReportType(KEY);
        int[] rownums=new int[2];
        rownums[0]=elrdbean.getMaxrownum();
        rownums[1]=elrdbean.getMinrownum();
        validateAndCalMaxMinRownum(ersqlbean,rownums);
        elrdbean.setMaxrownum(rownums[0]);
        elrdbean.setMinrownum(rownums[1]);

        EditableReportColBean ercolbean;
        for(ColBean cbean:reportbean.getDbean().getLstCols())
        {
            if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(true))) continue;
            String align=Tools.getPropertyValueByName("align",cbean.getValuestyleproperty(null,true),true);
            if(align==null||align.trim().equals("")) align="left";
            ercolbean=(EditableReportColBean)cbean.getExtendConfigDataForReportType(KEY);
            if(ercolbean==null)
            {
                ercolbean=new EditableReportColBean(cbean);
                cbean.setExtendConfigDataForReportType(KEY,ercolbean);
            }
            ercolbean.setTextalign(align);
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
        if(ersqlbean.getDeletebean()!=null||ersqlbean.getInsertbean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
        if(ersqlbean.getUpdatebean()!=null||ersqlbean.getInsertbean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,SaveButton.class,Consts.SAVE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(SaveButton.class);
        }
    }

    private void validateAndCalMaxMinRownum(EditableReportSqlBean ersbean,int[] rownums)
    {
        ReportBean reportbean=ersbean.getOwner().getReportBean();
        int maxrownum=rownums[0];
        int minrownum=rownums[1];
        if(maxrownum==0)
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"的maxrownum不能指定为0");
        }
        if(maxrownum<-1)
        {
            log.warn("报表"+reportbean.getPath()+"maxrownum："+maxrownum+"不合法，将用默认值-1");
            maxrownum=-1;
        }
        if(minrownum<0)
        {
            log.warn("报表"+reportbean.getPath()+"的minrownum指定为小于0的数不合法，将用默认值0");
            minrownum=0;
        }
        if(ersbean.getInsertbean()==null&&minrownum!=0)
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"没有配置添加记录功能，因此不能指定minrownum为不为0的数");
        }
        if(maxrownum>0&&maxrownum<minrownum)
        {
            throw new WabacusConfigLoadingException("报表"+reportbean.getPath()+"的maxrownum属性指定值必须大于等于minrownum属性值");
        }
        rownums[0]=maxrownum;
        rownums[1]=minrownum;
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
        return Consts_Private.REPORT_FAMILY_EDITABLELIST2;
    }
}
