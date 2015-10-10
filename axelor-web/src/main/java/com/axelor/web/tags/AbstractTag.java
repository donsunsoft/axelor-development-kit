/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.web.tags;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.axelor.app.AppSettings;

public abstract class AbstractTag extends SimpleTagSupport {

	private String src;

	private boolean production = AppSettings.get().isProduction();

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	private boolean exists(String path) {
		final PageContext ctx = (PageContext) getJspContext();
		try {
			return ctx.getServletContext().getResource(path) != null;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	protected List<String> getScripts() {
		return Arrays.asList(src);
	}

	protected abstract void doTag(String src) throws IOException;

	@Override
	public void doTag() throws JspException, IOException {

		if (production) {
			final String minified = src.replaceFirst("\\.(js|css)$", ".min.$1");
			if (exists(minified)) {
				doTag(minified);
				return;
			}
		}

		final List<String> scripts = getScripts();
		if (scripts == null || scripts.isEmpty()) {
			return;
		}

		for (String script : scripts) {
			doTag(script);
		}
	}
}
