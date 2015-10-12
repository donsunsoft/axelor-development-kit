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
package com.axelor.mail.db.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.PersistenceException;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JpaRepository;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailFlags;
import com.axelor.mail.db.MailMessage;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaAttachment;
import com.axelor.meta.db.MetaFile;
import com.axelor.meta.db.repo.MetaAttachmentRepository;
import com.axelor.rpc.Resource;
import com.google.inject.persist.Transactional;

public class MailMessageRepository extends JpaRepository<MailMessage> {

	@Inject
	private MetaFiles files;

	@Inject
	private MetaAttachmentRepository attachmentRepo;

	public MailMessageRepository() {
		super(MailMessage.class);
	}

	public List<MailMessage> findRelated(Model entity, int limit, int offset) {
		return all().filter("self.relatedModel = ? AND self.relatedId = ?",
				entity.getClass().getName(),
				entity.getId())
				.order("-createdOn")
				.fetch(limit, offset);
	}

	public long countRelated(Model entity) {
		return all().filter("self.relatedModel = ? AND self.relatedId = ?",
				entity.getClass().getName(),
				entity.getId()).count();
	}

	@Override
	public void remove(MailMessage message) {
		// delete all attachments
		List<MetaAttachment> attachments = attachmentRepo.all()
				.filter("self.objectId = ? AND self.objectName = ?", message.getId(), MailMessage.class.getName())
				.fetch();

		for (MetaAttachment attachment : attachments) {
			try {
				files.delete(attachment);
			} catch (IOException e) {
				throw new PersistenceException(e);
			}
		}
		super.remove(message);
	}

	@Override
	public MailMessage save(MailMessage entity) {
		if (entity.getParent() == null && entity.getRelatedId() != null) {
			MailMessage parent = all()
				.filter("self.parent is null AND "
						+ "self.type = 'notification' AND "
						+ "self.relatedId = :id AND self.relatedModel = :model")
				.bind("id", entity.getRelatedId())
				.bind("model", entity.getRelatedModel())
				.order("id")
				.cacheable()
				.autoFlush(false)
				.fetchOne();
			entity.setParent(parent);
		}

		MailMessage root = entity.getRoot();
		if (root == null) {
			root = entity.getParent();
		}
		if (root != null && root.getRoot() != null) {
			root = root.getRoot();
		}
		entity.setRoot(root);

		// mark root as unread
		if (root != null && root.getFlags() != null) {
			for (MailFlags flag : root.getFlags()) {
				flag.setIsRead(false);
			}
		}

		return super.save(entity);
	}

	@Transactional
	public MailMessage post(Model entity, MailMessage message, List<MetaFile> files) {

		final Mapper mapper = Mapper.of(entity.getClass());
		final MailFlags  flags = new MailFlags();

		message.setRelatedId(entity.getId());
		message.setRelatedModel(entity.getClass().getName());
		message.setAuthor(AuthUtils.getUser());

		// mark message as read
		flags.setMessage(message);
		flags.setUser(AuthUtils.getUser());
		flags.setIsRead(Boolean.TRUE);
		message.addFlag(flags);

		try {
			message.setRelatedName(mapper.getNameField().get(entity).toString());
		} catch (Exception e) {
		}

		message = save(message);

		if (files == null || files.isEmpty()) {
			return message;
		}

		final MetaAttachmentRepository repo = Beans.get(MetaAttachmentRepository.class);

		for (MetaFile file : files) {

			MetaAttachment attachment = new MetaAttachment();

			attachment.setObjectId(message.getId());
			attachment.setObjectName(message.getClass().getName());
			attachment.setMetaFile(file);

			repo.save(attachment);
		}

		return message;
	}

	public Map<String, Object> details(MailMessage message) {
		final String[] fields = {
				"id", "version", "type", "author", "recipients",
				"subject", "body", "relatedId", "relatedModel", "relatedName"};
		final Map<String, Object> details = Resource.toMap(message, fields);
		final List<Object> files = new ArrayList<>();

		final MetaAttachmentRepository repoAttachments = Beans.get(MetaAttachmentRepository.class);
		final MailFlagsRepository repoFlags = Beans.get(MailFlagsRepository.class);

		final MailFlags flags = repoFlags.findBy(message, AuthUtils.getUser());
		final List<MetaAttachment> attachments = repoAttachments.all().filter(
				"self.objectId = ? AND self.objectName = ?",
				message.getId(),
				message.getClass().getName()).fetch();

		for (MetaAttachment attachment : attachments) {
			files.add(Resource.toMapCompact(attachment.getMetaFile()));
		}

		if (flags != null) {
			details.put("$flags", Resource.toMap(flags, "isRead", "isStarred", "isArchived"));
		}

		String eventType = message.getType();
		String eventText = I18n.get("updated document");
		if ("comment".equals(eventType)) {
			eventText = I18n.get("added comment");
			details.put("$canDelete", message.getCreatedBy() == AuthUtils.getUser());
		}

		String avatar = "img/user.png";
		User author = message.getAuthor();
		if (author != null && author.getImage() != null) {
			avatar = "ws/rest/" + User.class.getName() + "/" + author.getId() + "/image/download?image=true&v=" + author.getVersion();
		}

		details.put("$avatar", avatar);
		details.put("$files", files);
		details.put("$eventType", eventType);
		details.put("$eventText", eventText);
		details.put("$eventTime", message.getCreatedOn());

		return details;
	}
}
