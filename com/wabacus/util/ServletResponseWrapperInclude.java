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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.jsp.JspWriter;

public class ServletResponseWrapperInclude extends HttpServletResponseWrapper
{
    private PrintWriter printWriter;

    private JspWriter jspWriter;

    public ServletResponseWrapperInclude(ServletResponse response,PrintWriter out)
    {
        super((HttpServletResponse)response);
        this.printWriter=out;
    }

    public ServletResponseWrapperInclude(ServletResponse response,JspWriter jspWriter)
    {
        super((HttpServletResponse)response);
        this.printWriter=new PrintWriter(jspWriter);
        this.jspWriter=jspWriter;
    }

    public PrintWriter getWriter() throws IOException
    {
        return this.printWriter;
    }

    public ServletOutputStream getOutputStream() throws IOException
    {
        throw new IllegalStateException();
    }

    public void resetBuffer()
    {
        try
        {
            this.jspWriter.clearBuffer();
        }catch(IOException ioe)
        {
        }
    }
}
