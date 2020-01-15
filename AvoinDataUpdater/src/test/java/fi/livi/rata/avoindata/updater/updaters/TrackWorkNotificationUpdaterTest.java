package fi.livi.rata.avoindata.updater.updaters;

import fi.livi.rata.avoindata.common.dao.trackwork.TrackWorkNotificationIdAndVersion;
import fi.livi.rata.avoindata.common.dao.trackwork.TrackWorkNotificationRepository;
import fi.livi.rata.avoindata.common.domain.trackwork.TrackWorkNotification;
import fi.livi.rata.avoindata.updater.BaseTest;
import fi.livi.rata.avoindata.updater.factory.TrackWorkNotificationFactory;
import fi.livi.rata.avoindata.updater.service.ruma.LocalTrackWorkNotificationService;
import fi.livi.rata.avoindata.updater.service.ruma.RemoteTrackWorkNotificationService;
import fi.livi.rata.avoindata.updater.service.ruma.RemoteTrackWorkNotificationStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class TrackWorkNotificationUpdaterTest extends BaseTest {

    private TrackWorkNotificationUpdater updater;

    @Autowired
    private TrackWorkNotificationRepository repository;
    @Autowired
    private LocalTrackWorkNotificationService localTrackWorkNotificationService;
    @Autowired
    private TrackWorkNotificationFactory factory;
    @MockBean
    private RemoteTrackWorkNotificationService remoteTrackWorkNotificationService;

    @Before
    public void setUp() {
        updater = new TrackWorkNotificationUpdater(remoteTrackWorkNotificationService, localTrackWorkNotificationService);
    }

    @After
    public void tearDown() {
        testDataService.clearTrackWorkNotifications();
    }

    @Test
    @Transactional
    public void addNew() {
        TrackWorkNotification twn = factory.create(1).get(0);
        when(remoteTrackWorkNotificationService.getStatuses()).thenReturn(new RemoteTrackWorkNotificationStatus[]{new RemoteTrackWorkNotificationStatus(twn.id.id, twn.id.version)});
        when(remoteTrackWorkNotificationService.getTrackWorkNotificationVersions(anyLong(), any())).thenReturn(Collections.singletonList(twn));

        updater.update();

        assertEquals(twn.id, repository.getOne(twn.id).id);
    }

    @Test
    @Transactional
    public void updateExistingForwards() {
        // only persist version 1
        List<TrackWorkNotification> twnVersions = factory.create(2);
        TrackWorkNotification twn = twnVersions.get(0);
        TrackWorkNotification twnV2 = twnVersions.get(1);
        repository.save(twn);

        when(remoteTrackWorkNotificationService.getStatuses()).thenReturn(new RemoteTrackWorkNotificationStatus[]{new RemoteTrackWorkNotificationStatus(twn.id.id, twnV2.getVersion())});
        when(remoteTrackWorkNotificationService.getTrackWorkNotificationVersions(anyLong(), any())).thenReturn(Collections.singletonList(twnV2));

        updater.update();

        List<TrackWorkNotificationIdAndVersion> idsAndVersions = repository.findIdsAndVersions(Collections.singleton(twn.id.id));
        assertEquals(2, idsAndVersions.size());
        assertEquals( twn.id.version.longValue(), idsAndVersions.get(0).getVersion().longValue());
        assertEquals( twnV2.id.version.longValue(), idsAndVersions.get(1).getVersion().longValue());
    }

    @Test
    @Transactional
    public void updateExistingBackwards() {
        // only persist version 2
        List<TrackWorkNotification> twnVersions = factory.create(2);
        TrackWorkNotification twn = twnVersions.get(0);
        TrackWorkNotification twnV2 = twnVersions.get(1);
        repository.save(twnV2);

        when(remoteTrackWorkNotificationService.getStatuses()).thenReturn(new RemoteTrackWorkNotificationStatus[]{new RemoteTrackWorkNotificationStatus(twn.id.id, twnV2.getVersion())});
        when(remoteTrackWorkNotificationService.getTrackWorkNotificationVersions(anyLong(), any())).thenReturn(Collections.singletonList(twn));

        updater.update();

        List<TrackWorkNotificationIdAndVersion> idsAndVersions = repository.findIdsAndVersions(Collections.singleton(twn.id.id));
        assertEquals(2, idsAndVersions.size());
        assertEquals( twn.id.version.longValue(), idsAndVersions.get(0).getVersion().longValue());
        assertEquals( twnV2.id.version.longValue(), idsAndVersions.get(1).getVersion().longValue());
    }

}