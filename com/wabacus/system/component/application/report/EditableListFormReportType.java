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

import java.util.List;

import com.wabacus.config.component.IComponentConfigBean;
import com.wabacus.config.component.application.report.AbsReportDataPojo;
import com.wabacus.config.component.application.report.ColBean;
import com.wabacus.system.ReportRequest;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColBean;
import com.wabacus.system.component.application.report.configbean.editablereport.EditableReportColDataBean;
import com.wabacus.system.component.container.AbsContainerType;
import com.wabacus.util.Consts;
import com.wabacus.util.Consts_Private;

public class EditableListFormReportType extends EditableListReportType2
{
    public EditableListFormReportType(AbsContainerType parentContainerType,IComponentConfigBean comCfgBean,ReportRequest rrequest)
    {
        super(parentContainerType,comCfgBean,rrequest);
    }

    public final static String KEY=EditableListFormReportType.class.getName();

    /*********************************************************************
     * 说明：对于listform报表类型，虽然读写列数据时都不会从<td/>的value中取值，但目前仍需要保留此属性，并在列数据变更时变更其value属性值
     *      这是因为修改列值后，要通过value属性判断是否变更，并决定是否需要保存到后台
     *      添加记录时，也会先将默认值设置到value属性中，然后填充输入框时再从value属性中取默认值
     ********************************************************************/
    
    
    protected String showInputBoxForCol(StringBuffer tdPropBuf,EditableReportColBean ercbean,int rowindex,AbsReportDataPojo object,ColBean cbean,
            EditableReportColDataBean ercdatabean)
    {
        boolean isReadonlyPermission=cbean.checkReadonlyPermission(rrequest);
        rrequest.getAttributes().put("DYN_INPUTBOX_ID",ercbean.getInputBoxId()+"__"+rowindex);
        /*if((editValueBean.getEditvalue()==null||editValueBean.getEditvalue().trim().equals(""))&&(ercbean.getInputbox() instanceof SelectBox))
        {
            String defaultvalue=ercbean.getInputbox().getDefaultvalue(rrequest);
            if(defaultvalue==null||defaultvalue.trim().equals(""))
            {
                List<String[]> lstOptions=null;
                if(((SelectBox)ercbean.getInputbox()).getParentid()==null||((SelectBox)ercbean.getInputbox()).getParentid().trim().equals(""))
                {
                    lstOptions=((SelectBox)ercbean.getInputbox()).getLstOptionsFromCache(rrequest,ercbean.getInputBoxId());
                }else
                {
                    lstOptions=((SelectBox)ercbean.getInputbox()).getLstOptionsFromCache(rrequest,ercbean.getInputBoxId()+"__"+rowindex);
                }
                if(lstOptions!=null&&lstOptions.size()>0&&!((SelectBox)ercbean.getInputbox()).hasBlankValueOption(lstOptions))
                {//此时将下拉框第一个选项值设置给它，以免保存时，下拉框默认选中了第一个值，而从它的<td/>中取不到值。
                    tdPropBuf.append(" value=\"").append(Tools.htmlEncode(lstOptions.get(0)[1])).append("\"");
                }
            }
        }*/
        String strvalue=ercbean.getInputbox().getDisplayStringValue(rrequest,ercdatabean.getValue(),
                "style=\"text-align:"+ercbean.getTextalign()+";\"",isReadonlyPermission);
        rrequest.getAttributes().remove("DYN_INPUTBOX_ID");//用完一定要清掉，否则可能会被同一页面的其它报表用上
        return strvalue;
    }

    protected String getDataTdClassName()
    {
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE||Consts_Private.REPORT_BORDER_HORIZONTAL2.equals(rbean.getBorder())) 
        {
            return super.getDataTdClassName();
        }
        return "cls-data-td-listform";
    }

    protected void initInputBox(StringBuilder resultBuf)
    {
        super.initInputBox(resultBuf);
        if(rrequest.getShowtype()!=Consts.DISPLAY_ON_PAGE) return;
        if(ersqlbean.getInsertbean()==null) return;
        List<ColBean> lstColBeans=this.getLstDisplayColBeans();
        EditableReportColBean ercbeanTmp;
        for(ColBean cbean:lstColBeans)
        {
            if(this.cacheDataBean.getColDisplayModeAfterAuthorize(cbean,true)<=0) continue;
            if(cbean.isSequenceCol()||cbean.isControlCol()) continue;
            ercbeanTmp=(EditableReportColBean)cbean.getExtendConfigDataForReportType(EditableListReportType2.KEY);
            if(ercbeanTmp!=null&&ercbeanTmp.isEditableForInsert())
            {
                resultBuf.append(ercbeanTmp.getInputbox().initDisplay(rrequest));
            }
        }
    }

    public String getColOriginalValue(AbsReportDataPojo object,ColBean cbean)
    {
        if(cbean==null||object==null) return "";
        return object.getColStringValue(cbean);
    }

    protected boolean getDefaultShowContextMenu()
    {
        return false;
    }
    
    public String getReportFamily()
    {
        return Consts_Private.REPORT_FAMILY_LISTFORM;
    }
}
