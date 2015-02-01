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
package com.wabacus.system.commoninterface;

import javax.servlet.http.HttpServletRequest;

import com.wabacus.config.component.container.page.PageBean;

public class DefaultPagePersonalizePersistenceImpl implements IPagePersonalizePersistence
{
    public String loadSkin(HttpServletRequest request,PageBean pbean)
    {
        if(request!=null&&request.getSession()!=null)
        {
            if(pbean!=null)
            {
                return (String)request.getSession().getAttribute("dynskin_"+pbean.getId());
            }else
            {
                return (String)request.getSession().getAttribute("dynskin");
            }
        }
        return null;
    }

    public void storeSkin(HttpServletRequest request,PageBean pbean,String skin)
    {
        if(request!=null&&request.getSession()!=null)
        {
            if(pbean!=null)
            {
                request.getSession().setAttribute("dynskin_"+pbean.getId(),skin);
            }else
            {
                request.getSession().setAttribute("dynskin",skin);
            }
        }
    }
}

