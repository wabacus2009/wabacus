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
package com.wabacus.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

public class WabacusRuntimeException extends RuntimeException
{
    private static final long serialVersionUID=785785821021141714L;

    private Throwable mRootCause=null;

    public WabacusRuntimeException()
    {}

    public WabacusRuntimeException(String message)
    {
        super(message);

    }

    public WabacusRuntimeException(Throwable t)
    {
        super(t);
        mRootCause=t;
    }

    public WabacusRuntimeException(String mess,Throwable t)
    {
        super(mess,t);
        mRootCause=t;
    }

    public Throwable getRootCause()
    {
        return mRootCause;
    }

    public String getRootCauseMessage()
    {
        String rcmessage=null;
        if(getRootCause()!=null)
        {
            if(getRootCause().getCause()!=null)
            {
                rcmessage=getRootCause().getCause().getMessage();
            }
            rcmessage=(rcmessage==null)?getRootCause().getMessage():rcmessage;
            rcmessage=(rcmessage==null)?super.getMessage():rcmessage;
            rcmessage=(rcmessage==null)?"NONE":rcmessage;
        }
        return rcmessage;
    }

    public void printStackTrace()
    {
        super.printStackTrace();
        if(mRootCause!=null)
        {
            System.out.println("--- ROOT CAUSE ---");
            mRootCause.printStackTrace();
        }
    }

    public void printStackTrace(PrintStream s)
    {
        super.printStackTrace(s);
        if(mRootCause!=null)
        {
            s.println("--- ROOT CAUSE ---");
            mRootCause.printStackTrace(s);
        }
    }

    public void printStackTrace(PrintWriter s)
    {
        super.printStackTrace(s);
        if(null!=mRootCause)
        {
            s.println("--- ROOT CAUSE ---");
            mRootCause.printStackTrace(s);
        }
    }

}
