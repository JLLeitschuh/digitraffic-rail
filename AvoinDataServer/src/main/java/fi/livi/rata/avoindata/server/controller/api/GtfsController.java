package fi.livi.rata.avoindata.server.controller.api;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fi.livi.rata.avoindata.common.dao.gtfs.GTFSRepository;
import fi.livi.rata.avoindata.common.domain.gtfs.GTFS;
import fi.livi.rata.avoindata.common.utils.DateProvider;
import fi.livi.rata.avoindata.server.config.WebConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "trains", description = "Returns trains as gtfs", position = Integer.MIN_VALUE)
@RestController
@RequestMapping(WebConfig.CONTEXT_PATH + "trains")
public class GtfsController {
    @Autowired
    private DateProvider dp;

    @Autowired
    private GTFSRepository gtfsRepository;

    @ApiOperation("Returns GTFS zip file")
    @RequestMapping(method = RequestMethod.GET, path = "gtfs-all.zip", produces = "application/zip")
    public byte[] getGtfsForAllTrains(HttpServletResponse response) {
        return getData(response, "gtfs-all.zip");
    }

    @ApiOperation("Returns GTFS zip file")
    @RequestMapping(method = RequestMethod.GET, path = "gtfs-passenger.zip", produces = "application/zip")
    public byte[] getGtfsForPassengerTrains(HttpServletResponse response) {
        return getData(response, "gtfs-passenger.zip");
    }

    @ApiIgnore
    @RequestMapping(method = RequestMethod.GET, path = "gtfs-vr-tre.zip", produces = "application/zip")
    public byte[] getGtfsForVRTRETrains(HttpServletResponse response) {
        return getData(response, "gtfs-vr-tre.zip");
    }

    @ApiIgnore
    @RequestMapping(method = RequestMethod.GET, path = "gtfs-vr.zip", produces = "application/zip")
    public byte[] getGtfsForVRTrains(HttpServletResponse response) {
        return getData(response, "gtfs-vr.zip");
    }

    private byte[] getData(HttpServletResponse response, String zipFileName) {
        GTFS gtfs = gtfsRepository.findFirstByFileNameOrderByIdDesc(zipFileName);
        response.addHeader("x-is-fresh", Boolean.toString(gtfs.created.isAfter(dp.nowInHelsinki().minusHours(25))));
        response.addHeader("x-timestamp", gtfs.created.toString());
        return gtfs.data;
    }
}
