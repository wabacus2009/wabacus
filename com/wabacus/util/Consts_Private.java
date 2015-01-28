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
package com.wabacus.util;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Consts_Private
{
    public final static String GLOBAL_SHEETIDX_KEY="global_sheetidx_key";

    public final static String GUID_SEPERATOR="_guid_";

    public final static String PATH_SEPERATOR=".";
    
    public final static String SKIN_PLACEHOLDER="%SKIN%";
    
    /***************************************************************************
     * <col/>的几个特殊column值的意义
     **************************************************************************/
    public final static String NON_VALUE="{non-value}";

    public final static String NON_FROMDB="{non-fromdb}";

    public final static String SEQUENCE="{sequence}";//对于数据自动列表报表，当前列用于显示序列，此时格式为{sequence:startnum}，其中startnum表示起始序列号。此时可以不配置property

    public final static String COL_ROWSELECT="{col-rowselect}";
    
    public final static String COL_ROWORDER_ARROW="{roworder-arrow}";
    
    public final static String COL_ROWORDER_INPUTBOX="{roworder-inputbox}";
    
    public final static String COL_ROWORDER_TOP="{roworder-top}";
    
    public final static String COL_EDITABLELIST_EDIT="{editablelist-edit}";//对于editablelist报表类型，用于显示编辑的列
    
    public final static String LANGUAGE_ZH="zh";

    public final static String LANGUAGE_EN="en";

    public final static String LANGUAGE_I18N="i18n";

    public final static String SEARCH_BUTTON="search";

    public final static String ADD_BUTTON="add";//添加按钮

    public final static String MODIFY_BUTTON="modify";

    public final static String DELETE_BUTTON="delete";

    public final static String SAVE_BUTTON="save";

    public final static String RESET_BUTTON="reset";

    public final static String BACK_BUTTON="back";//返回按钮
    
    public final static String FORWARDWITHBACK_BUTTON="forwardwithback";

    public final static String CANCEL_BUTTON="cancel";

    //    public final static String TAGNAME_REPORT="report";
    
    public final static String TAGNAME_DATA="data";

    public final static String TAGNAME_NAVIGATE="navigate";

    public final static String TAGNAME_BUTTON="button";

    public final static String TAGNAME_HEADER="header";

    public final static String TAGNAME_ITERATOR="iterator";

    public final static String TAGNAME_FOOTER="footer";

    public final static String TAGNAME_TITLE="title";

    public final static String TAGNAME_SEARCHBOX="searchbox";

    public final static String TAGNAME_DATAIMPORT="dataimport";
    
    public final static String TAGNAME_FILEUPLOAD="fileupload";
    
    public final static String TAGNAME_OUTPUT="output";
    
    public final static String NAVIGATE_FIRST="first";

    public final static String NAVIGATE_PREVIOUS="previous";

    public final static String NAVIGATE_NEXT="next";

    public final static String NAVIGATE_LAST="last";

    public final static String NAVIGATE_SEQUENCE="sequence";//连续页码

    public final static String NAVIGATE_PAGENO="pageno";

    public final static String NAVIGATE_PAGESIZE="pagesize";

    public final static String NAVIGATE_RECORDCOUNT="recordcount";

    public final static String NAVIGATE_PAGECOUNT="pagecount";

    public final static String DATAIMPORT_LOCKFILENAME="dataimport.lck";//数据导入的文件锁名
    
    public final static String DATAIMPORTTYPE_OVERWRITE="overwrite";

    public final static String DATAIMPORTTYPE_APPEND="append";
    
    public final static String DATAIMPORTTYPE_DELETE="delete";
    
    public static final String DATAIMPORT_MATCHMODE_INITIAL="initial";
    
    public static final String DATAIMPORT_MATCHMODE_LAZY="lazy";//在第一次进行数据导入时建立
    
    public static final String DATAIMPORT_MATCHMODE_EVERYTIME="everytime";
    
    public static final List<String> LST_DATAIMPORT_MATCHMODES=new ArrayList<String>();
    static
    {
        LST_DATAIMPORT_MATCHMODES.add(DATAIMPORT_MATCHMODE_INITIAL);
        LST_DATAIMPORT_MATCHMODES.add(DATAIMPORT_MATCHMODE_LAZY);
        LST_DATAIMPORT_MATCHMODES.add(DATAIMPORT_MATCHMODE_EVERYTIME);
    }
    
    public final static String FILEUPLOADTYPE_FILEINPUTBOX="fileinputbox";

    public final static String FILEUPLOADTYPE_FILETAG="filetag";

    public final static String FILEUPLOADTYPE_DATAIMPORTREPORT="dataimportreport";//为某个报表配置的数据导入功能的数据文件上传

    public final static String FILEUPLOADTYPE_DATAIMPORTTAG="dataimporttag";
    
    /******************************************************************************
     * 报表onload回调函数类型KEY
     ******************************************************************************/
    public final static String ONLOAD_CONFIG="config_onloadmethods";
    
//    public final static String ONLOAD_AFTERSAVE="aftersave_onloadmethods";//可编辑报表类型保存后客户端回调函数
    
//    public final static String ONLOAD_CHILD="child_onloadmethods";//容器中要执行的子组件的onload函数
    
    public final static String ONLOAD_REFRESHSLAVE="refreshslave_onloadmethods";
    
    public final static String ONlOAD_IMGSCROLL="imagescroll_onloadmethods";
    
    public final static String ONlOAD_CURVETITLE="curvetitle_onloadmethods";//为显示折线数据表头而自动生成的onload函数类型
    
    public final static String PLACEHOLDER_LISTREPORT_SQLKERNEL="%listreport_sql_kernel%";
    
    public final static Map<String,Integer> M_ALL_TRANSACTION_LEVELS=new HashMap<String,Integer>();
    
    static 
    {
        M_ALL_TRANSACTION_LEVELS.put(Consts.TRANS_NONE,Connection.TRANSACTION_NONE);
        M_ALL_TRANSACTION_LEVELS.put(Consts.TRANS_READ_UNCOMMITTED,Connection.TRANSACTION_READ_UNCOMMITTED);
        M_ALL_TRANSACTION_LEVELS.put(Consts.TRANS_READ_COMMITTED,Connection.TRANSACTION_READ_COMMITTED);
        M_ALL_TRANSACTION_LEVELS.put(Consts.TRANS_REPEATABLE_READ,Connection.TRANSACTION_REPEATABLE_READ);
        M_ALL_TRANSACTION_LEVELS.put(Consts.TRANS_SERIALIZABLE,Connection.TRANSACTION_SERIALIZABLE);
    }
    
    public final static String SAVE_ROWDATA_SEPERATOR="_ROTAREPES_ATADWOR_GNIVAS_";
    
    public final static String SAVE_COLDATA_SEPERATOR="_ROTAREPES_ATADLOC_GNIVAS_";
    
    public final static String SAVE_NAMEVALUE_SEPERATOR="_ROTAREPES_EULAVEMAN_GNIVAS_";
    
    public final static String COL_NONDISPLAY_PERMISSION_PREX="[NOISSIMREP_YALPSIDNON]";
    
    public final static String REPORT_TEMPLATE_NONE="none";
    
    
    public final static String REPORT_FAMILY_LIST="list";//所有只读的数据列表报表，包括simplelist、list、crosslist等报表类型

    public final static String REPORT_FAMILY_DETAIL="detail";

    public final static String REPORT_FAMILY_EDITABLELIST="editablelist";
    
    public final static String REPORT_FAMILY_EDITABLELIST2="editablelist2";

    public final static String REPORT_FAMILY_LISTFORM="listform";

    public final static String REPORT_FAMILY_EDITABLEDETAIL2="editabledetail2";

    public final static String REPORT_FAMILY_EDITABLEDETAIL="editabledetail";

    public final static String REPORT_FAMILY_FORM="form";
    
    public final static String REPORT_FAMILY_FUSIONCHARTS="fusioncharts";
    
    public final static String REPORT_BORDER_NONE0="none0";

    public final static String REPORT_BORDER_NONE1="none1";

    public final static String REPORT_BORDER_HORIZONTAL0="horizontal0";

    public final static String REPORT_BORDER_HORIZONTAL1="horizontal1";//只显示横向边框，但显示最外围表格的纵向边框
    
    public final static String REPORT_BORDER_HORIZONTAL2="horizontal2";

    public final static String REPORT_BORDER_VERTICAL="vertical";

    public final static String REPORT_BORDER_ALL="all";
    
    public final static List<String> lstAllReportBorderTypes=new ArrayList<String>();

    static
    {
        lstAllReportBorderTypes.add(REPORT_BORDER_NONE0);
        lstAllReportBorderTypes.add(REPORT_BORDER_NONE1);
        lstAllReportBorderTypes.add(REPORT_BORDER_HORIZONTAL0);
        lstAllReportBorderTypes.add(REPORT_BORDER_HORIZONTAL1);
        lstAllReportBorderTypes.add(REPORT_BORDER_HORIZONTAL2);
        lstAllReportBorderTypes.add(REPORT_BORDER_VERTICAL);
        lstAllReportBorderTypes.add(REPORT_BORDER_ALL);
    } 
    
    public final static String SCROLLSTYLE_NORMAL="normal";//普通滚动条
    
    public final static String SCROLLSTYLE_IMAGE="image";
    
    public final static List<String> lstAllScrollStyles=new ArrayList<String>();
    
    static
    {
        lstAllScrollStyles.add(SCROLLSTYLE_NORMAL);
        lstAllScrollStyles.add(SCROLLSTYLE_IMAGE);
    }
    
    public final static String ROWGROUP_COMMON="commonrowgroup";
    
    public final static String ROWGROUP_COLTREE="coltreerowgroup";
    
    public final static String ROWGROUP_DATATREE="datatreerowgroup";//根据数据确定的树形分组
}
