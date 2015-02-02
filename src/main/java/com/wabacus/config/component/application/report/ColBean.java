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
package com.wabacus.config.component.application.report;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.datatype.IDataType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;
import com.wabacus.util.Tools;

public class ColBean extends AbsConfigBean
{
    private String colid;
    
    private String property;

    private String column;

    private List<String> lstDatasetValueids;
    
    private String label;//列的标题信息
    
    private Map<String,String> mDynLableParts;

    private String[] displaytypes=new String[]{Consts.COL_DISPLAYTYPE_INITIAL,Consts.COL_DISPLAYTYPE_INITIAL};
    
    private String tagcontent;
    
    private Map<String,String> mDynTagcontentParts;
    
    private boolean isI18n;//是否支持国际化

    private IDataType datatypeObj;
 
    private String labelstyleproperty;
    
    private List<String> lstDynLabelstylepropertyParts;
    
    private String valuestyleproperty;
    
    private List<String> lstDynValuestylepropertyParts;//valuestyleproperty中的动态部分，key为此动态值的在valuestyleproperty中的占位符，值为request{xxx}、session{key}、url{key}等等形式，用于运行时得到真正值
    
    private String labelalign;
    
    private String valuealign;
    
    private float plainexcelwidth;
    
    private float pdfwidth;
    
    private String printwidth;//打印时的宽度
    
    private String printlabelstyleproperty;
    
    private List<String> lstDynPrintlabelstylepropertyParts;
    
    private String printvaluestyleproperty;
    
    private List<String> lstDynPrintvaluestylepropertyParts;
    
    private Method setMethod=null;

    private Method getMethod=null;

    public final static String NON_LABEL="{non-label}";//不显示label的

//    private Map<String,String> mFormatParamsColProperties;//存放当前列的所有格式化方法参数中用到的其它<col/>的定义property(即@{}格式)和真正property
    
    private boolean isDisplayNameValueProperty;//当前列是否需要在<col/>中通过<td/>或<font/>的value属性显示value_name="name" value="value"的属性
    
    public ColBean(AbsConfigBean parent)
    {
        super(parent);
        this.colid=String.valueOf(((DisplayBean)parent).generate_childid());
    }

    public ColBean(AbsConfigBean parent,int colid)
    {
        super(parent);
        this.colid=String.valueOf(colid);
    }
    
    public String getColid()
    {
        return colid;
    }

    public void setColid(String colid)
    {
        this.colid=colid;
    }

    public float getPlainexcelwidth()
    {
        return plainexcelwidth;
    }

    public void setPlainexcelwidth(float plainexcelwidth)
    {
        this.plainexcelwidth=plainexcelwidth;
    }

    public float getPdfwidth()
    {
        return pdfwidth;
    }

    public void setPdfwidth(float pdfwidth)
    {
        this.pdfwidth=pdfwidth;
    }

    public Method getGetMethod()
    {
        return getMethod;
    }

    public void setGetMethod(Method getMethod)
    {
        this.getMethod=getMethod;
    }

    public Method getSetMethod()
    {
        return setMethod;
    }

    public void setSetMethod(Method setMethod)
    {
        this.setMethod=setMethod;
    }

    public boolean isI18n()
    {
        return isI18n;
    }

    public void setI18n(boolean isI18n)
    {
        this.isI18n=isI18n;
    }

    public String getPrintwidth()
    {
        return printwidth;
    }

    public void setPrintwidth(String printwidth)
    {
        this.printwidth=printwidth;
    }

    public String getPrintlabelstyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.printlabelstyleproperty==null?"":this.printlabelstyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.printlabelstyleproperty,this.lstDynPrintlabelstylepropertyParts,"");
    }
    
    public void setPrintlabelstyleproperty(String printlabelstyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.printlabelstyleproperty=printlabelstyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(printlabelstyleproperty);
            this.printlabelstyleproperty=(String)objArr[0];
            this.lstDynPrintlabelstylepropertyParts=(List<String>)objArr[1];
        }
    }

    public String getPrintvaluestyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(isStaticPart) return this.printvaluestyleproperty==null?"":this.printvaluestyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.printvaluestyleproperty,this.lstDynPrintvaluestylepropertyParts,"");
    }
    
    public void setPrintvaluestyleproperty(String printvaluestyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.printvaluestyleproperty=printvaluestyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(printvaluestyleproperty);
            this.printvaluestyleproperty=(String)objArr[0];
            this.lstDynValuestylepropertyParts=(List<String>)objArr[1];
        }
    }

    public void setProperty(String property)
    {
        this.property=property;
    }

    public void setColumn(String column)
    {
        if(Tools.isDefineKey("i18n",column))
        {
            String columnTemp=Tools.getRealKeyByDefine("i18n",column);
            if(columnTemp.trim().equals(""))
            {
                throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"配置的列"
                        +column+"不合法");
            }
            setI18n(true);
            this.column=columnTemp;
        }else
        {
            setI18n(false);
            this.column=column;
        }
    }

    public void setLabel(String label)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getPageBean(),label);
        this.label=(String)objArr[0];
        this.mDynLableParts=(Map<String,String>)objArr[1];
    }

    public String getDisplaytype(boolean isPageDisplaytype)
    {
        return isPageDisplaytype?displaytypes[0]:displaytypes[1];
    }

    public String getDisplaytype(ReportRequest rrequest)
    {
        return rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE?displaytypes[0]:displaytypes[1];
    }
    
    public void setDisplaytype(String[] displaytypes)
    {
        if(displaytypes==null||displaytypes.length==0)
        {
            this.displaytypes=new String[] { Consts.COL_DISPLAYTYPE_INITIAL, Consts.COL_DISPLAYTYPE_INITIAL };
        }else if(displaytypes.length==1)
        {
            this.displaytypes=new String[] { displaytypes[0], displaytypes[0] };
        }else
        {
            this.displaytypes=new String[] { displaytypes[0], displaytypes[1] };
        }
        if(Tools.isEmpty(this.displaytypes[0])) this.displaytypes[0]=Consts.COL_DISPLAYTYPE_INITIAL;
        if(Tools.isEmpty(this.displaytypes[1])) this.displaytypes[1]=Consts.COL_DISPLAYTYPE_INITIAL;
        if(!Consts.lstAllColDisplayTypes.contains(this.displaytypes[0]))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的列"+this.column+"失败，配置的displaytype属性"+this.displaytypes[0]
                    +"不支持");
        }
        if(!Consts.lstAllColDisplayTypes.contains(this.displaytypes[1]))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"的列"+this.column+"失败，配置的displaytype属性"+this.displaytypes[1]
                    +"不支持");
        }
        if(!this.displaytypes[0].equals(this.displaytypes[1])) ((DisplayBean)this.getParent()).setAllColDisplaytypesEquals(false);//只要有一个列在页面和导出文件中显示模式不同，则整个报表都不同
    }

    public String getProperty()
    {
        return this.property;
    }

    public String getColumn()
    {
        return this.column;
    }

    public String getLabel(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.label,this.mDynLableParts,"");
    }

    public void setLstDatasetValueids(List<String> lstDatasetValueids)
    {
        this.lstDatasetValueids=lstDatasetValueids;
    }

    public List<String> getLstDatasetValueids()
    {
        return lstDatasetValueids;
    }

    public IDataType getDatatypeObj()
    {
        return datatypeObj;
    }

    public void setDatatypeObj(IDataType datatypeObj)
    {
        this.datatypeObj=datatypeObj;
    }
    
    public String getLabelstyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(rrequest!=null&&rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return this.getPrintlabelstyleproperty(rrequest,isStaticPart);
        if(isStaticPart) return this.labelstyleproperty==null?"":this.labelstyleproperty;
        String reallabelstyleproperty=WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.labelstyleproperty,
                this.lstDynLabelstylepropertyParts,"");
        if(rrequest!=null&&rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE&&this.getReportBean().isListReportType())
        {
            String stylevalue=Tools.getPropertyValueByName("style",reallabelstyleproperty,false);
            if(stylevalue==null) stylevalue="";
            if(!stylevalue.trim().equals("")&&!stylevalue.endsWith(";")) stylevalue=stylevalue+";";
            if(stylevalue.toLowerCase().indexOf("text-align")<0)
            {
                stylevalue=stylevalue+"text-align:center;";
            }
            if(stylevalue.toLowerCase().indexOf("vertical-align")<0)
            {
                stylevalue=stylevalue+"vertical-align:middle;";
            }
            reallabelstyleproperty=Tools.removePropertyValueByName("style",reallabelstyleproperty);
            reallabelstyleproperty=reallabelstyleproperty+" style=\""+stylevalue+"\"";
        }
        return reallabelstyleproperty;
    }
    
    public void setLabelstyleproperty(String labelstyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.labelstyleproperty=labelstyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(labelstyleproperty);
            this.labelstyleproperty=(String)objArr[0];
            this.lstDynLabelstylepropertyParts=(List<String>)objArr[1];
        }
    }
    
    public String getValuestyleproperty(ReportRequest rrequest,boolean isStaticPart)
    {
        if(rrequest!=null&&rrequest.getShowtype()==Consts.DISPLAY_ON_PRINT) return this.getPrintvaluestyleproperty(rrequest,isStaticPart);
        if(isStaticPart) return this.valuestyleproperty==null?"":this.valuestyleproperty;
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.valuestyleproperty,this.lstDynValuestylepropertyParts,"");
    }
    
    public void setValuestyleproperty(String valuestyleproperty,boolean isStaticPart)
    {
        if(isStaticPart)
        {
            this.valuestyleproperty=valuestyleproperty;
        }else
        {
            Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(valuestyleproperty);
            this.valuestyleproperty=(String)objArr[0];
            this.lstDynValuestylepropertyParts=(List<String>)objArr[1];
        }
    }

    public String getTagcontent(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.tagcontent,this.mDynTagcontentParts,"");
    }

    public void setTagcontent(String tagcontent)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.getPageBean(),tagcontent);
        this.tagcontent=(String)objArr[0];
        this.mDynTagcontentParts=(Map<String,String>)objArr[1];
    }
    
    public String getLabelalign()
    {
        return labelalign;
    }

    public void setLabelalign(String labelalign)
    {
        this.labelalign=labelalign;
    }

    public String getValuealign()
    {
        return valuealign;
    }

    public void setValuealign(String valuealign)
    {
        this.valuealign=valuealign;
    }

    public boolean isMatchDataSet(ReportDataSetValueBean dsvbean)
    {
        if(this.isControlCol()||this.isSequenceCol()||this.isNonFromDbCol()||this.isNonValueCol()) return false;
        if(this.lstDatasetValueids==null||this.lstDatasetValueids.size()==0)
        {//如果没有指定datasetid，则匹配所有数据集（这个时候不可能在一个<dataset/>中配置有多个<value/>，否则在doPostLoad()方法中就会报错）
            return true;
        }
        SqlBean sbean=(SqlBean)dsvbean.getParent().getParent();
        if(sbean.isHorizontalDataset()
                &&(this.column.equals(sbean.getHdsTitleLabelCbean().getColumn())||this.column.equals(sbean.getHdsTitleValueCbean().getColumn())))
            return true;//如果是横向数据集，且当前<col/>就是查询标题行的各列数据或显示label，则返回true，因为所有<value/>都会查询这两列数据
        return this.lstDatasetValueids.contains(dsvbean.getId());
    }
    
    public boolean checkDisplayPermission(ReportRequest rrequest)
    {
        if(!rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.column,Consts.PERMISSION_TYPE_DISPLAY)) return false;
        if(this.property!=null&&!this.property.trim().equals("")&&!this.property.equals(this.column))
        {
            if(!rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.property,Consts.PERMISSION_TYPE_DISPLAY)) return false;
        }
        return true;
    }
    
    public boolean checkReadonlyPermission(ReportRequest rrequest)
    {
        if(rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.column,Consts.PERMISSION_TYPE_READONLY)) return true;
        if(this.property!=null&&!this.property.trim().equals("")&&!this.property.equals(this.column))
        {
            if(rrequest.checkPermission(this.getReportBean().getId(),Consts.DATA_PART,this.property,Consts.PERMISSION_TYPE_READONLY)) return true;
        }
        return false;
    }
    
    public int getDisplaymode(ReportRequest rrequest,List<String> lstDisplayColIds,boolean isForPage)
    {
        DisplayBean dbean=(DisplayBean)this.getParent();
        if(rrequest!=null)
        {
            if(!checkDisplayPermission(rrequest)) return -1;
            if(!isForPage)
            {
                if(this.isControlCol()) return -1;
                if(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE&&!dbean.isAllColDisplaytypesEquals()) lstDisplayColIds=null;
            }
        }
        if(!isForPage&&this.isControlCol()) return -1;
        String displaymode=isForPage?displaytypes[0]:displaytypes[1];
        if(Consts.COL_DISPLAYTYPE_HIDDEN.equals(displaymode)) return 0;
        if(Consts.COL_DISPLAYTYPE_ALWAYS.equals(displaymode)) return 2;
        if(isForPage&&!dbean.isPageColselect()) return 1;//不允许列选择，则只要displaytype不是hidden的列都显示出来
        if(!isForPage&&!dbean.isDataexportColselect()&&(!dbean.isAllColDisplaytypesEquals()||!dbean.isPageColselect())) return 1;
        if(lstDisplayColIds==null||lstDisplayColIds.size()==0)
        {
            if(Consts.COL_DISPLAYTYPE_INITIAL.equals(displaymode)) return 1;
        }else if(lstDisplayColIds.contains(colid))
        {
            return 1;
        }
        return 0;
    }
    
    public boolean isNonValueCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.NON_VALUE);
    }
    
    public boolean isSequenceCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.indexOf("{sequence")==0&&column.indexOf("}")==column.length()-1;
    }
    
    public boolean isNonFromDbCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.NON_FROMDB);
    }
    
    public boolean isRowSelectCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWSELECT);
    }
    
    public boolean isRoworderArrowCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWORDER_ARROW);
    }
    
    public boolean isRoworderInputboxCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWORDER_INPUTBOX);
    }
    
    public boolean isRoworderTopCol()
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(Consts_Private.COL_ROWORDER_TOP);
    }
    
    public boolean isRoworderCol(String rowordertypeColumn)
    {
        if(column==null||column.trim().equals("")) return false;
        return column.equalsIgnoreCase(rowordertypeColumn);
    }
    
    public boolean isRoworderCol()
    {
        return isRoworderArrowCol()||isRoworderInputboxCol()||isRoworderTopCol();
    }
    
    public boolean isEditableListEditCol()
    {
        if(this.column==null||this.column.trim().equals("")) return false;
        return this.column.equalsIgnoreCase(Consts_Private.COL_EDITABLELIST_EDIT);
    }
    
    public boolean isControlCol()
    {
        if(isRowSelectCol()||isRoworderCol()||isEditableListEditCol())
        {
            return true;
        }
        return false;
    }
    
    public boolean isDisplayNameValueProperty()
    {
        return isDisplayNameValueProperty;
    }

    public void setDisplayNameValueProperty(boolean isDisplayNameValueProperty)
    {
        this.isDisplayNameValueProperty=isDisplayNameValueProperty;
    }

    public String getBorderStylePropertyOnColBean()
    {
        ReportBean rb=this.getReportBean();
        String border=rb.getBorder();
        String borderstyle="";
        if(Consts_Private.REPORT_BORDER_NONE0.equals(border)||Consts_Private.REPORT_BORDER_NONE1.equals(border))
        {
            borderstyle="border:none;";
        }else
        {
            String bordercolor=rb.getBordercolor();
            if(bordercolor!=null&&!bordercolor.trim().equals(""))
            {
                borderstyle="border-color:"+bordercolor+";";
            }
           if(Consts_Private.REPORT_BORDER_HORIZONTAL0.equals(border)||Consts_Private.REPORT_BORDER_HORIZONTAL1.equals(border))
           {//只显示横向border
               borderstyle=borderstyle+"border-left:none;border-right:none;";
           }else if(Consts_Private.REPORT_BORDER_VERTICAL.equals(border))
           {
               borderstyle=borderstyle+"border-top:none;border-bottom:none;";
           }
        }
        return borderstyle;
    }
    
    public ColBean getUpdateColBeanDest(boolean isMust)
    {
        EditableReportColBean ercbean=(EditableReportColBean)this.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ercbean==null||ercbean.getUpdatecolDest()==null||ercbean.getUpdatecolDest().trim().equals(""))
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>没有配置updatecol更新其它列");
        }
        ColBean cbTemp=this.getReportBean().getDbean().getColBeanByColProperty(ercbean.getUpdatecolDest());
        if(cbTemp==null)
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecolDest()+"引用的列不存在");
        }
        if(!Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbTemp.getDisplaytype(true)))
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecolDest()+"引用的列不是displaytype为hidden的列");
        }
        if(cbTemp.getProperty()==null||cbTemp.getProperty().trim().equals("")||cbTemp.isNonValueCol()||cbTemp.isSequenceCol()||cbTemp.isControlCol())
        {
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>通过updatecol为"
                    +ercbean.getUpdatecolDest()+"引用的列不是从数据库中获取数据，不能被引用");
        }
        return cbTemp;
    }
    
    public ColBean getUpdateColBeanSrc(boolean isMust)
    {
        EditableReportColBean ercbean=(EditableReportColBean)this.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ercbean==null||ercbean.getUpdatecolSrc()==null||ercbean.getUpdatecolSrc().trim().equals(""))
        {
            if(!isMust) return null;
            throw new WabacusConfigLoadingException("报表"+this.getReportBean().getPath()+"的column属性为"+this.getColumn()+"的<col/>没有被其它列通过updatecol属性引用");
        }
        ColBean cbTemp=this.getReportBean().getDbean().getColBeanByColProperty(ercbean.getUpdatecolSrc());
        if(cbTemp==null)
        {
            throw new WabacusConfigLoadingException("在报表"+this.getReportBean().getPath()+"中没有取到property为"+ercbean.getUpdatecolSrc()+"的列");
        }
        return cbTemp;
    }
    
    public void doPostLoad()
    {
        /*if(formatproperty!=null&&!formatproperty.trim().equals(""))
        {
            this.formatProperties=FormatPropertyBean.convertFormatStringToFormatBean(
                    formatproperty,this);
            this.formatproperty=null;
            mFormatParamsColProperties=new HashMap<String,String>();
            if(this.formatProperties!=null&&this.formatProperties.size()>0)
            {
                for(int i=0;i<this.formatProperties.size();i++)
                {
                    FormatPropertyBean fpropbean=this.formatProperties.get(i);
                    if(fpropbean==null) continue;
                    fpropbean.doPostLoad(mFormatParamsColProperties);
                }
            }
            if(mFormatParamsColProperties.size()==0) mFormatParamsColProperties=null;
        }*/
        if(this.lstDatasetValueids!=null)
        {
            for(int i=this.lstDatasetValueids.size()-1;i>=0;i--)
            {
                if("".equals(this.lstDatasetValueids.get(i)))
                {
                    this.lstDatasetValueids.remove(i);
                }
            }
            if(this.lstDatasetValueids.size()==0) this.lstDatasetValueids=null;
        }
        if(this.isControlCol()||this.isSequenceCol()||this.isNonValueCol()) return;
        SqlBean sbean=this.getReportBean().getSbean();
        if(!this.isNonFromDbCol()&&this.getReportBean().getSbean()!=null
                &&this.getReportBean().getSbean().isMultiDataSetCols()
                &&(this.lstDatasetValueids==null||this.lstDatasetValueids.size()==0)
                &&(!sbean.isHorizontalDataset()||(!sbean.getHdsTitleLabelColumn().equals(this.column)&&!sbean.getHdsTitleValueColumn().equals(
                        this.column))))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.getReportBean().getPath()+"上的列"+this.column
                    +"失败，此报表配置了横向多数据集查询各列数据，因此必须在column中指定数据集ID");
        }
        EditableReportColBean ecolbean=(EditableReportColBean)this.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ecolbean!=null) ecolbean.doPostLoad();
    }
    
    public AbsConfigBean clone(AbsConfigBean parent)
    {
        ColBean cbNew=(ColBean)super.clone(parent);
        cloneExtendConfig(cbNew);
        if(lstDatasetValueids!=null)
        {
            cbNew.lstDatasetValueids=(List<String>)((ArrayList<String>)this.lstDatasetValueids).clone();
        }
        return cbNew;
    }
    
    public int hashCode()
    {
        final int prime=31;
        int result=1;
        result=prime*result+((colid==null)?0:colid.hashCode());
        result=prime*result+((column==null)?0:column.hashCode());
        result=prime*result+((property==null)?0:property.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if(this==obj) return true;
        if(obj==null) return false;
        if(getClass()!=obj.getClass()) return false;
        final ColBean other=(ColBean)obj;
        if(colid==null)
        {
            if(other.colid!=null) return false;
        }else if(!colid.equals(other.colid)) return false;
        if(column==null)
        {
            if(other.column!=null) return false;
        }else if(!column.equals(other.column)) return false;
        if(property==null)
        {
            if(other.property!=null) return false;
        }else if(!property.equals(other.property)) return false;
        if(this.getReportBean()==null)
        {
            if(other.getReportBean()!=null) return false;
        }else if(!this.getReportBean().equals(other.getReportBean()))
        {
            return false;
        }
        return true;
    }
}
