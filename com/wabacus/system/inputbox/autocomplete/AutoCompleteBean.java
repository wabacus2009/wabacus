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
package com.wabacus.system.inputbox.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.DisplayBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.dataset.select.common.AbsCommonDataSetValueProvider;
import com.wabacus.system.dataset.select.common.SQLCommonDataSetValueProvider;
import com.wabacus.system.datatype.VarcharType;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.util.Tools;

public class AutoCompleteBean implements Cloneable
{
    public final static String MULTIPLE_FIRST="first";

    public final static String MULTIPLE_LAST="last";

    public final static String MULTIPLE_NONE="none";

    private AbsInputBox owner;

    private AbsCommonDataSetValueProvider datasetProvider;

    private List<String> lstAutoCompleteColumns;

    private List<ColBean> lstAutoCompleteColBeans;//需要自动填充的列对应的ColBean（在doPostLoad()中构造，所以不需出现在clone()中）
    
    private String colvalueCondition;

    private List<String> lstColPropertiesInColvalueConditions;
    
    private String multiple;//如果匹配了多条记录的处理办法，可取值为first/last/none，分别表示取第一条、最后一条、不取任何记录

    public AutoCompleteBean(AbsInputBox owner)
    {
        this.owner=owner;
    }

    public AbsInputBox getOwner()
    {
        return owner;
    }

    public void setOwner(AbsInputBox owner)
    {
        this.owner=owner;
    }

    public AbsCommonDataSetValueProvider getDatasetProvider()
    {
        return datasetProvider;
    }

    public List<String> getLstColPropertiesInColvalueConditions()
    {
        return lstColPropertiesInColvalueConditions;
    }

    public List<ColBean> getLstAutoCompleteColBeans()
    {
        return lstAutoCompleteColBeans;
    }

    public String getMultiple()
    {
        return multiple;
    }
    public String getColvalueConditionExpressionForSql(Map<String,String> mParams)
    {
        String realColConditionExpression=colvalueCondition;
        String colValTmp;
        for(String colpropertyTmp:lstColPropertiesInColvalueConditions)
        {
            colValTmp=mParams.get(colpropertyTmp);
            if(colValTmp==null) colValTmp="";
            realColConditionExpression=Tools.replaceAll(realColConditionExpression,"#"+colpropertyTmp+"#",colValTmp);
        }
        return realColConditionExpression;
    }

    public void loadConfig(XmlElementBean eleAutocompleteBean)
    {
        ReportBean rbean=this.owner.getOwner().getReportBean();
        String columns=eleAutocompleteBean.attributeValue("columns");
        if(columns==null||columns.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的输入框"+this.owner.getOwner().getInputBoxId()
                    +"的自动填充配置失败，没有指定要填充的列columns属性");
        }
        this.lstAutoCompleteColumns=Tools.parseStringToList(columns,";",false);
        String colvaluecondition=eleAutocompleteBean.attributeValue("colvaluecondition");
        if(colvaluecondition!=null) this.colvalueCondition=colvaluecondition.trim();
        String multiple=eleAutocompleteBean.attributeValue("multiple");
        if(multiple==null||multiple.trim().equals("")) multiple=MULTIPLE_FIRST;
        multiple=multiple.toLowerCase().trim();
        if(!multiple.equals(MULTIPLE_FIRST)&&!multiple.equals(MULTIPLE_LAST)&&!multiple.equals(MULTIPLE_NONE))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的输入框"+this.owner.getOwner().getInputBoxId()+"的自动填充配置失败，配置的multiple属性值"
                    +multiple+"无效，只能配置为first/last/none");
        }
        this.multiple=multiple;
        String dataset=eleAutocompleteBean.attributeValue("dataset");
        if(dataset==null||dataset.trim().equals(""))
        {
            throw new WabacusConfigLoadingException("加载报表"+rbean.getPath()+"上的输入框"+this.owner.getOwner().getInputBoxId()
                    +"的自动填充配置失败，没有配置dataset属性指定数据集");
        }
        dataset=dataset.trim();
        AbsCommonDataSetValueProvider dsProvider=AbsCommonDataSetValueProvider.createCommonDataSetValueProviderObj(rbean,dataset);
        if(dsProvider==null)
        {
            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的自动填充的dataset："+eleAutocompleteBean.attributeValue("dataset")+"不合法");
        }
        this.datasetProvider=dsProvider;
        dsProvider.setOwnerAutoCompleteBean(this);
        dsProvider.loadConfig(eleAutocompleteBean);
    }

    public void doPostLoad()
    {
        processAutoCompleteCols();
        if(this.datasetProvider instanceof SQLCommonDataSetValueProvider)
        {
            processColvalueConditionForSqlDataset();
        }else
        {
            processColvalueConditionForJAVASPDataset();
        }
        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
        ColBean cbUpdatecolDest=cbOwner.getUpdateColBeanDest(false);
        if(!this.lstColPropertiesInColvalueConditions.contains(cbOwner.getProperty()))
        {
            if(cbUpdatecolDest==null||!this.lstColPropertiesInColvalueConditions.contains(cbUpdatecolDest.getProperty()))
            {
                throw new WabacusConfigLoadingException("加载报表"+cbOwner.getReportBean().getPath()+"的列"+cbOwner.getColumn()
                        +"失败，没有在colvaluecondition属性中指定本列输入框的值做为动态条件");
            }
        }
        if(!this.lstColPropertiesInColvalueConditions.contains(cbOwner.getProperty())) this.lstColPropertiesInColvalueConditions.add(cbOwner.getProperty());
        if(cbUpdatecolDest!=null&&!this.lstColPropertiesInColvalueConditions.contains(cbUpdatecolDest.getProperty()))
        {
            this.lstColPropertiesInColvalueConditions.add(cbUpdatecolDest.getProperty());
        }
    }
    
    private void processAutoCompleteCols()
    {
        DisplayBean dbean=owner.getOwner().getReportBean().getDbean();
        this.lstAutoCompleteColBeans=new ArrayList<ColBean>();
        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
        if(!this.lstAutoCompleteColumns.contains(cbOwner.getColumn())) this.lstAutoCompleteColumns.add(cbOwner.getColumn());
        ColBean cbTmp, cbUpdatecolDest, cbUpdatecolSrc;
        List<String> lstRealAutoCompleteColumns=new ArrayList<String>();
        for(String columnTmp:this.lstAutoCompleteColumns)
        {
            if(lstRealAutoCompleteColumns.contains(columnTmp)) continue;
            lstRealAutoCompleteColumns.add(columnTmp);
            cbTmp=dbean.getColBeanByColColumn(columnTmp);
            if(cbTmp==null||cbTmp.isControlCol()||cbTmp.getProperty()==null||cbTmp.getProperty().trim().equals(""))
            {
                throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"的列"+cbOwner.getColumn()+"失败，为它配置的自动填充列"+columnTmp
                        +"不存在或不是有效填充列");
            }
            this.lstAutoCompleteColBeans.add(cbTmp);
            cbUpdatecolDest=cbTmp.getUpdateColBeanDest(false);
            if(cbUpdatecolDest!=null&&!lstRealAutoCompleteColumns.contains(cbUpdatecolDest.getColumn()))
            {
                this.lstAutoCompleteColBeans.add(cbUpdatecolDest);
                lstRealAutoCompleteColumns.add(cbUpdatecolDest.getColumn());
            }
            cbUpdatecolSrc=cbTmp.getUpdateColBeanSrc(false);
            if(cbUpdatecolSrc!=null&&!lstRealAutoCompleteColumns.contains(cbUpdatecolSrc.getColumn()))
            {
                this.lstAutoCompleteColBeans.add(cbUpdatecolSrc);
                lstRealAutoCompleteColumns.add(cbUpdatecolSrc.getColumn());
            }
        }
        this.lstAutoCompleteColumns=null;
    }
    
    private void processColvalueConditionForSqlDataset()
    {
        DisplayBean dbean=owner.getOwner().getReportBean().getDbean();
        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
        if(this.lstColPropertiesInColvalueConditions==null) this.lstColPropertiesInColvalueConditions=new ArrayList<String>();
        if(this.colvalueCondition==null||this.colvalueCondition.trim().equals(""))
        {
            boolean isVarcharType=cbOwner.getDatatypeObj() instanceof VarcharType;
            this.colvalueCondition=cbOwner.getColumn()+"=";
            if(isVarcharType) this.colvalueCondition+="'";
            this.colvalueCondition+="#"+cbOwner.getProperty()+"#";
            if(isVarcharType) this.colvalueCondition+="'";
            this.lstColPropertiesInColvalueConditions.add(cbOwner.getProperty());
        }else
        {//动态字段值都是用#column#的方式进行指定
            ColBean cbTmp;
            String expressTmp=this.colvalueCondition;
            String propertyTmp;
            while(true)
            {
                int idx=expressTmp.indexOf("#");
                if(idx<0) break;
                expressTmp=expressTmp.substring(idx+1);
                idx=expressTmp.indexOf("#");
                if(idx<=0) break;
                propertyTmp=expressTmp.substring(0,idx);
                cbTmp=dbean.getColBeanByColProperty(propertyTmp.trim());
                if(cbTmp!=null)
                {
                    if(cbTmp.isControlCol()||cbTmp.getProperty()==null||cbTmp.getProperty().trim().equals(""))
                    {
                        throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"的列"+cbOwner.getColumn()
                                +"失败，为它配置的自动填充列所用做为条件的列"+propertyTmp+"不存在或不是有效数据列");
                    }
                    if(!lstColPropertiesInColvalueConditions.contains(cbTmp.getProperty()))
                    {
                        lstColPropertiesInColvalueConditions.add(cbTmp.getProperty());
                    }
                    expressTmp=expressTmp.substring(idx+1);
                }
            }
        }
    }
    
    private void processColvalueConditionForJAVASPDataset()
    {
        DisplayBean dbean=owner.getOwner().getReportBean().getDbean();
        ColBean cbOwner=(ColBean)((EditableReportColBean)owner.getOwner()).getOwner();
        if(this.lstColPropertiesInColvalueConditions==null) this.lstColPropertiesInColvalueConditions=new ArrayList<String>();
        if(this.colvalueCondition==null||this.colvalueCondition.trim().equals(""))
        {
            this.lstColPropertiesInColvalueConditions.add(cbOwner.getProperty());
        }else
        {
            ColBean cbTmp;
            List<String> lstColProperties=Tools.parseStringToList(this.colvalueCondition,";",false);
            for(String propTmp:lstColProperties)
            {
                if(propTmp==null||propTmp.trim().equals("")) continue;
                cbTmp=dbean.getColBeanByColProperty(propTmp.trim());
                if(cbTmp==null||cbTmp.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+dbean.getReportBean().getPath()+"的列"+cbOwner.getColumn()+"失败，为它配置的自动填充列所用做为条件的列"
                            +propTmp+"不存在或不是有效数据列");
                }
                if(!lstColPropertiesInColvalueConditions.contains(cbTmp.getProperty()))
                {
                    lstColPropertiesInColvalueConditions.add(cbTmp.getProperty());
                }
            }
        }
    }
    
    public AutoCompleteBean clone(AbsInputBox newowner)
    {
        AutoCompleteBean newBean=null;
        try
        {
            newBean=(AutoCompleteBean)super.clone();
            newBean.setOwner(newowner);
            if(lstAutoCompleteColumns!=null) newBean.lstAutoCompleteColumns=(List<String>)((ArrayList<String>)lstAutoCompleteColumns).clone();
            if(datasetProvider!=null) newBean.datasetProvider=datasetProvider.clone(newBean);
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone输入框对象失败",e);
        }
        return newBean;
    }
}
