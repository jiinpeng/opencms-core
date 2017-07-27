/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH & Co. KG (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.contenteditor;

import org.opencms.acacia.shared.rpc.I_CmsSerialDateService;
import org.opencms.gwt.CmsGwtService;
import org.opencms.util.CmsPair;
import org.opencms.widgets.serialdate.CmsSerialDateBeanFactory;
import org.opencms.widgets.serialdate.I_CmsSerialDateBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.SortedSet;

/** Implementation of the serial date RPC service. */
public class CmsSerialDateService extends CmsGwtService implements I_CmsSerialDateService {

    /** Serialization id. */
    private static final long serialVersionUID = -5078405766510438917L;

    /**
     * @see org.opencms.acacia.shared.rpc.I_CmsSerialDateService#getDates(java.lang.String)
     */
    public Collection<CmsPair<Date, Boolean>> getDates(String config) {

        I_CmsSerialDateBean bean = CmsSerialDateBeanFactory.createSerialDateBean(config);
        Collection<Date> dates = bean.getDates();
        Collection<Date> exceptions = bean.getExceptions();
        Collection<CmsPair<Date, Boolean>> result = new ArrayList<>(dates.size() + exceptions.size());
        for (Date d : dates) {
            result.add(new CmsPair<Date, Boolean>(d, Boolean.TRUE));
        }
        for (Date d : exceptions) {
            result.add(new CmsPair<Date, Boolean>(d, Boolean.FALSE));
        }
        return result;
    }

    /**
     * @see org.opencms.acacia.shared.rpc.I_CmsSerialDateService#hasTooManyDates(java.lang.String)
     */
    public CmsPair<Boolean, Date> hasTooManyDates(String config) {

        I_CmsSerialDateBean bean = CmsSerialDateBeanFactory.createSerialDateBean(config);
        SortedSet<Date> dates = bean.getDates();
        Boolean hasTooManyDates = Boolean.valueOf(bean.hasTooManyDates());
        return new CmsPair<>(hasTooManyDates, dates.isEmpty() ? null : dates.last());

    }

}