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

package org.opencms.jsp.util;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.CmsJspTagEditable;
import org.opencms.jsp.Messages;
import org.opencms.main.CmsException;
import org.opencms.main.CmsRuntimeException;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.containerpage.CmsContainerElementBean;
import org.opencms.xml.containerpage.CmsFlexFormatterBean;
import org.opencms.xml.containerpage.CmsMacroFormatterBean;
import org.opencms.xml.containerpage.I_CmsFormatterBean;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.stringtemplate.v4.DateRenderer;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.compiler.CompiledST;
import org.stringtemplate.v4.compiler.FormalArgument;

/**
 * Renderer for string templates.<p>
 */
public class CmsStringTemplateRenderer {

    /** The current cms context. */
    private CmsObject m_cms;

    /** The page context. */
    private PageContext m_context;

    /** The JSP context bean. */
    private CmsJspStandardContextBean m_contextBean;

    /** The element to render. */
    private CmsContainerElementBean m_element;

    /** The request. */
    private HttpServletRequest m_request;

    /**
     * Constructor.<p>
     *
     * @param context the page context
     * @param req the request
     */
    public CmsStringTemplateRenderer(PageContext context, HttpServletRequest req) {
        m_context = context;
        m_request = req;
        CmsFlexController controller = CmsFlexController.getController(req);
        if (controller == null) {
            handleMissingFlexController();
            return;
        }
        m_cms = controller.getCmsObject();
        m_contextBean = CmsJspStandardContextBean.getInstance(m_request);
        m_element = m_contextBean.getElement();
    }

    /**
     * Renders the given string template.<p>
     *
     * @param cms the cms context
     * @param template the template
     * @param content the content
     * @param contextObjects additional context objects made available to the template
     *
     * @return the rendering result
     */
    public static String renderTemplate(
        CmsObject cms,
        String template,
        CmsJspContentAccessBean content,
        Map<String, Object> contextObjects) {

        STGroup group = new STGroup('%', '%');
        group.registerRenderer(Date.class, new DateRenderer());
        CompiledST cST = group.defineTemplate("main", template);
        cST.addArg(new FormalArgument("content"));
        if (contextObjects != null) {
            for (Entry<String, Object> entry : contextObjects.entrySet()) {
                cST.addArg(new FormalArgument(entry.getKey()));
            }
        }
        ST st = group.getInstanceOf("main");
        st.add("content", content);
        if (contextObjects != null) {
            for (Entry<String, Object> entry : contextObjects.entrySet()) {
                st.add(entry.getKey(), entry.getValue());
            }
        }
        return st.render(cms.getRequestContext().getLocale());
    }

    /**
     * Renders the given string template.<p>
     *
     * @param cms the cms context
     * @param template the template
     * @param content the content
     * @param contextObjects additional context objects made available to the template
     *
     * @return the rendering result
     */
    public static String renderTemplate(
        CmsObject cms,
        String template,
        CmsResource content,
        Map<String, Object> contextObjects) {

        return renderTemplate(cms, template, new CmsJspContentAccessBean(cms, content), contextObjects);
    }

    /**
     * Renders the requested element content with the flex formatter string template.<p>
     *
     * @throws IOException in case writing to to page context out fails
     */
    public void render() throws IOException {

        I_CmsFormatterBean formatterConfig = OpenCms.getADEManager().getCachedFormatters(
            m_cms.getRequestContext().getCurrentProject().isOnlineProject()).getFormatters().get(
                m_element.getFormatterId());
        if (formatterConfig instanceof CmsFlexFormatterBean) {
            CmsFlexFormatterBean config = (CmsFlexFormatterBean)formatterConfig;
            String template = config.getStringTemplate();
            if (m_element.isInMemoryOnly()) {
                if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(config.getPlaceholderStringTemplate())) {
                    template = config.getPlaceholderStringTemplate();
                }
                if (config.getDefaultContentStructureId() != null) {
                    try {
                        CmsResource defaultContent = m_cms.readResource(
                            ((CmsMacroFormatterBean)formatterConfig).getDefaultContentStructureId());
                        CmsFile defaultFile = m_cms.readFile(defaultContent);
                        m_element = new CmsContainerElementBean(
                            defaultFile,
                            m_element.getFormatterId(),
                            m_element.getIndividualSettings(),
                            true,
                            m_element.editorHash(),
                            m_element.isCreateNew());
                    } catch (CmsException e) {
                        //      LOG.error("Error reading default content for new resource", e);
                    }
                }
            }
            try {
                m_context.getOut().print(
                    renderTemplate(
                        m_cms,
                        template,
                        m_element.getResource(),
                        Collections.<String, Object> singletonMap("settings", m_element.getSettings())));
            } catch (Throwable t) {
                if (CmsJspTagEditable.isEditableRequest(m_request)) {

                    m_context.getOut().println("<h2 style=\"color:red\">ERROR: " + t.getLocalizedMessage() + "</h2>");
                }
            }
        }
    }

    /**
     * This method is called when the flex controller can not be found during initialization.<p>
     *
     * Override this if you are reusing old workplace classes in a context where no flex controller is available.
     */
    private void handleMissingFlexController() {

        // controller not found - this request was not initialized properly
        throw new CmsRuntimeException(
            Messages.get().container(Messages.ERR_MISSING_CMS_CONTROLLER_1, CmsMacroFormatterResolver.class.getName()));
    }

}