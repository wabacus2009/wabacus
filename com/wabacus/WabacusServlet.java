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
package com.wabacus;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.wabacus.config.Config;
import com.wabacus.config.ConfigLoadManager;
import com.wabacus.config.database.datasource.AbsDataSource;
import com.wabacus.system.assistant.FilePathAssistant;
import com.wabacus.system.assistant.WabacusAssistant;
import com.wabacus.system.dataimport.thread.FileUpDataImportThread;
import com.wabacus.system.task.TimingThread;
import com.wabacus.util.Consts;
import com.wabacus.util.Tools;
import com.wabacus.util.WabacusClassLoader;

public class WabacusServlet extends HttpServlet implements ServletContextListener
{
    private static final long serialVersionUID=715456159702221404L;

    private static Log log=LogFactory.getLog(WabacusServlet.class);

    public void contextInitialized(ServletContextEvent event)
    {
        closeAllDatasources();
        Config.homeAbsPath=event.getServletContext().getRealPath("/");
        Config.homeAbsPath=FilePathAssistant.getInstance().standardFilePath(Config.homeAbsPath+"\\");
        /*try
        {
            Config.webroot=event.getServletContext().getContextPath();
            if(!Config.webroot.endsWith("/")) Config.webroot+="/";
        }catch(NoSuchMethodError e)
        {
            Config.webroot=null;
        }*/
        Config.webroot=null;
        Config.configpath=event.getServletContext().getInitParameter("configpath");
        if(Config.configpath==null||Config.configpath.trim().equals(""))
        {
            log.info("没有配置存放配置文件的根路径，将使用路径："+Config.homeAbsPath+"做为配置文件的根路径");
            Config.configpath=Config.homeAbsPath;
        }else
        {
            Config.configpath=WabacusAssistant.getInstance().parseConfigPathToRealPath(
                    Config.configpath,Config.homeAbsPath);
        }
        loadReportConfigFiles();
        FileUpDataImportThread.getInstance().start();
        TimingThread.getInstance().start();
    }
    
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
//        this.context=config.getServletContext();
//        closeAllDatasources();//关闭所有的连接池
//        Config.homeAbsPath=context.getRealPath("/");
//        Config.configpath=config.getInitParameter("configpath");//配置文件存放的物理路径
//            Config.configpath=Config.homeAbsPath;
//        }
    }

    public static void loadReportConfigFiles()
    {
        log.info("正在加载配置文件wabacus.cfg.xml及所有报表配置文件...");
        TimingThread.getInstance().reset();
        ConfigLoadManager.currentDynClassLoader=new WabacusClassLoader(Thread.currentThread().getContextClassLoader());
        int flag=ConfigLoadManager.loadAllReportSystemConfigs();
        if(flag==-1)
        {
            log.error("加载报表配置文件wabacus.cfg.xml失败");
        }else if(flag==0)
        {
            log.warn("报表配置文件wabacus.cfg.xml内容为空，或没有配置报表");
        }
    }

    public void service(HttpServletRequest request,HttpServletResponse response)
            throws ServletException,IOException
    {
        String contentType=request.getHeader("Content-type");
        if(contentType!=null&&contentType.startsWith("multipart/"))
        {
            WabacusFacade.uploadFile(request,response);
        }else
        {
            String action=Tools.getRequestValue(request,"ACTIONTYPE","");
            if(action.equalsIgnoreCase("updateconfig"))
            {
                loadReportConfigFiles();
                PrintWriter out=response.getWriter();
                out.println("完成配置文件更新");
            }else if(action.equalsIgnoreCase("invokeServerAction"))
            {
                String resultStr=WabacusFacade.invokeServerAction(request,response);
                if(resultStr!=null&&!resultStr.trim().equals(""))
                {
                    PrintWriter out=response.getWriter();
                    out.println(resultStr);
                }
            }else if(action.equalsIgnoreCase("ServerValidateOnBlur"))
            {//某个输入框onblur后进行服务器端校验
                String resultStr=WabacusFacade.doServerValidateOnBlur(request,response);
                if(resultStr!=null&&!resultStr.trim().equals(""))
                {
                    PrintWriter out=response.getWriter();
                    out.println(resultStr);
                }
            }else if(action.equalsIgnoreCase("download"))
            {
                WabacusFacade.downloadFile(request,response);
            }else if(action.equalsIgnoreCase("GetFilterDataList"))
            {
                response.reset();
                response.setContentType("text/xml;charset="+Config.encode);
                StringBuffer sbuffer=new StringBuffer("<?xml version=\"1.0\" encoding=\""+Config.encode+"\"?><items>");
                sbuffer.append(WabacusFacade.getFilterDataList(request,response));
                sbuffer.append("</items>");
                PrintWriter out=response.getWriter();
                out.println(sbuffer.toString().trim());
            }else if(action.equalsIgnoreCase("GetTypePromptDataList"))
            {
                response.reset();
                response.setContentType("text/xml;charset="+Config.encode);
                StringBuffer sbuffer=new StringBuffer("<?xml version=\"1.0\" encoding=\""+Config.encode+"\"?><items>");
                sbuffer.append(WabacusFacade.getTypePromptDataList(request,response));
                sbuffer.append("</items>");
                PrintWriter out=response.getWriter();
                out.println(sbuffer.toString().trim());
            }else if(action.equalsIgnoreCase("GetSelectBoxDataList"))
            {
                response.reset();
                response.setContentType("text/html;charset="+Config.encode);
                String resultStr=WabacusFacade.getSelectBoxDataList(request,response);
                PrintWriter out=response.getWriter();
                out.print(resultStr);
            }else if(action.equalsIgnoreCase(Consts.GETAUTOCOMPLETEDATA_ACTION))
            {//获取自动填充表单列的数据
                PrintWriter out=response.getWriter();
                out.print(WabacusFacade.getAutoCompleteColValues(request,response));
            }else if(action.equalsIgnoreCase("ShowUploadFilePage"))
            {
                PrintWriter out=response.getWriter();
                out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
                out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset="+Config.encode+"\">");
                WabacusFacade.showUploadFilePage(request,out);
            }else if(action.equalsIgnoreCase("getChartDataString"))
            {//获取图形报表数据的<chart/>数据部分
                response.reset();
                response.setContentType("text/xml;charset="+Config.encode);
                StringBuffer sbuffer=new StringBuffer("<?xml version=\"1.0\" encoding=\""+Config.encode+"\"?>");
                sbuffer.append(WabacusFacade.getChartDataString(request,response));
                PrintWriter out=response.getWriter();
                out.println(sbuffer.toString().trim());
            }else if(action.equalsIgnoreCase("loadChartXmlFile"))
            {
                response.reset();
                response.setContentType("text/xml;charset="+Config.encode);
                StringBuffer sbuffer=new StringBuffer("<?xml version=\"1.0\" encoding=\""+Config.encode+"\"?>");
                sbuffer.append(WabacusFacade.getChartDataStringFromLocalFile(request,response));
                PrintWriter out=response.getWriter();
                out.println(sbuffer.toString().trim());
            }else
            {
                int itype=Integer.parseInt(Tools.getRequestValue(request,Consts.DISPLAYTYPE_PARAMNAME,String.valueOf(Consts.DISPLAY_ON_PAGE)));
                if(itype==Consts.DISPLAY_ON_PRINT)
                {
                    WabacusFacade.printComponents(request,response);
                }else if(itype==Consts.DISPLAY_ON_PLAINEXCEL)
                {//下载纯数据
                    WabacusFacade.exportReportDataOnPlainExcel(request,response);
                }else if(itype==Consts.DISPLAY_ON_RICHEXCEL)
                {
                    WabacusFacade.exportReportDataOnWordRichexcel(request,response,Consts.DISPLAY_ON_RICHEXCEL);
                }else if(itype==Consts.DISPLAY_ON_WORD)
                {
                    WabacusFacade.exportReportDataOnWordRichexcel(request,response,Consts.DISPLAY_ON_WORD);
                }else if(itype==Consts.DISPLAY_ON_PDF)
                {
                    WabacusFacade.exportReportDataOnPDF(request,response,Consts.DISPLAY_ON_PDF);
                }else
                { 
                    WabacusFacade.displayReport(request,response);
                }
            }
        }
    }

    public void destroy()
    {
    }

    public void contextDestroyed(ServletContextEvent event)
    {
        closeAllDatasources();
        FileUpDataImportThread.getInstance().stopRunning();
        TimingThread.getInstance().stopRunning();
    }

    private void closeAllDatasources()
    {
        Map<String,AbsDataSource> mDataSourcesTmp=Config.getInstance().getMDataSources();
        if(mDataSourcesTmp!=null)
        {
            for(Entry<String,AbsDataSource> entry:mDataSourcesTmp.entrySet())
            {
                if(entry.getValue()!=null)
                    entry.getValue().closePool();
            }
        }
    }
}
