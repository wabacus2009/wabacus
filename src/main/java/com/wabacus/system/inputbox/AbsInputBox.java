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
import java.util.List;
import java.util.Map;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ConditionBean;
import com.wabacus.config.xml.XmlElementBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.JavaScriptAssistant;
import com.wabacus.system.assistant.ReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.EditableDetailReportType2;
import com.wabacus.system.component.application.report.EditableListFormReportType;
import com.wabacus.system.component.application.report.EditableListReportType2;
import com.wabacus.system.component.application.report.abstractreport.AbsReportType;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.inputbox.autocomplete.AutoCompleteBean;
import com.wabacus.system.inputbox.validate.ServerValidateBean;
import com.wabacus.util.Tools;

public abstract class AbsInputBox implements Cloneable
{
    public final static String VALIDATE_TYPE_ONBLUR="onblur";

    public final static String VALIDATE_TYPE_ONSUBMIT="onsubmit";

    public final static String VALIDATE_TYPE_BOTH="both";

    protected String defaultvalue;//输入框的默认显示值，如果当前输入框没有数据进行显示时，将显示这里配置的默认值，只有此输入框属于编辑列，即配置在<col/>下时，此默认值才有效，当为查询条件下的输入框时，此属性无效。

    protected String defaultstyleproperty;//在wabacus.cfg.xml中配置的默认样式字符串，在editablelist2/editabledetail2两种报表类型的编辑列输入框中不会用到这里的样式，在其它任意场合的输入框中都会用到这里的样式，且不会被覆盖

    protected String inputboxparams;

    protected String styleproperty;//样式字符串

    private List<String> lstDynStylepropertyParts;

    private String beforedescription;

    private Map<String,String> mDynBeforedescriptionParts;

    private String afterdescription;

    private Map<String,String> mDynAfterdescriptionParts;

    private String tip;//配置当鼠标滑过输入框时的提示信息

    protected String language;

    private String jsvalidate;

    private String jsvalidatetype;

    private String servervalidate;

    private String servervalidatetype;

    private String servervalidateCallback;//服务器端校验时的回调函数

    private String errorpromptparamsonblur;//本输入框在onblur进行客户/服务器端校验失败时提示错误信息的窗口参数
    
    protected String typename;

    protected IInputBoxOwnerBean owner;

    private AutoCompleteBean autoCompleteBean;

    private List<String> lstChildids;

    protected boolean displayOnClick;

    protected boolean displayNonStyle;

    private String displayon;//如果当前输入框是配置给可编辑报表，且当此列没有出现在<insert/>和<update/>中时，如果想显示输入框，则通过此属性指定在什么条件下需要显示输入框，可配置值为insert、update、insert|update，此属性对查询条件中的输入框无效

    protected final static int IS_CONDITION_BOX=1;
    
    protected final static int IS_COL_BOX=2;//当前输入框是编辑列上的输入框
    
    public AbsInputBox(String typename)
    {
        this.typename=typename;
    }

    public IInputBoxOwnerBean getOwner()
    {
        return owner;
    }

    public void setOwner(IInputBoxOwnerBean owner)
    {
        this.owner=owner;
    }

    public List<String> getLstChildids()
    {
        return lstChildids;
    }

    public void addChildInputboxId(String inputboxid)
    {
        if(inputboxid==null||inputboxid.trim().equals("")) return;
        if(this.lstChildids==null) this.lstChildids=new ArrayList<String>();
        if(!this.lstChildids.contains(inputboxid)) this.lstChildids.add(inputboxid);
    }

    protected String getInputBoxValue(ReportRequest rrequest,String value)
    {
        if(value==null||value.trim().equals("")&&defaultvalue!=null)
        {
            value=ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
        }
        if(value==null) value="";
        return Tools.htmlEncode(value);
    }

    public String getJsvalidatetype()
    {
        return jsvalidatetype;
    }

    protected boolean isJsvalidateOnblur()
    {
        if(this.jsvalidate==null||this.jsvalidate.trim().equals("")) return false;
        return this.VALIDATE_TYPE_BOTH.equals(this.jsvalidatetype)||this.VALIDATE_TYPE_ONBLUR.equals(this.jsvalidatetype);
    }

    public String getLanguage()
    {
        return language;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public String getDefaultvalue(ReportRequest rrequest)
    {
        if(defaultvalue==null) return null;
        return ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
    }

    public String getDefaultlabel(ReportRequest rrequest)
    {
        return defaultvalue==null?"":ReportAssistant.getInstance().getColAndConditionDefaultValue(rrequest,defaultvalue);
    }
    
    public AutoCompleteBean getAutoCompleteBean()
    {
        return autoCompleteBean;
    }

    public void setAutoCompleteBean(AutoCompleteBean autoCompleteBean)
    {
        this.autoCompleteBean=autoCompleteBean;
    }

    public String getTypename()
    {
        return typename;
    }

    public boolean isDisplayOnClick()
    {
        return displayOnClick;
    }

    public int getBoxOwnerType()
    {
        if(this.owner==null) return -1;
        if(this.owner instanceof ConditionBean) return IS_CONDITION_BOX;
        if(this.owner instanceof EditableReportColBean) return IS_COL_BOX;
        return -1;
    }
    
    protected String getStyleproperty(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStylepropertyWithDynPart(rrequest,this.styleproperty,this.lstDynStylepropertyParts,"");
    }
    
    private void setStyleproperty(String styleproperty)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStylepropertyWithDynPart(styleproperty);
        this.styleproperty=(String)objArr[0];
        this.lstDynStylepropertyParts=(List<String>)objArr[1];
    }

    public String getJsvalidate()
    {
        return jsvalidate;
    }

    private void setBeforedescription(String beforedescription)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.owner.getReportBean().getPageBean(),beforedescription);
        this.beforedescription=(String)objArr[0];
        this.mDynBeforedescriptionParts=(Map<String,String>)objArr[1];
    }

    private void setAfterdescription(String afterdescription)
    {
        Object[] objArr=WabacusAssistant.getInstance().parseStringWithDynPart(this.owner.getReportBean().getPageBean(),afterdescription);
        this.afterdescription=(String)objArr[0];
        this.mDynAfterdescriptionParts=(Map<String,String>)objArr[1];
    }

    public String getBeforedescription(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.beforedescription,this.mDynBeforedescriptionParts,"");
    }

    public String getAfterdescription(ReportRequest rrequest)
    {
        return WabacusAssistant.getInstance().getStringValueWithDynPart(rrequest,this.afterdescription,this.mDynAfterdescriptionParts,"");
    }

    protected boolean hasDescription()
    {
        if(this.beforedescription!=null&&!this.beforedescription.trim().equals("")) return true;
        if(this.afterdescription!=null&&!this.afterdescription.trim().equals("")) return true;
        return false;
    }

    public String getTip(ReportRequest rrequest)
    {
        if(this.tip!=null&&!this.tip.trim().equals(""))
        {
            return rrequest.getI18NStringValue(this.tip);
        }
        return "";
    }

    public String getErrorpromptparamsonblur()
    {
        if(Tools.isEmpty(errorpromptparamsonblur)) return Config.default_errorpromptparams_onblur;
        return errorpromptparamsonblur;
    }

    public String getDisplayon()
    {
        return displayon;
    }
    
    public String getDisplayStringValue(ReportRequest rrequest,String value,String dynstyleproperty,boolean isReadonly)
    {
        String resultStr=doGetDisplayStringValue(rrequest,value,Tools.mergeHtmlTagPropertyString(this.getStyleproperty(rrequest),dynstyleproperty,1),
                isReadonly);
        if(!Tools.isEmpty(resultStr)&&!Tools.isEmpty(this.lstChildids)&&!isDependsOtherInputbox())
        {
            String myrealid=getInputBoxId(rrequest);
            int idx=myrealid.lastIndexOf("__");
            int rowidx=idx>0?Integer.parseInt(myrealid.substring(idx+2)):-1;
            List<String> lstRealChildids=null;
            if(rowidx>=0)
            {
                lstRealChildids=new ArrayList<String>();
                for(String childidTmp:this.lstChildids)
                {
                    if(Tools.isEmpty(childidTmp)) continue;
                    lstRealChildids.add(childidTmp.trim()+"__"+rowidx);
                }
            }else
            {
                lstRealChildids=(List<String>)((ArrayList<String>)this.lstChildids).clone();
            }
            rrequest.getWResponse().addChildInputboxIdsToOnload(lstRealChildids);
        }
        return resultStr;
    }

    public void setDefaultstyleproperty(String defaultstyleproperty)
    {
        this.defaultstyleproperty=defaultstyleproperty;
    }

    public boolean isDependsOtherInputbox()
    {
        return false;
    }
    
    protected boolean isBelongToEditable2ReportCol(boolean isIncludeListForm)
    {
        if(owner instanceof EditableReportColBean)
        {
            AbsReportType reportTypeObj=Config.getInstance().getReportType(owner.getReportBean().getType());
            if(reportTypeObj instanceof EditableDetailReportType2) return true;
            if(reportTypeObj instanceof EditableListReportType2)
            {
                if(isIncludeListForm) return true;
                return !(reportTypeObj instanceof EditableListFormReportType);
            }
        }
        return false;
    }
    
    public String initDisplay(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append(initDisplaySpanStart(rrequest)).append(">");
        resultBuf.append(initDisplaySpanContent(rrequest));
        resultBuf.append("</span>");
        return resultBuf.toString();
    }

    protected String initDisplaySpanStart(ReportRequest rrequest)
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("<span id=\"span_"+this.owner.getInputBoxId()+"_span\" style=\"display:none;\"");
        resultBuf.append(" typename=\""+this.typename+"\"");
        resultBuf.append(" isconditionbox=\""+(this.owner instanceof ConditionBean)+"\"");
        if(this.owner instanceof EditableReportColBean)
        {
            String formatemplate=((EditableReportColBean)this.owner).getFormatemplate(rrequest);
            if(formatemplate!=null&&!formatemplate.trim().equals(""))
            {
                resultBuf.append(" formatemplate=\"").append(Tools.onlyHtmlEncode(formatemplate)).append("\"");
                resultBuf.append(" formatemplate_dyncols=\"").append(
                        ((EditableReportColBean)this.owner).getColPropertyAndPlaceHoldersInFormatemplate()).append("\"");
            }
        }
        resultBuf.append(" displayonclick=\""+this.displayOnClick+"\"").append(" displaynonstyle=\""+this.displayNonStyle+"\"");
        if(this.inputboxparams!=null&&!this.inputboxparams.trim().equals(""))
        {//显示输入框时要传入的参数
            resultBuf.append(" inputboxparams=\""+this.inputboxparams.trim()+"\"");
        }
        if(this.lstChildids!=null&&this.lstChildids.size()>0)
        {
            resultBuf.append(" childboxids=\"");
            for(String childidTmp:this.lstChildids)
            {
                resultBuf.append(childidTmp).append(";");
            }
            if(resultBuf.charAt(resultBuf.length()-1)==';') resultBuf.deleteCharAt(resultBuf.length()-1);
            resultBuf.append("\"");
        }
        if(!Tools.isEmpty(this.getErrorpromptparamsonblur()))
        {
            resultBuf.append(" errorpromptparamsonblur=\""+Tools.jsParamEncode(this.getErrorpromptparamsonblur())+"\"");
        }
        if(isBelongToEditable2ReportCol(true))
        {
            String realstyle=this.getStyleproperty(rrequest);
            if(realstyle!=null&&!realstyle.trim().equals(""))
            {
                resultBuf.append(" styleproperty=\""+Tools.jsParamEncode(realstyle)+"\"");
            }
        }
        return resultBuf.toString();
    }

    protected String initDisplaySpanContent(ReportRequest rrequest)
    {
        return "";
    }

    protected String getInputBoxCommonFilledProperties()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("boxstr=boxstr+\" id= '\"+realinputboxid+\"'").append(" name='\"+realinputboxid+\"'\";");
        resultBuf.append("if(styleproperty==null) styleproperty='';");
        resultBuf.append(addPropValueToFillStyleproperty());
        resultBuf.append("boxstr=boxstr+\" \"+styleproperty;");
        return resultBuf.toString();
    }

    protected String addPropValueToFillStyleproperty()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("if(displayonclick==='true'||boxMetadataObj.getAttribute('displaynonstyle')=='true'){");//如果是显示在editablelist2/editabledetail2上的输入框，则加上如下样式
        resultBuf.append("  arrTmp=getPropertyValueFromHtmlProperties(styleproperty,'style');");
        resultBuf.append("  styleproperty=arrTmp[0];");
        resultBuf.append("  var styleValue=arrTmp[1];if(styleValue==null) styleValue='';");
        resultBuf.append("  if(styleValue.toLowerCase().indexOf('text-align:')<0) styleValue=styleValue+\"text-align:\"+textalign+\";\";");
        resultBuf.append("  if(wid!=null&&parseInt(wid)>0&&styleValue.toLowerCase().indexOf('width:')<0){");
        resultBuf.append("      styleValue=styleValue+\"width:\"+wid+\"px;\";}");
        resultBuf.append("  styleproperty=\" style=\\\"\"+styleValue+\"\\\" \"+arrTmp[0];");
        resultBuf.append("}");
        return resultBuf.toString();
    }

    public String createGetInputboxValueJs(boolean isGetLabel)
    {
        return "";
    }
    
    public String createSetInputboxValueJs(boolean isSetLabel)
    {
        return "";
    }
    
    public String getChangeStyleObjOnEdit()
    {
        return "";//默认就是改变输入框本身的样式
    }   
    
    protected String getInputBoxId(ReportRequest rrequest)
    {
        String inputboxid=rrequest.getStringAttribute("DYN_INPUTBOX_ID");
        if(inputboxid==null||inputboxid.trim().equals("")) inputboxid=owner.getInputBoxId();
        return inputboxid;
    }

    protected String addReadonlyToStyleProperty1(String style_property)
    {
        if(style_property==null)
        {
            style_property="";
        }else if(style_property.toLowerCase().indexOf(" readonly ")>=0)
        {
            return style_property;
        }
        return style_property+" readonly ";
    }

    protected String addReadonlyToStyleProperty2(String style_property)
    {
        if(style_property==null)
        {
            style_property="";
        }else if(style_property.toLowerCase().indexOf(" disabled ")>=0)
        {
            return style_property;
        }
        return style_property+" disabled ";
    }

    public void loadInputBoxConfig(XmlElementBean eleInputboxBean)
    {
        if(eleInputboxBean==null) return;
        XmlElementBean eleAutocompleteBean=eleInputboxBean.getChildElementByName("autocomplete");
        if(eleAutocompleteBean!=null)
        {
            if(owner instanceof EditableReportColBean)
            {
                this.autoCompleteBean=new AutoCompleteBean(this);
                this.autoCompleteBean.loadConfig(eleAutocompleteBean);
            }else
            {//是查询条件上的自动填充输入框，则不用加载<autocomplete/>里面的配置，只要生成一个此对象标识一下有这个功能
            }
        }
        String beforedescription=eleInputboxBean.attributeValue("beforedescription");
        if(beforedescription!=null)
        {
            this.setBeforedescription(beforedescription);
        }
        String afterdescription=eleInputboxBean.attributeValue("afterdescription");
        if(afterdescription!=null)
        {
            this.setAfterdescription(afterdescription);
        }
        String tip=eleInputboxBean.attributeValue("tip");
        if(tip!=null)
        {
            this.tip=Config.getInstance().getResourceString(null,owner.getReportBean().getPageBean(),tip,true);
        }
        String styleproperty=eleInputboxBean.attributeValue("styleproperty");
        if(styleproperty!=null)
        {
            this.setStyleproperty(Tools.formatStringBlank(styleproperty.trim()));
        }
        loadValidateConfig(eleInputboxBean);
        String inputboxparams=eleInputboxBean.attributeValue("inputboxparams");
        if(inputboxparams!=null) this.inputboxparams=inputboxparams.trim();
        String _language=eleInputboxBean.attributeValue("language");
        if(_language==null||_language.trim().equals(""))
        {
            this.language=null;
        }else
        {
            this.language=_language;
        }
        String displayon=eleInputboxBean.attributeValue("displayon");
        if(displayon!=null)
        {
            displayon=displayon.toLowerCase().trim();
            if(!displayon.equals(""))
            {
                this.displayon="";
                List<String> lstTmp=Tools.parseStringToList(displayon,"|",false);
                for(String tmp:lstTmp)
                {
                    if(!tmp.trim().equals("insert")&&!tmp.trim().equals("update")) continue;
                    this.displayon=this.displayon+tmp+"|";
                }
            }else
            {
                this.displayon=null;
            }
        }
    }

    protected void loadValidateConfig(XmlElementBean eleInputboxBean)
    {
        String jsvalidate=eleInputboxBean.attributeValue("jsvalidate");
        if(jsvalidate!=null&&!jsvalidate.trim().equals(""))
        {
            this.jsvalidate=jsvalidate.trim();
            this.jsvalidatetype=loadValidateType(eleInputboxBean,"jsvalidatetype","default-jsvalidatetype");
        }
        String servervalidate=eleInputboxBean.attributeValue("servervalidate");
        if(servervalidate!=null&&!servervalidate.trim().equals(""))
        {
            this.servervalidate=servervalidate.trim();
            this.servervalidatetype=loadValidateType(eleInputboxBean,"servervalidatetype","default-servervalidatetype");
        }
        String serverValidateCallback=eleInputboxBean.attributeValue("servervalidatecallback");
        if(serverValidateCallback!=null&&!serverValidateCallback.trim().equals(""))
        {
            this.servervalidateCallback=serverValidateCallback.trim();
        }
        if(this.servervalidateCallback==null||this.servervalidateCallback.trim().equals("")) this.servervalidateCallback="null";
        errorpromptparamsonblur=eleInputboxBean.attributeValue("errorpromptparamsonblur");
    }

    private String loadValidateType(XmlElementBean eleInputboxBean,String typename,String defaulttypename)
    {
        String type=eleInputboxBean.attributeValue(typename);
        if(!Tools.isEmpty(type))
        {
            type=type.toLowerCase().trim();
            if(!type.equals(VALIDATE_TYPE_BOTH)&&!type.equals(VALIDATE_TYPE_ONBLUR)&&!type.equals(VALIDATE_TYPE_ONSUBMIT))
            {
                throw new WabacusConfigLoadingException("加载报表"+owner.getReportBean().getPath()+"上的输入框"+owner.getInputBoxId()+"失败，配置的"+typename+"无效");
            }
        }else
        {
            type=Config.getInstance().getSystemConfigValue(defaulttypename,VALIDATE_TYPE_BOTH).toLowerCase().trim();
            if(!type.equals(VALIDATE_TYPE_BOTH)&&!type.equals(VALIDATE_TYPE_ONBLUR)&&!type.equals(VALIDATE_TYPE_ONSUBMIT))
            {
                throw new WabacusConfigLoadingException("在wabacus.cfg.xml系统级配置文件配置的"+defaulttypename+"配置项无效");
            }
        }
        return type;
    }
    
    public void doPostLoad()
    {
        initDisplayModeAndStyle();
        if(displayNonStyle) this.defaultstyleproperty=this.getDefaultStylePropertyForDisplayMode2();
        this.styleproperty=this.styleproperty==null?"":this.styleproperty.trim();
        if(this.styleproperty.toLowerCase().startsWith("(overwrite)"))
        {
            this.styleproperty=this.styleproperty.substring("(overwrite)".length());
        }else if(this.styleproperty.toLowerCase().startsWith("[overwrite]"))
        {//覆盖同名的样式字符串
            this.styleproperty=this.styleproperty.substring("[overwrite]".length());
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,this.styleproperty,0);
        }else
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.defaultstyleproperty,this.styleproperty,1);
        }
        processRelativeInputBoxes();
        if(this.autoCompleteBean!=null) this.autoCompleteBean.doPostLoad();
        if(this.autoCompleteBean!=null)
        {
            owner.getReportBean().addInputboxWithAutoComplete(this);
            styleproperty=Tools.mergeHtmlTagPropertyString(styleproperty,
                    "onfocus=\"this.autoComplete_oldData=wx_getColValue(getInputboxParentElementObj(this));\"",1);
            StringBuilder blurEventBuf=new StringBuilder();
            blurEventBuf.append("loadAutoCompleteInputboxData(this,'");
            if(this.autoCompleteBean.getLstColPropertiesInColvalueConditions()!=null)
            {
                for(String colPropTmp:this.autoCompleteBean.getLstColPropertiesInColvalueConditions())
                {
                    if(colPropTmp==null||colPropTmp.trim().equals("")) continue;
                    blurEventBuf.append(colPropTmp).append(";");
                }
            }
            blurEventBuf.append("');");
            styleproperty=Tools.mergeHtmlTagPropertyString(styleproperty,"onblur=\"try{"+blurEventBuf.toString()
                    +"}catch(e){logErrorsAsJsFileLoad(e);}\"",1);
        }
        processJsValidate();
        processServerValidate();
        processStylePropertyAfterMerged();
        if(this.owner instanceof EditableReportColBean)
        {
            if(this.displayon!=null)
            {
                if(this.displayon.indexOf("insert")>=0&&((EditableReportColBean)this.owner).getEditableWhenInsert()<=0)
                {
                    ((EditableReportColBean)this.owner).setEditableWhenInsert(1);
                }
                if(this.displayon.indexOf("update")>=0&&((EditableReportColBean)this.owner).getEditableWhenUpdate()<=0)
                {
                    ((EditableReportColBean)this.owner).setEditableWhenUpdate(1);
                }
            }
            if(this.isDisplayOnClick())
            {
                this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,fillParentValueMethodName()+"=\"fillBoxValueToParentElement(this,'"
                        +this.getOwner().getInputBoxId()+"',"+isChangeDisplayValueWhenFillValue()+")\"",1);//默认是在失去焦点时，填充值到<font/>或<td/>父标签上
            }
        }
        if(this.defaultvalue!=null&&Tools.isDefineKey("url",this.defaultvalue))
        {
            this.owner.getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",this.defaultvalue));
        }
    }

    protected boolean isChangeDisplayValueWhenFillValue()
    {
        return true;
    }

    protected String fillParentValueMethodName()
    {
        return "onblur";
    }
    
    public void initDisplayModeAndStyle()
    {
        initDisplayStyle();
        initDisplayMode();
    }
    
    private void initDisplayStyle()
    {
        if(!(this.owner instanceof EditableReportColBean))
        {//如果不是编辑列的输入框（比如是查询条件输入框）
            this.displayNonStyle=false;
        }else
        {
            AbsReportType reportTypeObj=Config.getInstance().getReportType(this.owner.getReportBean().getType());
            if((reportTypeObj instanceof EditableListReportType2&&!(reportTypeObj instanceof EditableListFormReportType))
                    ||reportTypeObj instanceof EditableDetailReportType2)
            {
                this.displayNonStyle=true;
            }else
            {
                this.displayNonStyle=false;
            }
        }
    }
    
    protected void initDisplayMode()
    {
        if(!(this.owner instanceof EditableReportColBean))
        {
            this.displayOnClick=false;
        }else
        {
            AbsReportType reportTypeObj=Config.getInstance().getReportType(this.owner.getReportBean().getType());
            if((reportTypeObj instanceof EditableListReportType2&&!(reportTypeObj instanceof EditableListFormReportType))
                    ||reportTypeObj instanceof EditableDetailReportType2)
            {
                this.displayOnClick=true;
            }else
            {
                this.displayOnClick=false;
            }
        }
    }
    
    protected void processJsValidate()
    {
        if(this.jsvalidate!=null&&!this.jsvalidate.trim().equals(""))
        {
            JavaScriptAssistant.getInstance().createInputBoxValidateMethod(this);
            addJsValidateOnBlurEvent();
        }
    }
    
    protected void addJsValidateOnBlurEvent()
    {
        if(this.isJsvalidateOnblur())
        {
            String onblur="onblur=\"wx_onblurValidate('"+this.owner.getReportBean().getGuid()+"',this,";
            onblur+=(this.owner instanceof ConditionBean)+",false,null)\"";
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,onblur,1);
        }
    }
    
    protected void processServerValidate()
    {
        if(this.servervalidate==null||this.servervalidate.trim().equals("")) return;
        List<String> lstValidateMethods=Tools.parseStringToList(servervalidate.trim(),";",new String[]{"'","'"},false);
        if(lstValidateMethods==null||lstValidateMethods.size()==0) return;
        ServerValidateBean svbean=new ServerValidateBean(this);
        svbean.setValidatetype(this.servervalidatetype);
        svbean.setServervalidateCallback(this.servervalidateCallback);
        String methodnameTmp, configParamsTmp;
        for(String methodTmp:lstValidateMethods)
        {
            methodTmp=methodTmp.trim();
            methodnameTmp=methodTmp;
            configParamsTmp=null;
            int lidx=methodTmp.indexOf("(");
            int ridx=methodTmp.lastIndexOf(")");
            if(lidx>0&&lidx<ridx)
            {
                methodnameTmp=methodTmp.substring(0,lidx);
                if(methodnameTmp.equals("")) continue;
                configParamsTmp=methodTmp.substring(lidx+1,ridx).trim();
            }
            svbean.addServerValidateMethod(methodnameTmp,configParamsTmp);
        }
        if(!Tools.isEmpty(svbean.getLstValidateMethods()))
        {
            this.owner.setServerValidateBean(svbean);
            if(!VALIDATE_TYPE_ONSUBMIT.equals(this.servervalidatetype))
            {//当前校验不是只在提交时进行校验，即在onblur时也要进行校验
                this.owner.getReportBean().addServerValidateBeanOnBlur(this.owner.getInputBoxId(),svbean);
                String onblur="onblur=\"wx_onblurValidate('"+this.owner.getReportBean().getGuid()+"',this,";
                onblur+=(this.owner instanceof ConditionBean)+",true,";
                onblur+=this.servervalidateCallback+")\"";
                this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,onblur,1);
            }
        }
    }

    protected void processRelativeInputBoxes()
    {
        if(this.lstChildids==null||this.lstChildids.size()==0) return;
        String event;
        if(!this.isDisplayOnClick())
        {
            event=getRefreshChildboxDataEventName()+"=\"wx_reloadChildSelectBoxOptionsByParentInputbox(this)\"";
        }else
        {
            event=getRefreshChildboxDataEventName()+"=\"resetChildSelectBoxData(this)\"";
        }
        this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,event,1);
    }

    protected String getRefreshChildboxDataEventName()
    {
        return "onblur";
    }

    protected void processStylePropertyAfterMerged()
    {
        if(this.tip!=null&&!this.tip.trim().equals(""))
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"title=\""+this.tip+"\"",1);
        }
        if(!(this.owner instanceof ConditionBean))
        {
            this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"onblur=\"try{addInputboxDataForSaving('"
                    +this.getOwner().getReportBean().getGuid()+"',this);}catch(e){logErrorsAsJsFileLoad(e);}\"",1);
        }
        //this.styleproperty=Tools.mergeHtmlTagPropertyString(this.styleproperty,"typename=\""+this.typename+"\"",1);
    }

    protected abstract String doGetDisplayStringValue(ReportRequest rrequest,String value,String style_property,boolean isReadonly);

    public abstract String filledInContainer();

    public String doPostFilledInContainer()
    {
        return "";
    }
    
    public String fillBoxValueToParentElement()
    {
        StringBuilder resultBuf=new StringBuilder();
        resultBuf.append("  realvalue=boxObj.value; displayvalue=boxObj.value;");
        resultBuf.append("  if(displayvalue==null) displayvalue=''; if(realvalue==null) realvalue='';");
        //resultBuf.append("  else{ value=value.replace(/</g,'&lt;');value=value.replace(/>/g,'&gt;');value=value.replace(/\\\'/g,'&#039;');value=value.replace(/\\\"/g,'&quot;');}");
        resultBuf.append("  var updateDestElementObj=getUpdateColDestObj(parentElementObj,reportguid,parentElementObj);");
        resultBuf.append("  updateDestElementObj.setAttribute('value',realvalue);");
        return resultBuf.toString();
    }
    
    protected abstract String getDefaultStylePropertyForDisplayMode2();
    
    public abstract String getIndependentDisplayString(ReportRequest rrequest,String value,String dynstyleproperty,Object specificDataObj,
            boolean isReadonly);
    
    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    public Object clone(IInputBoxOwnerBean owner)
    {
        try
        {
            AbsInputBox inputBoxNew=(AbsInputBox)clone();
            inputBoxNew.setOwner(owner);
            if(this.autoCompleteBean!=null)
            {
                inputBoxNew.setAutoCompleteBean(this.autoCompleteBean.clone(inputBoxNew));
                owner.getReportBean().addInputboxWithAutoComplete(inputBoxNew);
            }
            return inputBoxNew;
        }catch(CloneNotSupportedException e)
        {
            throw new WabacusConfigLoadingException("clone输入框对象失败",e);
        }
    }
}
