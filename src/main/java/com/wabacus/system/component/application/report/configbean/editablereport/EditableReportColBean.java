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
package com.wabacus.system.component.application.report.configbean.editablereport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.component.application.report.AbsConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.config.component.application.report.ReportBean;
import com.wabacus.config.component.application.report.extendconfig.AbsExtendConfigBean;
import com.wabacus.exception.WabacusConfigLoadingException;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.component.application.report.abstractreport.configbean.AbsListReportColBean;
import com.wabacus.system.inputbox.AbsInputBox;
import com.wabacus.system.inputbox.IInputBoxOwnerBean;
import com.wabacus.system.inputbox.validate.ServerValidateBean;
import com.wabacus.util.Tools;

public final class EditableReportColBean extends AbsExtendConfigBean implements IInputBoxOwnerBean
{
    private static Log log=LogFactory.getLog(EditableReportColBean.class);
    
    private String updatecolDest;

    private String updatecolSrc;//被哪个<col/>通过updatecol属性引用，这里存放相应<col/>的property。只有hidden=1或hidden=2才能被别的<col/>引用。
    
    private String defaultvalue;

    private String textalign;

    private AbsInputBox inputbox;

    private int editableWhenInsert;//本列添加时是否可编辑，如果为0，不可编辑；1：因为显示辅助输入框可编辑；2：因为在<insert/>指定了它可编辑；
    
    private int editableWhenUpdate;//本列修改时是否可编辑如果为0，不可编辑；1：因为显示辅助输入框可编辑；2：因为在<update/>指定了它可编辑；
    
    private String formatemplate;//对于editablelist2/editabledetail2报表类型，用于格式化编辑列的显示的。
    
    private Map<String,String> mDynpartsInFormatemplate;
    
    private ServerValidateBean serverValidateBean;
    
    private String onsetvalueMethod;//设置此列值时的客户端回调函数
    
    private String ongetvalueMethod;
    
    public EditableReportColBean(AbsConfigBean owner)
    {
        super(owner);
    }

    public void setUpdatecolDest(String updatecol)
    {
        if(updatecol.trim().equals(""))
        {
            this.updatecolDest=null;
        }else
        {
            if(!Tools.isDefineKey("@",updatecol))
            {
                throw new WabacusConfigLoadingException("加载报表"+((ColBean)getOwner()).getReportBean().getPath()+"失败，column为"
                        +((ColBean)getOwner()).getColumn()+"的<col/>的value属性配置不合法，必须配置为@{其它<col/>的property}格式");
            }
            this.updatecolDest=Tools.getRealKeyByDefine("@",updatecol);
        }
    }

    public String getUpdatecolSrc()
    {
        return updatecolSrc;
    }

    public void setUpdatecolSrc(String updatecolSrc)
    {
        this.updatecolSrc=updatecolSrc;
    }

    public String getUpdatecolDest()
    {
        return updatecolDest;
    }

    public String getDefaultvalue()
    {
        return defaultvalue;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public void setServerValidateBean(ServerValidateBean svbean)
    {
        this.serverValidateBean=svbean;
    }

    public ServerValidateBean getServerValidateBean()
    {
        return serverValidateBean;
    }

    public AbsInputBox getInputbox()
    {
        if(this.inputbox==null)
        {
            this.inputbox=Config.getInstance().getInputBoxTypeByName(null);
            this.inputbox=(AbsInputBox)this.inputbox.clone(this);
            this.inputbox.initDisplayModeAndStyle();
        }
        return inputbox;
    }

    public void setInputbox(AbsInputBox inputbox)
    {
        this.inputbox=inputbox;
    }

    public String getTextalign()
    {
        return textalign;
    }

    public void setTextalign(String textalign)
    {
        this.textalign=textalign;
    }

    public int getEditableWhenInsert()
    {
        return editableWhenInsert;
    }

    public void setFormatemplate(String formatemplate)
    {
        this.formatemplate=formatemplate;
    }

    public String getFormatemplate()
    {
        return formatemplate;
    }

    public void setOnsetvalueMethod(String onsetvalueMethod)
    {
        this.onsetvalueMethod=onsetvalueMethod;
    }

    public void setOngetvalueMethod(String ongetvalueMethod)
    {
        this.ongetvalueMethod=ongetvalueMethod;
    }

    public String getOnsetvalueMethod()
    {
        return onsetvalueMethod;
    }

    public String getOngetvalueMethod()
    {
        return ongetvalueMethod;
    }

    public String getFormatemplate(ReportRequest rrequest)
    {
        if(this.formatemplate==null||this.mDynpartsInFormatemplate==null||this.mDynpartsInFormatemplate.size()==0) return this.formatemplate;
        String realformatemplate=this.formatemplate;
        for(Entry<String,String> entryTmp:this.mDynpartsInFormatemplate.entrySet())
        {
            if(!Tools.isDefineKey("@",entryTmp.getValue()))
            {
                realformatemplate=Tools.replaceAll(realformatemplate,entryTmp.getKey(),WabacusAssistant.getInstance().getRequestContextStringValue(
                        rrequest,entryTmp.getValue(),""));
            }
        }
        return realformatemplate;
    }
    
    public String getColPropertyAndPlaceHoldersInFormatemplate()
    {
        if(this.mDynpartsInFormatemplate==null||this.mDynpartsInFormatemplate.size()==0) return "";
        StringBuffer resultBuf=new StringBuffer();
        for(Entry<String,String> entryTmp:this.mDynpartsInFormatemplate.entrySet())
        {
            if(Tools.isDefineKey("@",entryTmp.getValue()))
            {
                resultBuf.append(entryTmp.getKey()+"="+Tools.getRealKeyByDefine("@",entryTmp.getValue())).append(";");
            }
        }
        return resultBuf.toString();
    }
    
    public String getRealColDisplayValue(ReportRequest rrequest,AbsReportDataPojo dataObj,String col_displayvalue)
    {
        if(this.formatemplate==null||this.formatemplate.trim().equals("")) return col_displayvalue;
        if(col_displayvalue==null||col_displayvalue.equals(""))
        {
            if(!this.formatemplate.startsWith("[")||!this.formatemplate.endsWith("]")) return "";
        }
        if(this.mDynpartsInFormatemplate==null||this.mDynpartsInFormatemplate.size()==0) return this.formatemplate;//模板中没有动态内容
        String realformatemplate=getFormatemplate(rrequest);
        if(realformatemplate.startsWith("[")&&realformatemplate.endsWith("]")) realformatemplate=realformatemplate.substring(1,realformatemplate.length()-1);
        String colpropTmp,colvalTmp;
        for(Entry<String,String> entryTmp:this.mDynpartsInFormatemplate.entrySet())
        {
            if(Tools.isDefineKey("@",entryTmp.getValue()))
            {
                colpropTmp=Tools.getRealKeyByDefine("@",entryTmp.getValue());
                if(colpropTmp.endsWith("__old")) colpropTmp=colpropTmp.substring(0,colpropTmp.length()-5);
                colvalTmp=dataObj.getColStringValue(colpropTmp);
                if(colvalTmp==null) colvalTmp="";
                realformatemplate=Tools.replaceAll(realformatemplate,entryTmp.getKey(),colvalTmp);
            }
        }
        return realformatemplate;
    }
    
    public Map<String,String> getMDynpartsInFormatemplate()
    {
        return mDynpartsInFormatemplate;
    }

    public void setEditableWhenInsert(int editableWhenInsert)
    {
        if(editableWhenInsert>0)
        {
            ColBean cbean=(ColBean)this.getOwner();
            AbsListReportColBean lcolbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportColBean.class);
            if(lcolbean!=null&&lcolbean.isRowgroup())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"上的列"+cbean.getColumn()
                        +"失败，此列为分组列，不能将其配置为可编辑，可以用editablelist报表类型编辑此列");
            }
        }
        this.editableWhenInsert=editableWhenInsert;
    }

    public int getEditableWhenUpdate()
    {
        return editableWhenUpdate;
    }

    public void setEditableWhenUpdate(int editableWhenUpdate)
    {
        if(editableWhenUpdate>0)
        {
            ColBean cbean=(ColBean)this.getOwner();
            AbsListReportColBean lcolbean=(AbsListReportColBean)cbean.getExtendConfigDataForReportType(AbsListReportColBean.class);
            if(lcolbean!=null&&lcolbean.isRowgroup())
            {
                throw new WabacusConfigLoadingException("加载报表"+this.getOwner().getReportBean().getPath()+"上的列"+cbean.getColumn()
                        +"失败，此列为分组列，不能将其配置为可编辑，可以用editablelist报表类型编辑此列");
            }
        }
        this.editableWhenUpdate=editableWhenUpdate;
    }

    public String getOwnerId()
    {
        return ((ColBean)this.getOwner()).getProperty();
    }

    public String getInputBoxId()
    {
        return EditableReportAssistant.getInstance().getInputBoxId((ColBean)this.getOwner());
    }

    public ReportBean getReportBean()
    {
        return this.getOwner().getReportBean();
    }

    public String getLabel(ReportRequest rrequest)
    {
        return ((ColBean)this.getOwner()).getLabel(rrequest);
    }
    
    public boolean isEditableForInsert()
    {
        return this.editableWhenInsert>0;
    }
    
    public boolean isEditableForUpdate()
    {
        return this.editableWhenUpdate>0;
    }
    
    public void doPostLoad()
    {
        if(this.defaultvalue!=null&&Tools.isDefineKey("url",this.defaultvalue))
        {
            this.getOwner().getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",this.defaultvalue));
        }
        if(this.formatemplate==null||this.formatemplate.trim().equals(""))
        {
            this.mDynpartsInFormatemplate=null;
        }else
        {
            this.mDynpartsInFormatemplate=new HashMap<String,String>();
            this.formatemplate=WabacusAssistant.getInstance().parseDynPartsInConfigValue(this.formatemplate,this.mDynpartsInFormatemplate,
                    new String[] { "@", "url", "request", "rrequest", "session" });
            for(Entry<String,String> entryTmp:this.mDynpartsInFormatemplate.entrySet())
            {
                if(Tools.isDefineKey("url",entryTmp.getValue()))
                {
                    this.getOwner().getReportBean().addParamNameFromURL(Tools.getRealKeyByDefine("url",entryTmp.getValue()));
                }
            }
            if(this.formatemplate!=null&&!this.formatemplate.equals("")&&(this.mDynpartsInFormatemplate==null||this.mDynpartsInFormatemplate.size()==0))
            {
                log.warn("报表"+this.getOwner().getReportBean().getPath()+"的列"+((ColBean)this.getOwner()).getProperty()+"配置的formatemplate是个常量字符串，将无法正常显示本列的内容");
            }
        }
        if(this.getInputbox()!=null)
        {
            this.getInputbox().doPostLoad();
        }
        this.ongetvalueMethod=parseOnGetSetValueMethod(this.ongetvalueMethod);
        this.onsetvalueMethod=parseOnGetSetValueMethod(this.onsetvalueMethod);
    }

    private String parseOnGetSetValueMethod(String method)
    {
        if(Tools.isEmpty(method)) return null;
        List<String> lstMethods=Tools.parseStringToList(method,";",false);
        StringBuilder bufTmp=new StringBuilder();
        for(String methodTmp:lstMethods)
        {
            bufTmp.append("{method:"+methodTmp+"},");
        }
        method=bufTmp.toString();
        if(method.endsWith(",")) method=method.substring(0,method.length()-1);
        if(!method.trim().equals("")) method="["+method+"]";
        return method;
    }
    
    public AbsExtendConfigBean clone(AbsConfigBean owner)
    {
        EditableReportColBean ercbeanNew=(EditableReportColBean)super.clone(owner);
        if(inputbox!=null)
        {
            ercbeanNew.setInputbox((AbsInputBox)inputbox.clone(ercbeanNew));
        }
        return ercbeanNew;
    }
}
