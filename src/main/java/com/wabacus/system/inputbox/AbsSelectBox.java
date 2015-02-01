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
package com.wabacus.system.inputbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.dataset.select.common.AbsCommonDataSetValueProvider;
import com.wabacus.system.inputbox.option.SelectboxOptionBean;
import com.wabacus.util.Tools;

public abstract class AbsSelectBox extends AbsInputBox implements Cloneable
{
    private String depends;
    
    protected Map<String,Boolean> mParentIds;

    protected boolean isRegex=false;

    protected boolean isMultiply;
    
    protected String separator;//如果允许多选，则存放每个值之间的分隔符，默认为空格
    
    protected boolean isBelongtoUpdatecolSrcCol;
    
    protected List<SelectboxOptionBean> lstOptions=null;
    
    public AbsSelectBox(String typename)
    {
        super(typename);
    }

    public boolean isRegex()
    {
        return isRegex;
    }

    public void setRegex(boolean isRegex)
    {
        this.isRegex=isRegex;
    }

    public void setLstOptions(List<SelectboxOptionBean> lstOptions)
    {
        this.lstOptions=lstOptions;
    }

    public boolean isDependsOtherInputbox()
    {
        return !Tools.isEmpty(this.depends);
    }
    
    public Map<String,Boolean> getMParentIds()
    {
        return mParentIds;
    }

    protected abstract boolean isMultipleSelect();
    
    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanStart(rrequest));
        if(this.isDependsOtherInputbox())
        {
            resultBuf.append(" parentids=\""+getAllParentIdsAsString()+"\"");
        }
        if(this.isMultipleSelect()) resultBuf.append(" separator=\"").append(this.separator).append("\"");
        resultBuf.append(" selectboxtype=\""+this.getSelectboxType()+"\"");
        return resultBuf.toString();
    }
    
    private String getAllParentIdsAsString()
    {
        StringBuilder resultBuf=new StringBuilder();
        boolean isConditionBox=this.owner instanceof ConditionBean;
        ConditionBean conbeanTmp;
        for(String parentidTmp:this.mParentIds.keySet())
        {
            if(isConditionBox)
            {
                conbeanTmp=this.owner.getReportBean().getSbean().getConditionBeanByName(parentidTmp);
                if(conbeanTmp==null||conbeanTmp.isHidden()||conbeanTmp.isConstant()||!conbeanTmp.isConditionValueFromUrl()||conbeanTmp.getInputbox()==null) continue;
            }
            resultBuf.append(parentidTmp).append(";");
        }
        if(resultBuf.length()>0&&resultBuf.charAt(resultBuf.length()-1)==';') resultBuf.deleteCharAt(resultBuf.length()-1);
        return resultBuf.toString();
    }
    
    protected String initDisplaySpanContent(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(super.initDisplaySpanContent(rrequest));
        if(!this.isDependsOtherInputbox())
        {
            List<Map<String,String>> lstOptionsResult=getLstOptionsFromCache(rrequest);
            if(lstOptionsResult!=null&&lstOptionsResult.size()>0)
            {
                String name_temp, value_temp;
                for(Map<String,String> mItems:lstOptionsResult)
                {
                    name_temp=mItems.get("label");
                    value_temp=mItems.get("value");
                    value_temp=value_temp==null?"":value_temp.trim();
                    resultBuf.append("<span value=\""+value_temp+"\" label=\""+name_temp+"\"></span>");
                }
            }
        }
        return resultBuf.toString();
    }
    
    public String getDefaultlabel(ReportRequest rrequest)
    {
        if(this.defaultvalue==null) return null;
        List<Map<String,String>> lstOptionsResult=getLstOptionsFromCache(rrequest);
        if(lstOptionsResult==null||lstOptionsResult.size()==0) return null;
        StringBuilder labelBuf=new StringBuilder();
        String selectedvalue=this.getDefaultvalue(rrequest);
        for(Map<String,String> mItems:lstOptionsResult)
        {
            if(mItems.get("value")!=null&&isSelectedValueOfSelectBox(selectedvalue,mItems.get("value")))
            {//找到了默认值对应的显示label
                if(!this.isMultipleSelect()) return mItems.get("label");
                labelBuf.append(mItems.get("label")).append(separator);
            }
        }
        if(this.isMultipleSelect()&&labelBuf.toString().endsWith(separator)) labelBuf.delete(0,labelBuf.length()-separator.length());
        return labelBuf.toString();
    }
    
    protected List<Map<String,String>> getLstOptionsFromCache(ReportRequest rrequest)
    {
        List<Map<String,String>> lstOptionsResult=(List<Map<String,String>>)rrequest.getAttribute("LISTOPTIONS_"+owner.getInputBoxId());
        if(lstOptionsResult==null)
        {
            lstOptionsResult=getOptionsList(rrequest,null);
            if(lstOptionsResult!=null) rrequest.setAttribute("LISTOPTIONS_"+owner.getInputBoxId(),lstOptionsResult);
        }
        return lstOptionsResult;
    }
    
    public List<Map<String,String>> getOptionsList(ReportRequest rrequest,Map<String,String> mParentValues)
    {
        List<Map<String,String>> lstResults=new ArrayList<Map<String,String>>();
        List<Map<String,String>> lstOptionsTmp;
        for(SelectboxOptionBean obean:lstOptions)
        {
            lstOptionsTmp=obean.getLstRuntimeOptions(rrequest,mParentValues);
            if(lstOptionsTmp!=null) lstResults.addAll(lstOptionsTmp);
        }
        Map<String,String> mOptionTmp;
        if(lstResults.size()==0)
        {
            for(SelectboxOptionBean obean:lstOptions)
            {
                if(obean.getDatasetProvider()!=null||obean.getType()==null) continue;
                if(obean.getType().length==1&&obean.getType()[0].equals("%false-false%"))
                {
                    mOptionTmp=new HashMap<String,String>();
                    mOptionTmp.put("label",rrequest.getI18NStringValue(obean.getLabel()));
                    mOptionTmp.put("value",obean.getValue());
                    lstResults.add(mOptionTmp);
                }
            }
        }else
        {
            for(SelectboxOptionBean obean:lstOptions)
            {
                if(obean.getDatasetProvider()!=null||obean.getType()==null) continue;
                if(obean.getType().length==1&&obean.getType()[0].equals("%true-true%"))
                {//当前下拉选项是在选择框有选项数据时才显示出来
                    mOptionTmp=new HashMap<String,String>();
                    mOptionTmp.put("label",rrequest.getI18NStringValue(obean.getLabel()));
                    mOptionTmp.put("value",obean.getValue());
                    lstResults.add(0,mOptionTmp);
                }
            }
        }
        ReportBean rbean=this.owner.getReportBean();
        if(rbean.getInterceptor()!=null)
        {
            lstResults=(List<Map<String,String>>)rbean.getInterceptor().afterLoadData(rrequest,rbean,this,lstResults);
        }
        return lstResults;
    }

    protected String getInputBoxValue(ReportRequest rrequest,String value)
    {
        value=super.getInputBoxValue(rrequest,value);
        if(this.getBoxOwnerType()==IS_CONDITION_BOX)
        {
            ConditionBean cbean=(ConditionBean)this.owner;
            String reallabel=cbean.getLabel(rrequest);
            if(!Tools.isEmpty(reallabel)&&ConditionBean.LABELPOSITION_INNER.equals(cbean.getLabelposition())&&reallabel.equals(value))
            {
                return "";
            }
        }
        return value;
    }
    
    protected boolean isSelectedValueOfSelectBox(String selectedvalues,String optionvalue)
    {
        if(selectedvalues==null||optionvalue==null) return false;
        if(this.isMultipleSelect())
        {
            if(separator==null||separator.equals("")) separator=" ";
            selectedvalues=selectedvalues.trim();
            optionvalue=optionvalue.trim();
            while(selectedvalues.startsWith(separator))
            {
                selectedvalues=selectedvalues.substring(separator.length());
            }
            while(selectedvalues.endsWith(separator))
            {
                selectedvalues=selectedvalues.substring(0,selectedvalues.length()-separator.length());
            }
            if(selectedvalues.equals(optionvalue)) return true;
            String[] tmpArr=selectedvalues.split(separator);
            for(int i=0;i<tmpArr.length;i++)
            {
                if(optionvalue.equals(tmpArr[i].trim())) return true;
            }
            return false;
        }else
        {//当前是单选框
            return selectedvalues.trim().equals(optionvalue);
        }
    }
    
    public String fillBoxValueToParentElement()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("var selectboxvalue='',selectboxlabel='';");
        resultBuf.append(getBoxValueAndLabelScript());
        resultBuf.append("realvalue=selectboxvalue;displayvalue=selectboxlabel;");
        resultBuf.append("var updateDestElementObj=getUpdateColDestObj(parentElementObj,reportguid,null);");
        resultBuf.append("if(updateDestElementObj==null){updateDestElementObj=parentElementObj;}");
        resultBuf.append("else{parentElementObj.setAttribute('value',displayvalue);}");
        resultBuf.append("updateDestElementObj.setAttribute('value',realvalue);");////这里直接设置给td对象的value属性，不考虑用户配置的回调函数
        return resultBuf.toString();
    }
    
    protected abstract String getBoxValueAndLabelScript();

    protected abstract String getSelectboxType();
    
    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        super.loadInputBoxConfig(eleInputboxBean);
        if(eleInputboxBean==null)
        {
            throw new WabacusConfigLoadingException("加载报表"+owner.getReportBean().getPath()+"的选择框类型输入框失败，没有配置下拉选项");
        }
        List<SelectboxOptionBean> lstObs=new ArrayList<SelectboxOptionBean>();
        List<XmlElementBean> lstOptionElements=eleInputboxBean.getLstChildElementsByName("option");
        if(lstOptionElements!=null&&lstOptionElements.size()>0)
        {
            loadOptionInfo(lstObs,lstOptionElements);
        }
        if(lstObs==null||lstObs.size()==0)
        {
            throw new WabacusConfigLoadingException("加载报表"+owner.getReportBean().getPath()+"配置的选择框类型的输入框失败，没有配置下拉选项");
        }
        this.setLstOptions(lstObs);
        String depends=eleInputboxBean.attributeValue("depends");
        if(depends!=null) this.depends=depends.trim();
        String isregex=eleInputboxBean.attributeValue("isregrex");
        if(isregex!=null) this.isRegex=isregex.toLowerCase().trim().equals("true");
    }
    
    private void loadOptionInfo(List<SelectboxOptionBean> lstOptions,List<XmlElementBean> lstOptionBeans)
    {
        if(lstOptionBeans==null) return;
        ReportBean rbean=this.getOwner().getReportBean();
        String labeltemp,valuetemp;
        for(XmlElementBean eleOptionBeanTmp:lstOptionBeans)
        {
            if(eleOptionBeanTmp==null) continue;
            SelectboxOptionBean ob=new SelectboxOptionBean(this);
            String dataset=eleOptionBeanTmp.attributeValue("dataset");
            labeltemp=eleOptionBeanTmp.attributeValue("label");
            valuetemp=eleOptionBeanTmp.attributeValue("value");
            labeltemp=labeltemp==null?"":labeltemp.trim();
            valuetemp=valuetemp==null?"":valuetemp.trim();
            ob.setLabel(Config.getInstance().getResourceString(null,rbean.getPageBean(),labeltemp,true));
            ob.setValue(valuetemp);
            dataset=dataset==null?"":dataset.trim();
            if(dataset.equals(""))
            {
                String type=eleOptionBeanTmp.attributeValue("type");
                if(type!=null)
                {
                    type=type.trim();
                    String[] typearray=null;
                    if(type.equalsIgnoreCase("true"))
                    {//当前选项只有在当前下拉框有数据时才显示出来，如果没选项数据，则不显示出来
                        typearray=new String[1];
                        typearray[0]="%true-true%";
                    }else if(type.equalsIgnoreCase("false"))
                    {
                        typearray=new String[1];
                        typearray[0]="%false-false%";
                    }else if(!type.equals(""))
                    {
                        if(!type.startsWith("[")&&!type.endsWith("]"))
                        {
                            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的下拉框的type属性值没有用[]括住");
                        }
                        typearray=Tools.parseStringToArray(type,'[',']');
                        if(typearray==null||typearray.length==0)
                        {
                            throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的下拉框的下拉选项的type属性不合法");
                        }
                    }
                    ob.setType(typearray);
                }
            }else if(Tools.isDefineKey("$",dataset))
            {
                loadOptionInfo(lstOptions,(List<XmlElementBean>)Config.getInstance().getResourceObject(null,rbean.getPageBean(),dataset,true));
                continue;
            }else
            {
                AbsCommonDataSetValueProvider dsProvider=AbsCommonDataSetValueProvider.createCommonDataSetValueProviderObj(rbean,dataset);
                if(dsProvider==null)
                {
                    throw new WabacusConfigLoadingException("报表"+rbean.getPath()+"配置的选择框选项的dataset："+eleOptionBeanTmp.attributeValue("dataset")+"不合法");
                }
                ob.setDatasetProvider(dsProvider);
                dsProvider.setOwnerOptionBean(ob);
                dsProvider.loadConfig(eleOptionBeanTmp);
            }
            lstOptions.add(ob);
        }
    }

    public void processParentInputBox()
    {
        if(Tools.isEmpty(depends)) return;
        List<String> lstParentidsTmp=Tools.parseStringToList(depends,";",false);
        this.mParentIds=new HashMap<String,Boolean>();
        String dependtypeTmp=null;
        boolean isDependsOnChangeValue;
        AbsInputBox parentBoxTmp;//存放父输入框数据改变时立即刷新子输入框选项数据的父输入框
        for(String parentidTmp:lstParentidsTmp)
        {
            if(parentidTmp==null||parentidTmp.trim().equals("")) continue;
            parentBoxTmp=null;
            isDependsOnChangeValue=true;
            int idx=parentidTmp.indexOf("=");
            if(idx>0)
            {
                dependtypeTmp=parentidTmp.substring(idx+1).trim();
                isDependsOnChangeValue=!dependtypeTmp.trim().toLowerCase().equals("false");
                parentidTmp=parentidTmp.substring(0,idx).trim();
            }
            if(this.getBoxOwnerType()==IS_CONDITION_BOX)
            {
                if(isDependsOnChangeValue)
                {
                    ConditionBean cbTmp=this.owner.getReportBean().getSbean().getConditionBeanByName(parentidTmp);
                    if(cbTmp==null)
                    {
                        throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"的选择框"+this.owner.getInputBoxId()+"失败，其依赖的"
                                +parentidTmp+"对应的父输入框不存在");
                    }
                    if(cbTmp.isHidden()||cbTmp.isConstant()||cbTmp.getInputbox()==null)
                    {
                        isDependsOnChangeValue=false;
                    }else
                    {
                        parentBoxTmp=cbTmp.getInputbox();
                    }
                }
                this.mParentIds.put(parentidTmp,isDependsOnChangeValue);
            }else
            {
                ColBean cbTmp=this.owner.getReportBean().getDbean().getColBeanByColProperty(parentidTmp);
                if(cbTmp==null||cbTmp.isControlCol())
                {
                    throw new WabacusConfigLoadingException("加载报表"+this.owner.getReportBean().getPath()+"的选择框"+this.owner.getInputBoxId()+"失败，其依赖的"
                            +parentidTmp+"对应的父输入框不存在");
                }
                //如果依赖的父列有updatecol列，则把相应的两列全部加上
                ColBean cbSrc=cbTmp.getUpdateColBeanSrc(false);
                if(cbSrc==null) cbSrc=cbTmp;
                ColBean cbDest=cbTmp.getUpdateColBeanDest(false);
                if(cbDest==null) cbDest=cbTmp;
                EditableReportColBean ercbTmp=(EditableReportColBean)cbSrc.getExtendConfigDataForReportType(EditableReportColBean.class);
                if(isDependsOnChangeValue&&ercbTmp!=null&&ercbTmp.getInputbox()!=null)
                {
                    this.mParentIds.put(cbSrc.getProperty(),true);
                    this.mParentIds.put(cbDest.getProperty(),true);
                    parentBoxTmp=ercbTmp.getInputbox();
                }else
                {
                    this.mParentIds.put(cbSrc.getProperty(),false);
                    this.mParentIds.put(cbDest.getProperty(),false);
                }
            }
            if(parentBoxTmp!=null) parentBoxTmp.addChildInputboxId(this.getOwner().getInputBoxId());
            owner.getReportBean().addSelectBoxWithRelate(this);
        }
        if(this.mParentIds==null||this.mParentIds.size()==0) this.depends=null;
    }
    
    public void doPostLoad()
    {
        if(this.isMultiply&&(this.separator==null||this.separator.equals(""))) this.separator=" ";
        if(!this.isMultiply) this.separator=null;
        super.doPostLoad();
        if(owner instanceof EditableReportColBean)
        {
            ColBean cbean=(ColBean)((EditableReportColBean)owner).getOwner();
            this.isBelongtoUpdatecolSrcCol=cbean.getUpdateColBeanDest(false)!=null;
        }
        for(SelectboxOptionBean obTmp:this.lstOptions)
        {
            obTmp.doPostLoad();
        }
    }
    
    public Object clone(IInputBoxOwnerBean owner)
    {
        AbsSelectBox boxObjNew=(AbsSelectBox)super.clone(owner);
        if(lstOptions!=null)
        {
            List<SelectboxOptionBean> lstOptionsNew=new ArrayList<SelectboxOptionBean>();
            for(SelectboxOptionBean obTmp:lstOptions)
            {
                lstOptionsNew.add((SelectboxOptionBean)obTmp.clone(boxObjNew));
            }
            boxObjNew.setLstOptions(lstOptionsNew);
        }
        if(mParentIds!=null&&mParentIds.size()>0)
        {
            boxObjNew.mParentIds=(Map<String,Boolean>)((HashMap<String,Boolean>)mParentIds).clone();
            if(owner!=null&&owner.getReportBean()!=null)
            {
                owner.getReportBean().addSelectBoxWithRelate(boxObjNew);
            }
        }
        return boxObjNew;
    }
}
