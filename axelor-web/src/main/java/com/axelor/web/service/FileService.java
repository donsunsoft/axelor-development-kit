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
package com.axelor.web.service;

import java.io.File;
import java.io.FileNotFoundException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.axelor.common.FileUtils;
import com.axelor.meta.schema.actions.ActionExport;
import com.axelor.meta.schema.actions.ActionReport;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
@Path("/files")
public class FileService extends AbstractService {

	@GET
	@Path("data-export/{name:.*}")
	public javax.ws.rs.core.Response exportFile(@PathParam("name") final String name) {
		final File file = FileUtils.getFile(ActionExport.getExportPath(), name);
		if (!file.isFile()) {
			throw new IllegalArgumentException(new FileNotFoundException(name));
		}
		return javax.ws.rs.core.Response.ok(file, MediaType.APPLICATION_OCTET_STREAM_TYPE)
				.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
				.header("Content-Transfer-Encoding", "binary")
				.build();
	}

	@GET
	@Path("report/{name:.*}")
	public javax.ws.rs.core.Response reportFile(@PathParam("name") final String name) {
		final File file = FileUtils.getFile(ActionReport.getOutputPath(), name);
		if (!file.isFile()) {
			throw new IllegalArgumentException(new FileNotFoundException(name));
		}

		MediaType type = MediaType.APPLICATION_OCTET_STREAM_TYPE;
		if (name.endsWith("pdf")) type = new MediaType("application", "pdf");
		if (name.endsWith("html")) type = new MediaType("text", "html");

		ResponseBuilder builder = javax.ws.rs.core.Response.ok(file, type);
		if (type != MediaType.APPLICATION_OCTET_STREAM_TYPE) {
			return builder.build();
		}

		return builder
				.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
				.header("Content-Transfer-Encoding", "binary")
				.build();
	}
}
