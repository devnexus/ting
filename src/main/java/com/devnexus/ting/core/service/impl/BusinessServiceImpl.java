/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devnexus.ting.core.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.devnexus.ting.common.CalendarUtils;
import com.devnexus.ting.common.SystemInformationUtils;
import com.devnexus.ting.core.dao.ApplicationCacheDao;
import com.devnexus.ting.core.dao.CfpSubmissionDao;
import com.devnexus.ting.core.dao.EvaluationDao;
import com.devnexus.ting.core.dao.EventDao;
import com.devnexus.ting.core.dao.OrganizerDao;
import com.devnexus.ting.core.dao.PresentationDao;
import com.devnexus.ting.core.dao.PresentationTagDao;
import com.devnexus.ting.core.dao.RoomDao;
import com.devnexus.ting.core.dao.ScheduleItemDao;
import com.devnexus.ting.core.dao.SpeakerDao;
import com.devnexus.ting.core.dao.SponsorDao;
import com.devnexus.ting.core.dao.TrackDao;
import com.devnexus.ting.core.model.ApplicationCache;
import com.devnexus.ting.core.model.CfpSubmission;
import com.devnexus.ting.core.model.Evaluation;
import com.devnexus.ting.core.model.Event;
import com.devnexus.ting.core.model.FileData;
import com.devnexus.ting.core.model.Organizer;
import com.devnexus.ting.core.model.Presentation;
import com.devnexus.ting.core.model.PresentationTag;
import com.devnexus.ting.core.model.Room;
import com.devnexus.ting.core.model.ScheduleItem;
import com.devnexus.ting.core.model.ScheduleItemList;
import com.devnexus.ting.core.model.ScheduleItemType;
import com.devnexus.ting.core.model.Speaker;
import com.devnexus.ting.core.model.Sponsor;
import com.devnexus.ting.core.model.SponsorLevel;
import com.devnexus.ting.core.model.SponsorList;
import com.devnexus.ting.core.model.Track;
import com.devnexus.ting.core.model.support.PresentationSearchQuery;
import com.devnexus.ting.core.service.BusinessService;

/**
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
@Service("businessService")
public class BusinessServiceImpl implements BusinessService {

	/**
	 *   Initialize Logging.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(BusinessServiceImpl.class);

	@Autowired private CfpSubmissionDao cfpSubmissionDao;
	@Autowired private EvaluationDao   evaluationDao;
	@Autowired private EventDao        eventDao;
	@Autowired private OrganizerDao    organizerDao;
	@Autowired private PresentationDao presentationDao;
	@Autowired private PresentationTagDao presentationTagDao;
	@Autowired private RoomDao         roomDao;
	@Autowired private ScheduleItemDao scheduleItemDao;
	@Autowired private SpeakerDao      speakerDao;
	@Autowired private SponsorDao      sponsorDao;
	@Autowired private TrackDao        trackDao;
	@Autowired private ApplicationCacheDao applicationCacheDao;
	@Autowired private Environment environment;

	@Autowired private MessageChannel mailChannel;

	private final TransactionTemplate transactionTemplate;

	@Autowired
	public BusinessServiceImpl(PlatformTransactionManager transactionManager) {
		super();
		Assert.notNull(transactionManager, "The 'transactionManager' argument must not be null.");
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void deleteEvent(Event event) {
		Assert.notNull(event, "The provided event must not be null.");
		Assert.notNull(event.getId(), "Id must not be Null for event " + event);

		LOGGER.debug("Deleting Event {}", event);
		eventDao.remove(event);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void deleteOrganizer(Organizer organizerFromDb) {

		Assert.notNull(organizerFromDb,         "The provided organizer must not be null.");
		Assert.notNull(organizerFromDb.getId(), "Id must not be Null for organizer " + organizerFromDb);

		LOGGER.debug("Deleting Organizer {}", organizerFromDb);
		organizerDao.remove(organizerFromDb);
	}

	@CacheEvict(value="sponsors", allEntries=true)
	@Override
	@Transactional
	public void deleteSponsor(Sponsor sponsorFromDb) {
		Assert.notNull(sponsorFromDb,         "The provided sponsor must not be null.");
		Assert.notNull(sponsorFromDb.getId(), "Id must not be Null for sponsor " + sponsorFromDb);

		LOGGER.debug("Deleting Sponsor {}", sponsorFromDb);
		sponsorDao.remove(sponsorFromDb);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void deletePresentation(Presentation presentation) {

		Assert.notNull(presentation,         "The provided presentation must not be null.");
		Assert.notNull(presentation.getId(), "Id must not be Null for presentation " + presentation);

		LOGGER.debug("Deleting Presentation {}", presentation);

		presentationDao.remove(presentation);

	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void deleteSpeaker(Speaker speaker) {

		Assert.notNull(speaker,         "The provided speaker must not be null.");
		Assert.notNull(speaker.getId(), "Id must not be Null for speaker " + speaker);

		LOGGER.debug("Deleting Speaker {}", speaker);

		speakerDao.remove(speaker);
	}

	/** {@inheritDoc} */
	@Override
	public List<Event> getAllEventsOrderedByName() {
		return eventDao.getAllEventsOrderedByName();
	}

	/** {@inheritDoc} */
	@Override
	public List<Event> getAllNonCurrentEvents() {
		return eventDao.getAllNonCurrentEvents();
	}

	/** {@inheritDoc} */
	@Override
	public List<Organizer> getAllOrganizers() {
		return organizerDao.getAllOrganizers();
	}

	/** {@inheritDoc} */
	@Override
	public List<Presentation> getAllPresentations() {
		return presentationDao.getAll();
	}

	/** {@inheritDoc} */
	@Override
	public List<Speaker> getAllSpeakersOrderedByName() {
		return speakerDao.getAllSpeakersOrderedByName();
	}

	/** {@inheritDoc} */
	@Override
	public Event getEvent(Long id) {
		return eventDao.get(id);
	}

	/** {@inheritDoc} */
	@Override
	public Event getEventByEventKey(String eventKey) {
		return eventDao.getByEventKey(eventKey);
	}

	/** {@inheritDoc} */
	@Override
	public Organizer getOrganizer(final Long organizerId) {
		return organizerDao.get(organizerId);
	}

	@Override
	public Sponsor getSponsor(Long sponsorId) {
		return sponsorDao.get(sponsorId);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public Organizer getOrganizerWithPicture(Long organizerId) {
		return organizerDao.getOrganizerWithPicture(organizerId);
	}

	@Override
	public Sponsor getSponsorWithPicture(final Long sponsorId) {

		final Sponsor sponsor = transactionTemplate.execute(new TransactionCallback<Sponsor>() {
			public Sponsor doInTransaction(TransactionStatus status) {
				return sponsorDao.getSponsorWithPicture(sponsorId);
			}
		});

		return sponsor;
	}

	@Override
	@Transactional
	public List<Organizer> getAllOrganizersWithPicture() {
		return organizerDao.getOrganizersWithPicture();
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly=false)
	public Presentation getPresentation(Long id) {
		return presentationDao.get(id);
	}

	/** {@inheritDoc} */
	@Override
	public List<Presentation> getPresentationsForCurrentEvent() {
		List<Presentation> list = presentationDao.getPresentationsForCurrentEvent();
		Collections.sort(list);
		return list;
	}

	/** {@inheritDoc} */
	@Override
	public List<Presentation> getPresentationsForEventOrderedByName(Long eventId) {
		List<Presentation> list = presentationDao.getPresentationsForEventOrderedByName(eventId);
		return list;
	}

	@Override
	public List<Presentation> getPresentationsForEventOrderedByTrack(Long eventId) {
		List<Presentation> list = presentationDao.getPresentationsForEventOrderedByTrack(eventId);
		return list;
	}

	@Override
	public List<Presentation> getPresentationsForEventOrderedByRoom(Long eventId) {
		List<Presentation> list = presentationDao.getPresentationsForEventOrderedByRoom(eventId);
		return list;
	}

	/** {@inheritDoc} */
	@Override
	public Speaker getSpeaker(Long speakerId) {
		return speakerDao.get(speakerId);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional(readOnly=false)
	public byte[] getSpeakerImage(Long speakerId) {

		Assert.notNull(speakerId, "SpeakerId must not be null.");

		final Speaker speaker = getSpeaker(speakerId);

		final byte[] speakerPicture;

		if (speaker==null || speaker.getPicture() == null) {
			speakerPicture = SystemInformationUtils.getSpeakerImage(null);
		} else {
			speakerPicture = speaker.getPicture().getFileData();
		}

		return speakerPicture;

	}

	/** {@inheritDoc} */
	@Override
	public List<Speaker> getSpeakersForCurrentEvent() {
		return speakerDao.getSpeakersForCurrentEvent();
	}

	/** {@inheritDoc} */
	@Override
	public List<Speaker> getSpeakersForEvent(Long eventId) {
		return speakerDao.getSpeakersForEvent(eventId);
	}

	@Override
	public List<Room> getRoomsForEvent(Long eventId) {
		return roomDao.getRoomsForEvent(eventId);
	}

	@Override
	public List<Track> getTracksForEvent(Long eventId) {
		return trackDao.getTracksForEvent(eventId);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public void saveEvent(Event event) {
		eventDao.save(event);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public Organizer saveOrganizer(Organizer organizer) {
		return organizerDao.save(organizer);
	}

	@Override
	@Transactional
	@CacheEvict(value="sponsors", allEntries=true)
	public Sponsor saveSponsor(Sponsor sponsor) {
		return sponsorDao.save(sponsor);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public Presentation savePresentation(Presentation presentation) {
		return presentationDao.save(presentation);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public Speaker saveSpeaker(Speaker speaker) {
		return speakerDao.save(speaker);
	}

	/** {@inheritDoc} */
	@Override
	@Transactional
	public ApplicationCache updateApplicationCacheManifest() {

		final List<ApplicationCache> applicationCacheList = applicationCacheDao.getAll();

		if (applicationCacheList.isEmpty()) {
			ApplicationCache applicationCache = new ApplicationCache();
			applicationCache.setUpdatedDate(new Date());
			applicationCache.setUuid(UUID.randomUUID().toString());
			ApplicationCache savedApplicationCache = applicationCacheDao.save(applicationCache);
			return savedApplicationCache;
		} else if (applicationCacheList.size() >1) {
			throw new IllegalStateException("ApplicationCacheList should only contain 1 elements but found " + applicationCacheList.size());
		} else {
			ApplicationCache applicationCache = applicationCacheList.iterator().next();
			applicationCache.setUpdatedDate(new Date());
			applicationCache.setUuid(UUID.randomUUID().toString());
			return applicationCacheDao.save(applicationCache);
		}

	}

	@Override
	@Transactional
	public ApplicationCache getApplicationCacheManifest() {
		final List<ApplicationCache> applicationCacheList = applicationCacheDao.getAll();

		if (applicationCacheList.isEmpty()) {
			ApplicationCache applicationCache = new ApplicationCache();
			applicationCache.setUpdatedDate(new Date());
			applicationCache.setUuid(UUID.randomUUID().toString());
			ApplicationCache savedApplicationCache = applicationCacheDao.save(applicationCache);
			return savedApplicationCache;
		} else if (applicationCacheList.size() >1) {
			throw new IllegalStateException("ApplicationCacheList should only contain 1 elements but found " + applicationCacheList.size());
		} else {
			return applicationCacheList.iterator().next();
		}
	}

	@Override
	@Transactional
	public FileData getPresentationFileData(Long presentationId) {

		final Presentation presentation = this.getPresentation(presentationId);

		if (presentation == null) {
			return null;
		}

		FileData fileData = presentation.getPresentationFile();
		return fileData;
	}

	@Override
	public Event getCurrentEvent() {
		environment.getActiveProfiles();
		environment.getProperty("database.jdbc.url");
		return eventDao.getCurrentEvent();
	}

	@Override
	public Room getRoom(Long id) {
		return roomDao.get(id);
	}

	@Override
	public ScheduleItemList getScheduleForEvent(Long eventId) {

		final List<ScheduleItem> scheduleItems = scheduleItemDao.getScheduleForEvent(eventId);

		final ScheduleItemList scheduleItemList = new ScheduleItemList();
		scheduleItemList.setScheduleItems(scheduleItems);

		ScheduleItem currentScheduleItem = null;

		String hourOfDay = null;

		final SortedSet<Date> days = new TreeSet<Date>();

		int numberOfSessions = 0;
		int numberOfKeynoteSessions = 0;
		int numberOfBreakoutSessions = 0;
		int numberOfUnassignedSessions = 0;

		int numberOfBreaks = 0;

		Set<Long> speakerIds = new HashSet<Long>();
		Set<Long> roomIds = new HashSet<Long>();

		for (ScheduleItem scheduleItem : scheduleItems) {

			roomIds.add(scheduleItem.getRoom().getId());

			final Date fromTime = scheduleItem.getFromTime();
			final Date dayOfConference = CalendarUtils.getCalendarWithoutTime(fromTime).getTime();
			days.add(dayOfConference);

			if (ScheduleItemType.KEYNOTE.equals(scheduleItem.getScheduleItemType())
					|| ScheduleItemType.SESSION.equals(scheduleItem.getScheduleItemType())) {

				numberOfSessions++;

				if (scheduleItem.getPresentation() != null) {
					for (Speaker speaker : scheduleItem.getPresentation().getSpeakers()) {
						speakerIds.add(speaker.getId());
					}
				} else {
					numberOfUnassignedSessions++;
				}

				if (ScheduleItemType.KEYNOTE.equals(scheduleItem.getScheduleItemType())) {
					numberOfKeynoteSessions++;

				}

				if (ScheduleItemType.SESSION.equals(scheduleItem.getScheduleItemType())) {
					numberOfBreakoutSessions++;
				}

			}

			if (ScheduleItemType.BREAK.equals(scheduleItem.getScheduleItemType())) {
				numberOfBreaks++;
			}

			Calendar cal = Calendar.getInstance();
			cal.setTime(fromTime);

			String loopHour = cal.get(Calendar.HOUR_OF_DAY) + "_" + cal.get(Calendar.MINUTE);

			if (hourOfDay == null || !hourOfDay.equals(loopHour)) {
				currentScheduleItem = scheduleItem;
				hourOfDay = loopHour;
			} else {
				currentScheduleItem.setRowspan(currentScheduleItem.getRowspan() + 1);
			}

		}

		scheduleItemList.setDays(days);
		scheduleItemList.setNumberOfBreakoutSessions(numberOfBreakoutSessions);
		scheduleItemList.setNumberOfBreaks(numberOfBreaks);
		scheduleItemList.setNumberOfSessions(numberOfSessions);
		scheduleItemList.setNumberOfKeynoteSessions(numberOfKeynoteSessions);
		scheduleItemList.setNumberOfUnassignedSessions(numberOfUnassignedSessions);
		scheduleItemList.setNumberOfSpeakersAssigned(speakerIds.size());
		scheduleItemList.setNumberOfRooms(roomIds.size());
		return scheduleItemList;
	}

	@Override
	@Transactional
	public Evaluation saveEvaluation(Evaluation evaluation) {
		return evaluationDao.save(evaluation);
	}

	@Override
	public List<Evaluation> getEvaluationsForCurrentEvent() {
		return evaluationDao.getEvaluationsForCurrentEvent();
	}

	@Override
	public List<Evaluation> getEvaluationsForEvent(Long eventId) {
		return evaluationDao.getEvaluationsForEvent(eventId);
	}

	@Override
	public void removeEvaluation(Long evaluationId) {
		evaluationDao.remove(evaluationId);
	}

	@Override
	public List<CfpSubmission> getCfpSubmissions(Long eventId) {
		return cfpSubmissionDao.getCfpSubmissions(eventId);
	}

	@Override
	@Transactional
	public CfpSubmission saveCfpSubmission(CfpSubmission cfpSubmission) {
		return cfpSubmissionDao.save(cfpSubmission);
	}

	@Override
	public CfpSubmission saveAndNotifyCfpSubmission(final CfpSubmission cfpSubmission) {
		final BusinessService businessService = this;
		final CfpSubmission savedCfpSubmission = transactionTemplate.execute(new TransactionCallback<CfpSubmission>() {
			public CfpSubmission doInTransaction(TransactionStatus status) {
				return businessService.saveCfpSubmission(cfpSubmission);
			}
		});

		final String mailEnabled = environment.getProperty("mail.enabled");

		if (Boolean.valueOf(mailEnabled)) {
			mailChannel.send(MessageBuilder.withPayload(cfpSubmission).build());
		}

		return savedCfpSubmission;
	}

	@Override
	public CfpSubmission getCfpSubmission(Long cfpId) {
		return this.cfpSubmissionDao.get(cfpId);
	}

	@Override
	public Track getTrack(Long id) {
		return trackDao.get(id);
	}

	@Override
	public PresentationTag getPresentationTag(String tagName) {
		return presentationTagDao.getPresentationTag(tagName);
	}

	@Override
	public PresentationTag savePresentationTag(PresentationTag presentationTag) {
		return presentationTagDao.save(presentationTag);
	}

	@Override
	public Map<PresentationTag, Long> getTagCloud(Long eventId) {
		return presentationTagDao.getPresentationTagCountForEvent(eventId);
	}

	@Override
	public List<Presentation> findPresentations(
			PresentationSearchQuery presentationSearchQuery) {
		return presentationDao.findPresentations(presentationSearchQuery);
	}

	@Transactional
	@Override
	public void deleteCfpSubmission(Long id) {
		cfpSubmissionDao.remove(id);
	}

	@Transactional
	@Override
	public Set<PresentationTag> processPresentationTags(String tagsAsText) {

		final Set<PresentationTag> presentationTagsToSave = new HashSet<>();

		if (!tagsAsText.trim().isEmpty()) {
			Set<String> tags = StringUtils.commaDelimitedListToSet(tagsAsText);

			for (String tag : tags) {
				if (tag != null) {

					final String massagedTagName = tag.trim().toLowerCase(Locale.ENGLISH);
					PresentationTag tagFromDb = this.getPresentationTag(massagedTagName);

					if (tagFromDb == null) {
						PresentationTag presentationTag = new PresentationTag();
						presentationTag.setName(massagedTagName);
						tagFromDb = this.savePresentationTag(presentationTag);
					}

					presentationTagsToSave.add(tagFromDb);
				}
			}
		}

		return presentationTagsToSave;
	}

	@Override
	public List<Sponsor> getSponsorsForEvent(Long id) {
		return sponsorDao.getSponsorsForEvent(id);
	}

	@Cacheable("sponsors")
	@Override
	public SponsorList getSponsorListForEvent(Long id) {

		final List<Sponsor> sponsors = this.getSponsorsForEvent(id);

		final SponsorList sponsorList = new SponsorList();

		for (Sponsor sponsor : sponsors) {

			FileData imageData = this.getSponsorWithPicture(sponsor.getId()).getLogo();

			final int size;

			if (SponsorLevel.PLATINUM.equals(sponsor.getSponsorLevel())) {
				size = 180;
			}
			else if (SponsorLevel.GOLD.equals(sponsor.getSponsorLevel())) {
				size = 140;
			}
			else if (SponsorLevel.SILVER.equals(sponsor.getSponsorLevel())) {
				size = 110;
			}
			else if (SponsorLevel.COCKTAIL_HOUR.equals(sponsor.getSponsorLevel())) {
				size = 180;
			}
			else if (SponsorLevel.MEDIA_PARTNER.equals(sponsor.getSponsorLevel())) {
				size = 460;
			}
			else {
				throw new IllegalStateException("Unsupported SponsorLevel " + sponsor.getSponsorLevel());
			}

			if (imageData != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(imageData.getFileData());
				BufferedImage image;
				try {
					image = ImageIO.read(bais);
					final BufferedImage scaled = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, size);
					final ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageIO.write(scaled, "PNG", out);

					byte[] bytes = out.toByteArray();

					final String base64bytes = Base64.encodeBase64String(bytes);
					final String src = "data:image/png;base64," + base64bytes;
					sponsorList.addSponsor(sponsor, src);
				} catch (IOException e) {
					LOGGER.error("Error while processing logo for sponsor " + sponsor.getName(), e);
				}
			}
		}

		return sponsorList;
	}

}
