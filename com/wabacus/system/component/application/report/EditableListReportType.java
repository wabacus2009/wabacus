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

import com.wabacus.config.Config;
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
import com.wabacus.system.component.application.report.abstractreport.AbsListReportType;
import com.wabacus.system.component.application.report.abstractreport.IEditableReportType;
import com.wabacus.system.component.application.report.abstractreport.SaveInfoDataBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportBean;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportColBean;
import com.wabacus.system.component.application.report.configbean.UltraListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportDisplayBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportInsertDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportInsertUpdateBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableListReportUpdateDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportSqlBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.system.dataset.update.action.AbsUpdateAction;
import com.wabacus.system.intercept.IInterceptor;
import com.wabacus.system.intercept.RowDataBean;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class EditableListReportType extends UltraListReportType implements IEditableReportType
{
    public final static String KEY=EditableListReportType.class.getName();

    protected EditableListReportDisplayBean elrdbean=null;

    protected EditableReportSqlBean ersqlbean=null;

    protected String realAccessMode;

    public EditableListReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
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
            rrequest.authorize(rbean.getId(),Consts.DATA_PART,"{editablelist-edit}","display","false");
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
        if(sidbean==null||!sidbean.hasDeleteData()) return;
        lstAllUpdateActions.addAll(ersqlbean.getDeletebean().getLsAllEditActions());
    }
    
    public int[] doSaveAction() throws SQLException
    {
        int[] result=new int[] { IInterceptor.WX_RETURNVAL_SKIP, 0 };
        SaveInfoDataBean sidbean=(SaveInfoDataBean)rrequest.getAttribute(rbean.getId(),"SAVEINFO_DATABEAN");
        if(sidbean==null||!sidbean.hasDeleteData()) return result;
        boolean[] shouldDoSave=sidbean.getShouldDoSave();
        if(shouldDoSave.length!=4) return result;
        if(rbean.getInterceptor()!=null)
        {
            result[0]=rbean.getInterceptor().doSave(this.rrequest,this.rbean,ersqlbean.getDeletebean());
        }else
        {
            result[0]=EditableReportAssistant.getInstance().doSaveReport(this.rrequest,this.rbean,ersqlbean.getDeletebean());
        }
        if(result[0]==IInterceptor.WX_RETURNVAL_TERMINATE||result[0]==IInterceptor.WX_RETURNVAL_SKIP) return result;
        result[1]=IEditableReportType.IS_DELETE_DATA;
        result[0]=EditableReportAssistant.getInstance().processAfterSaveAction(rrequest,rbean,"delete",result[0]);
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
        rrequest.setAttribute(rbean.getId()+"_ACCESSMODE",newaccessmode);
        rrequest.setAttribute(rbean.getId(),"CURRENT_ACCESSMODE",newaccessmode);
        rrequest.addParamToUrl(rbean.getId()+"_ACCESSMODE",newaccessmode,true);
        if(Consts.READONLY_MODE.equals(newaccessmode))
        {//新的模式是readonly
            rrequest.setAttribute(rbean.getId()+"_isReadonlyAccessmode","true");
        }else
        {
            rrequest.getAttributes().remove(rbean.getId()+"_isReadonlyAccessmode");
        }
    }

    protected String showMetaDataDisplayStringStart()
    {
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(super.showMetaDataDisplayStringStart());
        resultBuf.append(EditableReportAssistant.getInstance().getEditableMetaData(this));
        return resultBuf.toString();
    }

    public String getColOriginalValue(AbsReportDataPojo object,ColBean cbean)
    {
        String oldvalue=ReportAssistant.getInstance().getPropertyValueAsString(object,cbean.getProperty()+"_old",cbean.getDatatypeObj());
        if(oldvalue==null||oldvalue.equals("null"))
        {
            oldvalue="";
        }
        return oldvalue;
    }
    
    protected Object initDisplayCol(ColBean cbean,AbsReportDataPojo rowDataObjTmp)
    {
        if(cbean.isSequenceCol()||cbean.isControlCol()) return super.initDisplayCol(cbean,rowDataObjTmp);
        if(cbean.getProperty()==null||cbean.getProperty().trim().equals("")) return super.initDisplayCol(cbean,rowDataObjTmp);
        AbsListReportColBean alrcbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
        if(alrcbean==null) return super.initDisplayCol(cbean,rowDataObjTmp);
        String col_editvalue=getColOriginalValue(rowDataObjTmp,cbean);
        return EditableReportColDataBean.createInstance(rrequest,this.cacheDataBean,null,cbean,col_editvalue,this.currentSecretColValuesBean);
    }
    
    protected String getTdPropertiesForCol(ColBean cbean,Object colDataObj,int rowidx,boolean isCommonRowGroupCol)
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return "";
        if(!(colDataObj instanceof EditableReportColDataBean)) return super.getTdPropertiesForCol(cbean,colDataObj,rowidx,isCommonRowGroupCol);
        EditableReportColDataBean ercdatabean=(EditableReportColDataBean)colDataObj;
        StringBuilder resultBuf=new StringBuilder();
        String tdSuperProperties=super.getTdPropertiesForCol(cbean,ercdatabean,rowidx,isCommonRowGroupCol);
        resultBuf.append(tdSuperProperties);
        if(tdSuperProperties.indexOf(" value_name=")<0)
        {
            resultBuf.append(" value_name=\""+ercdatabean.getValuename()+"\"");
        }
        if(tdSuperProperties.indexOf(" value=")<0)
        {
            resultBuf.append(" value=\"").append(Tools.htmlEncode(ercdatabean.getValue())).append("\"");
        }
        if(tdSuperProperties.indexOf(" oldvalue=")<0)
        {
            resultBuf.append(" oldvalue=\""+Tools.htmlEncode(ercdatabean.getOldvalue())+"\"");
        }
        if(isCommonRowGroupCol)
        {//如果是普通分组的列，则必须显示id属性，以便它的数据行根据parentgroupid属性能找到此<td/>
            resultBuf.append(" id=\"").append(EditableReportAssistant.getInstance().getInputBoxId(cbean)).append("__td"+rowidx+"\" ");
        }
        return resultBuf.toString();
    }
    
    protected String getColDisplayValue(ColBean cbean,AbsReportDataPojo dataObj,RowDataBean rowDataByInterceptor,StringBuffer tdPropBuf,Object colDataObj,int rowidx,boolean isReadonly)
    {
        if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&cbean.isEditableListEditCol())
        {//如果是行编辑列
            String roweditcolvalue=cbean.getTagcontent(rrequest);
            if(roweditcolvalue==null||roweditcolvalue.trim().equals(""))
            {//如果没有在<col></col>中定义此列的显示内容，则取默认显示内容
                roweditcolvalue=Config.getInstance().getResourceString(rrequest,this.rbean.getPageBean(),"${editablelist.editcol}",true);
            }
            roweditcolvalue=rrequest.getI18NStringValue(roweditcolvalue);
            EditableListReportUpdateDataBean elrudbean=(EditableListReportUpdateDataBean)ersqlbean.getUpdatebean();
            String col_displayvalue=null;
            if(isReadonly||rrequest.checkPermission(this.rbean.getId(),Consts.DATA_PART,Consts_Private.COL_EDITABLELIST_EDIT,Consts.PERMISSION_TYPE_DISABLED))
            {
                col_displayvalue="<span class='cls-editablelist-disabledcol'>"+roweditcolvalue+"</span>";
            }else
            {
                col_displayvalue="<a  onClick=\"popupEditReportPage('"+elrudbean.getPopupPageUrlAndParams(rrequest,this,dataObj)+"','"
                        +Tools.jsParamEncode(elrudbean.getRealUpdateBean().getPopupparams())+"');\">"+roweditcolvalue+"</a>";
            }
            return col_displayvalue;
        }
        return super.getColDisplayValue(cbean,dataObj,rowDataByInterceptor,tdPropBuf,
                colDataObj instanceof EditableReportColDataBean?((EditableReportColDataBean)colDataObj).getEditvalue():colDataObj,rowidx,isReadonly);
    }
    
    public String getAddEvent()
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)||ersqlbean.getInsertbean()==null) return "";
        StringBuffer resultBuf=new StringBuffer();
        EditableListReportInsertDataBean elrudbean=(EditableListReportInsertDataBean)ersqlbean.getInsertbean();
        resultBuf.append("popupEditReportPage('").append(elrudbean.getPopupPageUrlAndParams(rrequest,this,null)).append("','");
        resultBuf.append(Tools.jsParamEncode(elrudbean.getRealInsertBean().getPopupparams())+"')");
        return resultBuf.toString();
    }

    public boolean needCertainTypeButton(AbsButtonType buttonType)
    {
        if(this.realAccessMode.equals(Consts.READONLY_MODE)) return false;
        if(buttonType instanceof AddButton)
        {
            if(ersqlbean.getInsertbean()==null) return false;
            return true;
        }else if(buttonType instanceof DeleteButton)
        {
            if(ersqlbean.getDeletebean()==null) return false;
            return true;
        }
        return false;
    }

    public int afterSqlLoading(SqlBean sqlbean,List<XmlElementBean> lstEleSqlBeans)
    {
        super.afterSqlLoading(sqlbean,lstEleSqlBeans);
        ComponentConfigLoadManager.loadEditableSqlConfig(sqlbean,lstEleSqlBeans,KEY);
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null)
        {
            ersqlbean=new EditableReportSqlBean(sqlbean);
            sqlbean.setExtendConfigDataForReportType(KEY,ersqlbean);
        }
        XmlElementBean eleInsertBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"insert");
        if(eleInsertBean!=null)
        {
            EditableListReportInsertDataBean insertBean=new EditableListReportInsertDataBean(ersqlbean);
            insertBean.setRealInsertBean(new EditableListReportInsertUpdateBean(insertBean));
            loadInsertUpdateConfig(sqlbean,eleInsertBean,insertBean.getRealInsertBean());
            ersqlbean.setInsertbean(insertBean);
        }
        XmlElementBean eleUpdateBean=ComponentConfigLoadManager.getEleSqlUpdateBean(lstEleSqlBeans,"update");
        if(eleUpdateBean!=null)
        {
            EditableListReportUpdateDataBean updateBean=new EditableListReportUpdateDataBean(ersqlbean);
            updateBean.setRealUpdateBean(new EditableListReportInsertUpdateBean(updateBean));
            loadInsertUpdateConfig(sqlbean,eleUpdateBean,updateBean.getRealUpdateBean());
            ersqlbean.setUpdatebean(updateBean);
        }
        return 1;
    }

    private void loadInsertUpdateConfig(SqlBean sqlbean,XmlElementBean eleInsertUpdateBean,EditableListReportInsertUpdateBean insertUpdateBean)
    {
        String pageurl=eleInsertUpdateBean.attributeValue("pageurl");
        if(pageurl==null||pageurl.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，这种报表类型必须为其<insert/>和<update/>指定pageurl属性");
        }
        pageurl=pageurl.trim();
        if(Tools.isDefineKey("report",pageurl))
        {
            String realpageurl=Tools.getRealKeyByDefine("report",pageurl);
            int idx=realpageurl.indexOf(".");
            if(idx<=0||idx==realpageurl.length()-1)
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，这种报表类型必须为其<insert/>和<update/>指定pageurl属性："
                        +pageurl+"不合法");
            }
            insertUpdateBean.setPageid(realpageurl.substring(0,idx).trim());
            insertUpdateBean.setReportid(realpageurl.substring(idx+1).trim());
            if(insertUpdateBean.getPageid().equals("")||insertUpdateBean.getReportid().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，这种报表类型必须为其<insert/>和<update/>指定pageurl属性："
                        +pageurl+"不合法");
            }
        }else
        {
            if(!pageurl.startsWith(Config.webroot)&&!pageurl.toLowerCase().startsWith("http://"))
            {
                pageurl=Tools.replaceAll(Config.webroot+"/"+pageurl,"//","/");
            }
            insertUpdateBean.setPageurl(pageurl.trim());
        }
        String urlparams=eleInsertUpdateBean.attributeValue("urlparams");
        if(urlparams!=null&&!urlparams.trim().equals(""))
        {
            List<Map<String,String>> lstUrlParams=new ArrayList<Map<String,String>>();
            List<String> lstParams=Tools.parseStringToList(urlparams,";",false);
            for(String paramTmp:lstParams)
            {
                if(paramTmp==null||paramTmp.trim().equals("")) continue;
                paramTmp=paramTmp.trim();
                int idxEquals=paramTmp.indexOf("=");
                if(idxEquals<=0)
                {
                    throw new WabacusConfigLoadingException("加载报表"+sqlbean.getReportBean().getPath()+"失败，为其<insert/>和<update/>指定urlparams属性中的："
                            +paramTmp+"参数不合法，没有=号，或者没有参数名");
                }
                Map<String,String> mParamTmp=new HashMap<String,String>();
                mParamTmp.put(paramTmp.substring(0,idxEquals).trim(),paramTmp.substring(idxEquals+1).trim());
                lstUrlParams.add(mParamTmp);
            }
            insertUpdateBean.setLstUrlParams(lstUrlParams);
        }
        String popupparams=eleInsertUpdateBean.attributeValue("popupparams");
        if(popupparams!=null) insertUpdateBean.setPopupparams(popupparams.trim());
        String pageinitsize=eleInsertUpdateBean.attributeValue("pageinitsize");
        if(pageinitsize!=null) insertUpdateBean.setPageinitsize(pageinitsize.toLowerCase().trim());
        String beforepopup=eleInsertUpdateBean.attributeValue("beforepopup");
        if(beforepopup!=null) insertUpdateBean.setBeforepopup(beforepopup.trim());
    }

    public int doPostLoad(ReportBean reportbean)
    {
        SqlBean sqlbean=reportbean.getSbean();
        if(sqlbean==null) return 1;
        EditableReportSqlBean ersqlbean=(EditableReportSqlBean)sqlbean.getExtendConfigDataForReportType(KEY);
        if(ersqlbean==null) return 1;
        processRowEditCol(ersqlbean);//放在super.doPostLoad()方法之前，因为在super.doPostLoad()方法中要对列进行calPosition，所以必须先生成这个列
        super.doPostLoad(reportbean);
        ComponentConfigLoadManager.doEditableReportTypePostLoad(reportbean,KEY);
        processEditableButtons(ersqlbean);
        if(ersqlbean.getDeletebean()!=null)
        {
            AbsListReportBean alrbean=(AbsListReportBean)reportbean.getExtendConfigDataForReportType(AbsListReportType.KEY);
            if(alrbean==null)
            {
                alrbean=new AbsListReportBean(reportbean);
                reportbean.setExtendConfigDataForReportType(AbsListReportType.KEY,alrbean);
            }
            if(alrbean.getRowSelectType()==null||alrbean.getRowSelectType().trim().equals(Consts.ROWSELECT_NONE))
            {
                alrbean.setRowSelectType(Consts.ROWSELECT_MULTIPLE);
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
        if(ersqlbean.getDeletebean()!=null)
        {
            ComponentConfigLoadAssistant.getInstance().checkAndAddButtons(reportbean,DeleteButton.class,Consts.DELETE_BUTTON_DEFAULT);
        }else if(reportbean.getButtonsBean()!=null)
        {
            reportbean.getButtonsBean().removeAllCertainTypeButtons(DeleteButton.class);
        }
    }
    
    protected void processRowEditCol(EditableReportSqlBean ersqlbean)
    {
        ReportBean reportbean=ersqlbean.getOwner().getReportBean();
        DisplayBean disbean=reportbean.getDbean();
        List<ColBean> lstCols=disbean.getLstCols();
        UltraListReportDisplayBean ulrdbean=(UltraListReportDisplayBean)disbean.getExtendConfigDataForReportType(UltraListReportType.KEY);
        if(ersqlbean.getUpdatebean()==null)
        {
            for(int i=lstCols.size()-1;i>=0;i--)
            {
                if(lstCols.get(i).isEditableListEditCol())
                {
                    lstCols.remove(i);
                }
            }
            if(ulrdbean!=null)
            {
                ulrdbean.removeChildColBeanByColumn(Consts_Private.COL_EDITABLELIST_EDIT,true);
            }
        }else
        {
            boolean hasEditCol=false;
            for(ColBean cbTmp:lstCols)
            {
                if(cbTmp!=null&&cbTmp.isEditableListEditCol())
                {
                    hasEditCol=true;
                    cbTmp.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
                }
            }
            if(!hasEditCol)
            {//如果用户没有配置行编辑列，则自动生成一个
                ColBean cbNewRowEditCol=new ColBean(disbean);
                cbNewRowEditCol.setColumn(Consts_Private.COL_EDITABLELIST_EDIT);
                cbNewRowEditCol.setProperty(Consts_Private.COL_EDITABLELIST_EDIT);
                AbsListReportColBean alrcbean=new AbsListReportColBean(cbNewRowEditCol);
                cbNewRowEditCol.setExtendConfigDataForReportType(AbsListReportType.KEY,alrcbean);
                cbNewRowEditCol.setLabel(Config.getInstance().getResourceString(null,disbean.getPageBean(),"${editablelist.editcol.label}",false));
                cbNewRowEditCol.setDisplaytype(new String[]{Consts.COL_DISPLAYTYPE_ALWAYS,Consts.COL_DISPLAYTYPE_ALWAYS});
                cbNewRowEditCol.setLabelstyleproperty("style=\"text-align:center;vertical-align:middle;\"",true);
                cbNewRowEditCol.setValuestyleproperty("style=\"text-align:center;vertical-align:middle;\"",true);
                lstCols.add(cbNewRowEditCol);
                if(ulrdbean!=null&&ulrdbean.getLstChildren()!=null)
                {
                    UltraListReportColBean ulrcbean=new UltraListReportColBean(cbNewRowEditCol);
                    cbNewRowEditCol.setExtendConfigDataForReportType(UltraListReportType.KEY,ulrcbean);
                    ulrdbean.getLstChildren().add(cbNewRowEditCol);
                }
            }
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
        return Consts_Private.REPORT_FAMILY_EDITABLELIST;
    }
}
