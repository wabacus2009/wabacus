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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Consts
{
    /***************************************************************************
     * 生成报表pojo字节码存放的包名
     **************************************************************************/
    public final static String BASE_PACKAGE_NAME="com.wabacus.generateclass";

    /***************************************************************************
     * 报表数据显示类型
     **************************************************************************/
    public final static int DISPLAY_ON_PAGE=1;

    public final static int DISPLAY_ON_RICHEXCEL=2;

    public final static int DISPLAY_ON_PLAINEXCEL=3;

    public final static int DISPLAY_ON_WORD=4;
    
    public final static int DISPLAY_ON_PDF=5;//显示在PDF中
    
    public final static int DISPLAY_ON_PRINT=6;

    public final static String DISPLAYTYPE_PARAMNAME="DISPLAY_TYPE";

    /***************************************************************************
     * 数据导出配置值
     **************************************************************************/
    public final static String DATAEXPORT_NONE="none";
    
    public final static String DATAEXPORT_WORD="word";
    
    public final static String DATAEXPORT_RICHEXCEL="richexcel";//导出富Excel文件
    
    public final static String DATAEXPORT_PLAINEXCEL="plainexcel";
    
    public final static String DATAEXPORT_PDF="pdf";
    
    public final static String IMPORT_DATA="dataimport";
    
    public final static List<String> lstDataExportTypes=new ArrayList<String>();
    static
    {
        lstDataExportTypes.add(DATAEXPORT_NONE);
        lstDataExportTypes.add(DATAEXPORT_WORD);
        lstDataExportTypes.add(DATAEXPORT_RICHEXCEL);
        lstDataExportTypes.add(DATAEXPORT_PLAINEXCEL);
        lstDataExportTypes.add(DATAEXPORT_PDF);
    }
    
    /***************************************************************************
     * 报表部分各部分的KEY的定义
     **************************************************************************/
    public final static String SEARCH_PART="searchbox";

    public final static String HEADER_PART="header";

    public final static String TITLE_PART="title";

    public final static String DATA_PART="data";

    public final static String FOOTER_PART="footer";

    public final static String NAVIGATE_PART="navigate";
    
    public final static String BUTTON_PART="button";
    
    public final static List<String> lstReportParts=new ArrayList<String>();
    
    static
    {
        lstReportParts.add(SEARCH_PART);
        lstReportParts.add(HEADER_PART);
        lstReportParts.add(TITLE_PART);
        lstReportParts.add(DATA_PART);
        lstReportParts.add(FOOTER_PART);
        lstReportParts.add(NAVIGATE_PART);
        lstReportParts.add(BUTTON_PART);
    }

    /***************************************************************************
     * 除了上面几个位置，新增定义按钮显示位置时的KEY
     **************************************************************************/
    public final static String OTHER_PART="other";
    
    public final static String CONTEXTMENU_PART="contextmenu";

    /***************************************************************************
     * 报表系统级资源项约定好的KEY
     **************************************************************************/
    public final static String SEARCHBOX_PREX_KEY="searchbox.prex";//查询框前导

    public final static String NODATA_PROMPT_KEY="nodata.mess";

    public final static String SEARCHBOX_OUTERSTYLE_KEY="searchbox_outer_style";

    public final static String LOADERROR_MESS_KEY="load.error.mess";

    public final static String WORD_LABEL="word.label";
    
    public final static String RICHEXCEL_LABEL="richexcel.label";//显示“下载EXCEL”的label
    
    public final static String PLAINEXCEL_LABEL="plainexcel.label";
    
    public final static String DATAIMPORT_LABEL="dataimport.label";
    
    public final static String FILEUPLOAD_LABEL="fileupload.label";

    public final static String SUBMIT_SEARCH_BUTTON="submit.search.button";
    
    public final static String NAVIGATE_ALLDATA_LABEL="navigate.alldata.label";// 翻页导航栏中切换页大小的下拉框中，不分页显示的label

    /***************************************************************************
     * 定义GridPageType页面类型的各列中所定义内容的类型
     **************************************************************************/
    public final static String GRIDPAGETYPE_CONSTANT_STRING="constant";

    //    public final static String GRIDPAGETYPE_INCLUDEPAGE_URL = "pageurl";//include其它子页面的URL

    public final static String GRIDPAGETYPE_PAGE_REPORT="report";

    /***************************************************************************
     * 客户端请求的操作类型
     **************************************************************************/
    public final static String SHOWREPORT_ACTION="showreport";

    public final static String GETFILTERDATALIST_ACTION="getFilterDataList";

    public final static String GETAUTOCOMPLETEDATA_ACTION="GetAutoCompleteFormData";//获取自动填充表单列的数据
    
    /***************************************************************************
     * 所有XML配置文件的namespace
     **************************************************************************/
    public final static String XML_NAMESPACE_KEY="default";

    public final static String XML_NAMESPACE_VALUE="http://www.wabacus.com";//命名空间的值

    /***************************************************************************
     * 默认值的键
     **************************************************************************/
    public final static String DEFAULT_KEY="default_default_default_key";

    /***************************************************************************
     * 可编辑表格的访问模式
     **************************************************************************/
    public final static String READONLY_MODE="readonly";
    
    public final static String READ_MODE="read";

    public final static String UPDATE_MODE="update";

    public final static String ADD_MODE="add";//插入模式

    /***************************************************************************
     * 系统级默认资源项的KEY
     **************************************************************************/
    public final static String REPORT_TEMPLATE_DEFAULT="report.template.default";

    public final static String DATAEXPORT_TEMPLATE_DEFAULT="dataexport.template.default";
    
    public final static String PRINT_TEMPLATE_DEFAULT="print.template.default";

    public final static String LISTREPORT_NAVIGATE_DEFAULT="listreport.navigate.default";
    
    public final static String DETAILREPORT_NAVIGATE_DEFAULT="detailreport.navigate.default";//默认数据细览报表的翻页导航栏静态模板资源项的KEY
    
    public final static String SEARCH_BUTTON_DEFAULT="search.button.default";

    public final static String ADD_BUTTON_DEFAULT="add.button.default";

    public final static String MODIFY_BUTTON_DEFAULT="modify.button.default";

    public final static String DELETE_BUTTON_DEFAULT="delete.button.default";

    public final static String SAVE_BUTTON_DEFAULT="save.button.default";//默认保存按钮资源项的KEY

    public final static String CANCEL_BUTTON_DEFAULT="cancel.button.default";

    public final static String RESET_BUTTON_DEFAULT="reset.button.default";

    public final static String BACK_BUTTON_DEFAULT="back.button.default";
    
    public final static String DATAIMPORT_BUTTON_DEFAULT="dataimport.button.default";
    
    public final static Map<String,String> M_DATAEXPORT_DEFAULTBUTTONS=new HashMap<String,String>();//存放每个导出类型中相应默认按钮KEY
    static
    {
        M_DATAEXPORT_DEFAULTBUTTONS.put(DATAEXPORT_WORD,"word.button.default");
        M_DATAEXPORT_DEFAULTBUTTONS.put(DATAEXPORT_RICHEXCEL,"richexcel.button.default");
        M_DATAEXPORT_DEFAULTBUTTONS.put(DATAEXPORT_PLAINEXCEL,"plainexcel.button.default");
        M_DATAEXPORT_DEFAULTBUTTONS.put(DATAEXPORT_PDF,"pdf.button.default");
    }

    public final static String PRINTTYPE_PRINT="print";//打印
    
    public final static String PRINTTYPE_PRINTPREVIEW="printpreview";
    
    public final static String PRINTTYPE_PRINTSETTING="printsetting";
    
    public final static Map<String,String> M_PRINT_DEFAULTBUTTONS=new HashMap<String,String>();
    static
    {
        M_PRINT_DEFAULTBUTTONS.put(PRINTTYPE_PRINT,"print.button.default");
        M_PRINT_DEFAULTBUTTONS.put(PRINTTYPE_PRINTPREVIEW,"printpreview.button.default");
        M_PRINT_DEFAULTBUTTONS.put(PRINTTYPE_PRINTSETTING,"printsetting.button.default");//默认打印设置按钮
    }
    public final static List<String> lstStatisticsType=new ArrayList<String>();
    static
    {
        lstStatisticsType.add("avg");
        lstStatisticsType.add("max");
        lstStatisticsType.add("min");
        lstStatisticsType.add("sum");
        lstStatisticsType.add("count");
    }
    
    /************************************************************************************
     * 可编辑报表类型的保存数据的事务隔离级别
     ***********************************************************************************/
    public final static String TRANS_NONE="none";

    public final static String TRANS_READ_UNCOMMITTED="read_uncommitted";

    public final static String TRANS_READ_COMMITTED="read_committed";

    public final static String TRANS_REPEATABLE_READ="read_repeatable";

    public final static String TRANS_SERIALIZABLE="serializable";
    
    public final static int SAVETYPE_ALONE=0;
    
    public final static int SAVETYPE_BINDING=1;
    
    public final static int SAVETYPE_BINDEDBY=2;
    
    /************************************************************************************
     * 数据自动列表报表行选中类型
     ***********************************************************************************/
    public final static String ROWSELECT_NONE="none";

    public final static String ROWSELECT_SINGLE="single";//提供单行选中功能

    public final static String ROWSELECT_MULTIPLE="multiple";

    public final static String ROWSELECT_CHECKBOX="checkbox";
    
    public final static String ROWSELECT_MULTIPLE_CHECKBOX="multiple-checkbox";
    
    public final static String ROWSELECT_RADIOBOX="radiobox";
    
    public final static String ROWSELECT_SINGLE_RADIOBOX="single-radiobox";//提供单选框的单选功能，且点击单选框或记录行都能选中
    
    public final static List<String> lstAllRowSelectTypes=new ArrayList<String>();
    
    static
    {
        lstAllRowSelectTypes.add(ROWSELECT_NONE);
        lstAllRowSelectTypes.add(ROWSELECT_SINGLE);
        lstAllRowSelectTypes.add(ROWSELECT_MULTIPLE);
        lstAllRowSelectTypes.add(ROWSELECT_CHECKBOX);
        lstAllRowSelectTypes.add(ROWSELECT_RADIOBOX);
        lstAllRowSelectTypes.add(ROWSELECT_MULTIPLE_CHECKBOX);
        lstAllRowSelectTypes.add(ROWSELECT_SINGLE_RADIOBOX);
    }
    
    /*********************************************************************************
     * 行排序的类型
     ********************************************************************************/
    public final static String ROWORDER_DRAG="drag";
    
    public final static String ROWORDER_ARROW="arrow";
    
    public final static String ROWORDER_INPUTBOX="inputbox";
    
    public final static String ROWORDER_TOP="top";//通过置顶方式进行排序
    
    public final static List<String> lstAllRoworderTypes=new ArrayList<String>();
    
    static
    {
        lstAllRoworderTypes.add(ROWORDER_DRAG);
        lstAllRoworderTypes.add(ROWORDER_ARROW);
        lstAllRoworderTypes.add(ROWORDER_INPUTBOX);
        lstAllRoworderTypes.add(ROWORDER_TOP);
    }
    
    /**********************************************************************************
     * 权限类型
     *********************************************************************************/
    public final static String PERMISSION_TYPE_DISPLAY="display";
    
    public final static String PERMISSION_TYPE_DISABLED="disabled";
    
    public final static String PERMISSION_TYPE_READONLY="readonly";
    
    public final static int CHKPERMISSION_YES=1;//显式为此组件/元素授了指定权限值

    public final static int CHKPERMISSION_NO=0;//显式没为此组件/元素授指定权限值

    public final static int CHKPERMISSION_EMPTY=-1;//没有为此组件/元素授此权限类型的权限值

    public final static int CHKPERMISSION_UNSUPPORTEDTYPE=-2;//权限类型为空或不支持的权限类型

    public final static int CHKPERMISSION_UNSUPPORTEDVALUE=-3;
    
    /*************************************************************************************
     * <col/>和<group/>的显示类型
     ************************************************************************************/
    public final static String COL_DISPLAYTYPE_INITIAL="initial";
    
    public final static String COL_DISPLAYTYPE_OPTIONAL="optional";
    
    public final static String COL_DISPLAYTYPE_HIDDEN="hidden";
    
    public final static String COL_DISPLAYTYPE_ALWAYS="always";//永远显示
    
    public final static List<String> lstAllColDisplayTypes=new ArrayList<String>();
    static
    {
        lstAllColDisplayTypes.add(COL_DISPLAYTYPE_INITIAL);
        lstAllColDisplayTypes.add(COL_DISPLAYTYPE_OPTIONAL);
        lstAllColDisplayTypes.add(COL_DISPLAYTYPE_HIDDEN);
        lstAllColDisplayTypes.add(COL_DISPLAYTYPE_ALWAYS);
    }
    /**************************************************************************************
     * 处理完请求后向客户端返回的状态码
     *************************************************************************************/
    public final static int STATECODE_NONREFRESHPAGE=0;
    
    public final static int STATECODE_FAILED=-1;
    
    public final static int STATECODE_SUCCESS=1;
    
    //public final static int STATECODE_NONE=Integer.MAX_VALUE;//保留已有的状态码，即本次不改变状态码
    
    /**************************************************************************************
     * 保存数据操作的类型
     *************************************************************************************/
    public final static int UPDATETYPE_INSERT=1;//当前是做添加记录的操作（即保存添加的记录）

    public final static int UPDATETYPE_UPDATE=2;

    public final static int UPDATETYPE_DELETE=3;
}
