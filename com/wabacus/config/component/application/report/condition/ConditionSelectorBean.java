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
package com.wabacus.config.component.application.report.condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.RadioBox;
import com.wabacus.system.inputbox.SelectBox;

public class ConditionSelectorBean implements Cloneable
{
    private String selectortype;
    
    private String label;

    private String inputbox="selectbox";

    private String labelstyleproperty;

    private String valuestyleproperty;

    private int left=3;

    private int right;

    private ConditionBean cbean;

    private List<ConditionSelectItemBean> lstSelectItemBeans;//当前条件选择器的选项列表

    private Map<String,ConditionSelectItemBean> mSelectItemBeans;

    public ConditionSelectorBean(ConditionBean cbean,String selectortype)
    {
        this.cbean=cbean;
        this.selectortype=selectortype;
    }
    
    public String getSelectortype()
    {
        return selectortype;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label=label;
    }

    public String getInputbox()
    {
        return inputbox;
    }

    public void setInputbox(String inputbox)
    {
        if(inputbox.equals("")) inputbox="selectbox";
        if(!inputbox.equals("selectbox")&&!inputbox.equals("radiobox"))
        {
            throw new WabacusConfigLoadingException("加载报表"+this.cbean.getReportBean().getPath()+"的查询条件失败，条件表达式选择框只能为下拉框和单选框");
        }
        this.inputbox=inputbox;
    }

    public String getLabelstyleproperty()
    {
        return labelstyleproperty;
    }

    public void setLabelstyleproperty(String labelstyleproperty)
    {
        this.labelstyleproperty=labelstyleproperty;
    }

    public String getValuestyleproperty()
    {
        return valuestyleproperty;
    }

    public void setValuestyleproperty(String valuestyleproperty)
    {
        this.valuestyleproperty=valuestyleproperty;
    }

    public int getLeft()
    {
        return left;
    }

    public void setLeft(int left)
    {
        this.left=left;
    }

    public int getRight()
    {
        return right;
    }

    public void setRight(int right)
    {
        this.right=right;
    }

    public ConditionBean getCbean()
    {
        return cbean;
    }

    public void setCbean(ConditionBean cbean)
    {
        this.cbean=cbean;
    }

    public List<ConditionSelectItemBean> getLstSelectItemBeans()
    {
        return lstSelectItemBeans;
    }

    public void setLstSelectItemBeans(List<ConditionSelectItemBean> lstSelectItemBeans)
    {
        this.lstSelectItemBeans=lstSelectItemBeans;
    }

    public Map<String,ConditionSelectItemBean> getMSelectItemBeans()
    {
        return mSelectItemBeans;
    }

    public void setMSelectItemBeans(Map<String,ConditionSelectItemBean> selectItemBeans)
    {
        mSelectItemBeans=selectItemBeans;
    }

    public boolean isEmpty()
    {
        if(this.lstSelectItemBeans==null||this.lstSelectItemBeans.size()==0) return true;
        return false;
    }
    
    public String getSelectedInputboxId(int iteratorindex)
    {
        String inputboxid=cbean.getReportBean().getGuid()+"_"+cbean.getName();
        inputboxid=inputboxid+"_"+selectortype;
        if(iteratorindex>=0)
        {
            inputboxid=inputboxid+"_"+iteratorindex;
        }
        return inputboxid;
    }
    
    public ConditionSelectItemBean getSelectItemBeanById(String selectitemid)
    {
        if(this.lstSelectItemBeans==null||this.lstSelectItemBeans.size()==0) return null;
        if(selectitemid==null||selectitemid.trim().equals("")) return this.lstSelectItemBeans.get(0);
        if(this.mSelectItemBeans==null)
        {
            Map<String,ConditionSelectItemBean> mSelectItemBeansTmp=new HashMap<String,ConditionSelectItemBean>();
            for(ConditionSelectItemBean csibTmp:this.lstSelectItemBeans)
            {
                mSelectItemBeansTmp.put(csibTmp.getId(),csibTmp);
            }
            this.mSelectItemBeans=mSelectItemBeansTmp;
        }
        return this.mSelectItemBeans.get(selectitemid);
    }
    
    public String showSelectedInputbox(ReportRequest rrequest,boolean isReadonlyPermission,int iteratorindex)
    {
        if(this.lstSelectItemBeans==null||this.lstSelectItemBeans.size()<=1) return "";
        StringBuffer resultBuf=new StringBuffer();
        resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(this.left));
        String reallabel=this.label;
        if(reallabel!=null&&!reallabel.trim().equals(""))
        {
            reallabel=rrequest.getI18NStringValue(reallabel);
            resultBuf.append("<span class=\"cls-search-label\" ");
            if(this.labelstyleproperty!=null) resultBuf.append(this.labelstyleproperty);
            resultBuf.append(">"+reallabel+"</span>&nbsp;");
        }
        String inputboxid=this.getSelectedInputboxId(iteratorindex);//输入框id
        String selectedvalue=rrequest.getStringAttribute(inputboxid,this.lstSelectItemBeans.get(0).getId());
        if(inputbox==null||!inputbox.equals("radiobox"))
        {
            resultBuf.append(getSelectedBoxDisplayString(rrequest,Config.getInstance().getInputBoxByType(SelectBox.class),selectedvalue,"id=\""
                    +inputboxid+"\"",isReadonlyPermission));
        }else
        {
            resultBuf.append(getSelectedBoxDisplayString(rrequest,Config.getInstance().getInputBoxByType(RadioBox.class),selectedvalue,"name=\""
                    +inputboxid+"\"",isReadonlyPermission));
        }
        resultBuf.append(WabacusAssistant.getInstance().getSpacingDisplayString(this.right));
        return resultBuf.toString();
    }
    
    private String getSelectedBoxDisplayString(ReportRequest rrequest,AbsInputBox selectboxObj,String selectedvalue,String dynstyleproperty,
            boolean isReadonly)
    {
        if(this.valuestyleproperty!=null) dynstyleproperty=dynstyleproperty+" "+this.valuestyleproperty;
        List<String[]> lstOptions=new ArrayList<String[]>();
        if(this.lstSelectItemBeans!=null)
        {
            for(ConditionSelectItemBean csibeanTmp:this.lstSelectItemBeans)
            {
                lstOptions.add(new String[] { rrequest.getI18NStringValue(csibeanTmp.getLabel()), csibeanTmp.getId() });
            }
        }
        return selectboxObj.getIndependentDisplayString(rrequest,selectedvalue,dynstyleproperty,lstOptions,isReadonly);
    }
    
    public void loadConfig(XmlElementBean eleConditionSelectorBean,String selectItemTagname)
    {
        String label=eleConditionSelectorBean.attributeValue("label");
        String left=eleConditionSelectorBean.attributeValue("left");
        String right=eleConditionSelectorBean.attributeValue("right");
        String labelstyleproperty=eleConditionSelectorBean.attributeValue("labelstyleproperty");
        String valuestyleproperty=eleConditionSelectorBean.attributeValue("valuestyleproperty");
        String inputbox=eleConditionSelectorBean.attributeValue("inputbox");
        if(label!=null)
        {
            this.setLabel(Config.getInstance().getResourceString(null,cbean.getPageBean(),label,true));
        }
        if(left!=null)
        {
            left=left.trim();
            if(left.equals("")) left="0";
            this.setLeft(Integer.parseInt(left));
            if(this.getLeft()<0) this.setLeft(0-this.getLeft());
        }
        if(right!=null)
        {
            right=right.trim();
            if(right.equals("")) right="0";
            this.setRight(Integer.parseInt(right));
            if(this.getRight()<0) this.setRight(0-this.getRight());
        }
        if(labelstyleproperty!=null)
        {
            this.setLabelstyleproperty(labelstyleproperty.trim());
        }
        if(valuestyleproperty!=null)
        {
            this.setValuestyleproperty(valuestyleproperty.trim());
        }
        if(inputbox!=null) this.setInputbox(inputbox.trim());
        
        List<XmlElementBean> lstEleSelectorItemElements=eleConditionSelectorBean.getLstChildElementsByName(selectItemTagname);//获取所有选择器选项的配置信息
        if(lstEleSelectorItemElements==null||lstEleSelectorItemElements.size()==0) return;
        List<ConditionSelectItemBean> lstSelectItemBeans=new ArrayList<ConditionSelectItemBean>();
        List<String> lstSelectItemIds=new ArrayList<String>();
        ConditionSelectItemBean csibeanTmp;
        for(XmlElementBean eleSelectItemBeanTmp:lstEleSelectorItemElements)
        {
            if(selectItemTagname.equals("value"))
            {
                csibeanTmp=new ConditionValueSelectItemBean(cbean);
            }else
            {
                csibeanTmp=new ConditionSelectItemBean(cbean);                
            }
            lstSelectItemBeans.add(csibeanTmp);
            csibeanTmp.loadConfig(eleSelectItemBeanTmp);
            if(lstSelectItemIds.contains(csibeanTmp.getId()))
            {
                throw new WabacusConfigLoadingException("加载报表"+cbean.getReportBean().getPath()+"的<condition/>的<column/>失败，配置的id属性"+csibeanTmp.getId()
                        +"重复");
            }
            lstSelectItemIds.add(csibeanTmp.getId());
        }
        this.setLstSelectItemBeans(lstSelectItemBeans);
    }
    
    public ConditionSelectorBean clone(ConditionBean cbeanNew)
    {
        try
        {
            ConditionSelectorBean csbeanNew=(ConditionSelectorBean)super.clone();
            csbeanNew.setCbean(cbeanNew);
            if(lstSelectItemBeans!=null)
            {
                List<ConditionSelectItemBean> lstSelectItemBeansNew=new ArrayList<ConditionSelectItemBean>();
                for(ConditionSelectItemBean ccbeanTmp:lstSelectItemBeans)
                {
                    lstSelectItemBeansNew.add((ConditionSelectItemBean)ccbeanTmp.clone(cbeanNew));
                }
                csbeanNew.setLstSelectItemBeans(lstSelectItemBeansNew);
            }
            return csbeanNew;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone 报表"+cbean.getReportBean().getPath()+"的ConditionSelectorBean对象失败",e);
        }
    }
}
