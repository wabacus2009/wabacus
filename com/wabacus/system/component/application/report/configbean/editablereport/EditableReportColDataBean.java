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

import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.CacheDataBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.assistant.EditableReportAssistant;
import com.wabacus.system.inputbox.PasswordBox;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public class EditableReportColDataBean
{
    private String editvalue;

    private String encodeEditvalue;

    private String defaultvalue;

    private String encodeDefaultvalue;


    private boolean isEditable;

    private boolean isNeedDefaultValue;

    private boolean isNeedEncode;

    private int displaymode;
    
    private String valuename;
    
    private String value;
    
    private String oldvalue;
    
    public String getEditvalue()
    {
        return editvalue;
    }

    public void setEditvalue(String editvalue)
    {
        this.editvalue=editvalue;
    }

    public String getEncodeEditvalue()
    {
        return encodeEditvalue;
    }

    public void setEncodeEditvalue(String encodeEditvalue)
    {
        this.encodeEditvalue=encodeEditvalue;
    }

    public String getDefaultvalue()
    {
        return defaultvalue;
    }

    public void setDefaultvalue(String defaultvalue)
    {
        this.defaultvalue=defaultvalue;
    }

    public String getEncodeDefaultvalue()
    {
        return encodeDefaultvalue;
    }

    public void setEncodeDefaultvalue(String encodeDefaultvalue)
    {
        this.encodeDefaultvalue=encodeDefaultvalue;
    }

//    }
//    }

    public boolean isEditable()
    {
        return isEditable;
    }

    public void setEditable(boolean isEditable)
    {
        this.isEditable=isEditable;
    }

    public boolean isNeedDefaultValue()
    {
        return isNeedDefaultValue;
    }

    public void setNeedDefaultValue(boolean isNeedDefaultValue)
    {
        this.isNeedDefaultValue=isNeedDefaultValue;
    }

    public boolean isNeedEncode()
    {
        return isNeedEncode;
    }

    public void setNeedEncode(boolean isNeedEncode)
    {
        this.isNeedEncode=isNeedEncode;
    }

    public int getDisplaymode()
    {
        return displaymode;
    }

    public void setDisplaymode(int displaymode)
    {
        this.displaymode=displaymode;
    }

    public String getValuename()
    {
        if(valuename==null) return "";
        return valuename;
    }

    public void setValuename(String valuename)
    {
        this.valuename=valuename;
    }

    public String getValue()
    {
        if(value==null) return "";
        return value;
    }

    public void setValue(String value)
    {
        this.value=value;
    }

    public String getOldvalue()
    {
        if(oldvalue==null) return "";
        return oldvalue;
    }

    public void setOldvalue(String oldvalue)
    {
        this.oldvalue=oldvalue;
    }

    public static EditableReportColDataBean createInstance(ReportRequest rrequest,CacheDataBean cdb,AbsEditableReportEditDataBean editbean,
            ColBean cbean,String col_editvalue,EditableReportSecretColValueBean currentSecretColValuesBean)
    {
        EditableReportColDataBean ercdbean=new EditableReportColDataBean();
        ercdbean.setEditvalue(col_editvalue);
        int displaymode=cdb.getColDisplayModeAfterAuthorize(cbean,rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE);
        ercdbean.setDisplaymode(displaymode);
        if(displaymode<0)
        {
            ercdbean.setEncodeEditvalue(cbean.getColid()+"_"+currentSecretColValuesBean.getUniqueEncodeString(6));
            ercdbean.setNeedEncode(true);
            currentSecretColValuesBean.addParamValue(ercdbean.getEncodeEditvalue(),col_editvalue);
        }
        ColBean cbeanUpdateSrc=cbean.getUpdateColBeanSrc(false);
        if(cbeanUpdateSrc==null) cbeanUpdateSrc=cbean;
        EditableReportColBean ercbeanSrc=(EditableReportColBean)cbeanUpdateSrc.getExtendConfigDataForReportType(EditableReportColBean.class);
        if(ercbeanSrc==null||cbean.isSequenceCol()||cbean.isControlCol()
                ||(Consts.COL_DISPLAYTYPE_HIDDEN.equals(cbean.getDisplaytype(rrequest.getShowtype()==Consts.DISPLAY_ON_PAGE))&&cbean.getUpdateColBeanSrc(false)==null))
        {
            return formatInstance(cbean,ercdbean);
        }
        if(editbean!=null)
        {
            if(editbean instanceof EditableReportInsertDataBean)
            {
                ercdbean.setEditable(ercbeanSrc.isEditableForInsert());
            }else if(editbean instanceof EditableReportUpdateDataBean)
            {
                ercdbean.setEditable(ercbeanSrc.isEditableForUpdate());
            }
        }
        if(displaymode<0) return formatInstance(cbean,ercdbean);
        if((col_editvalue==null||col_editvalue.trim().equals(""))&&ercdbean.isEditable())
        {//editvalue为空，且当前列是可编辑的，即有输入框，且当前列有显示权限
            String defaultvalue=ercbeanSrc.getInputbox().getDefaultvalue(rrequest);
            if(defaultvalue!=null&&!defaultvalue.equals(""))
            {
                if(cbean.getUpdateColBeanDest(false)!=null)
                {
                    ercdbean.setDefaultvalue(ercbeanSrc.getInputbox().getDefaultlabel(rrequest));
                }else
                {
                    ercdbean.setDefaultvalue(defaultvalue);
                }
                ercdbean.setNeedDefaultValue(true);
            }
        }
        if(ercdbean.isEditable()&&ercbeanSrc.getInputbox() instanceof PasswordBox)
        {
            int encodelength=((PasswordBox)ercbeanSrc.getInputbox()).getEncodelength();
            if(encodelength>0)
            {
                String encodevalue=currentSecretColValuesBean.getUniqueEncodeString(encodelength);//这里不加上colid，是为了保持用户指定的编码长度
                if(col_editvalue!=null&&!col_editvalue.trim().equals(""))
                {
                    currentSecretColValuesBean.addParamValue(encodevalue,col_editvalue);
                    ercdbean.setEncodeEditvalue(encodevalue);
                    ercdbean.setNeedEncode(true);
                }else if(ercdbean.isNeedDefaultValue())
                {
                    currentSecretColValuesBean.addParamValue(encodevalue,ercdbean.getDefaultvalue());
                    ercdbean.setEncodeDefaultvalue(encodevalue);
                    ercdbean.setNeedEncode(true);
                }
            }
        }
        return formatInstance(cbean,ercdbean);
    }
    
    private static EditableReportColDataBean formatInstance(ColBean cbean,EditableReportColDataBean ercdbean)
    {
        String value_name=EditableReportAssistant.getInstance().getColParamName(cbean);
        if(ercdbean.isNeedEncode())
        {
            ercdbean.setValuename(Consts_Private.COL_NONDISPLAY_PERMISSION_PREX+value_name);
            ercdbean.setOldvalue(ercdbean.getEncodeEditvalue());
        }else
        {
            ercdbean.setValuename(value_name);
            ercdbean.setOldvalue(ercdbean.getEditvalue());
        }
        if(!ercdbean.isEditable()||ercdbean.getDisplaymode()<0)
        {
            ercdbean.setValue(ercdbean.getOldvalue());
        }else
        {
            if(ercdbean.isNeedDefaultValue())
            {
                ercdbean.setValue(ercdbean.isNeedEncode()?ercdbean.getEncodeDefaultvalue():ercdbean.getDefaultvalue());
            }else
            {
                ercdbean.setValue(ercdbean.getOldvalue());
            }
        }
        return ercdbean;
    }
}
