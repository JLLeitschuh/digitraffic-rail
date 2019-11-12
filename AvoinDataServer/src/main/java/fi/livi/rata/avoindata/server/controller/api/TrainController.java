package fi.livi.rata.avoindata.server.controller.api;

import java.math.BigInteger;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;
import fi.livi.rata.avoindata.common.dao.train.AllTrainsRepository;
import fi.livi.rata.avoindata.common.dao.train.TrainRepository;
import fi.livi.rata.avoindata.common.dao.train.TrainStreamRepository;
import fi.livi.rata.avoindata.common.domain.common.TrainId;
import fi.livi.rata.avoindata.common.domain.jsonview.TrainJsonView;
import fi.livi.rata.avoindata.common.domain.train.Train;
import fi.livi.rata.avoindata.common.utils.BatchExecutionService;
import fi.livi.rata.avoindata.server.config.CacheConfig;
import fi.livi.rata.avoindata.server.config.WebConfig;
import fi.livi.rata.avoindata.server.controller.utils.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "trains", description = "Returns trains", position = Integer.MIN_VALUE)
@RestController
@RequestMapping(WebConfig.CONTEXT_PATH + "trains")
public class TrainController extends ADataController {
    public static int MAX_ANNOUNCED_TRAINS = 2500;
    @Autowired
    private TrainRepository trainRepository;
    @Autowired
    private AllTrainsRepository allTrainsRepository;
    @Autowired
    private BatchExecutionService bes;
    @Autowired
    private TrainStreamRepository trainStreamRepository;

    private Logger log = LoggerFactory.getLogger(TrainController.class);

    private CacheControl forAllLiveTrains = CacheConfig.LIVE_TRAIN_ALL_TRAINS_CACHECONTROL;
    private CacheControl forSingleLiveTrains = CacheConfig.LIVE_TRAIN_SINGLE_TRAIN_CACHECONTROL;

    @ApiOperation("Returns trains that are newer than {version}")
    @JsonView(TrainJsonView.LiveTrains.class)
    @RequestMapping(method = RequestMethod.GET, path = "")
    @Transactional(readOnly = true)
    public List<Train> getTrainsByVersion(@RequestParam(required = false) Long version, HttpServletResponse response) {
        log.info("HTTP-parametri {}", version);
        if (version == null) {
            version = allTrainsRepository.getMaxVersion() - 1;
            log.info("Haettu kannasta versio {}", version);
        }

        List<Object[]> rawIds = allTrainsRepository.findByVersionGreaterThanRawSql(version, MAX_ANNOUNCED_TRAINS);
        log.info("Idt palautettu {}", rawIds.stream().map(s -> String.format("%s: %s (%s)", s[0], s[1], s[2])).collect(Collectors.toList()));

        List<TrainId> trainIds = rawIds.stream().map(s -> {
            long trainNumber = ((BigInteger) s[0]).longValue();
            LocalDate departureDate = ((java.sql.Date) s[1]).toLocalDate();

            return new TrainId(trainNumber, departureDate);
        }).collect(Collectors.toList());

        final List<Train> trains = new LinkedList<>();
        if (!trainIds.isEmpty()) {
            bes.consume(trainIds, t -> trains.addAll(allTrainsRepository.findTrains(t)));
        }

        log.info("Junat haettu {}", trains);

        forAllLiveTrains.setCacheParameter(response, trains, version);

        return trains;
    }

    @ApiOperation("Returns latest train")
    @JsonView(TrainJsonView.LiveTrains.class)
    @RequestMapping(value = "/latest/{train_number}", method = RequestMethod.GET)
    public List<Train> getTrainByTrainNumber(@PathVariable final long train_number,
                                             @RequestParam(required = false, defaultValue = "0") long version, HttpServletResponse response) {
        return this.getTrainByTrainNumberAndDepartureDate(train_number, null, version, response);
    }

    @ApiOperation("Returns a specific train")
    @JsonView(TrainJsonView.LiveTrains.class)
    @RequestMapping(value = "/{departure_date}/{train_number}", method = RequestMethod.GET)
    public List<Train> getTrainByTrainNumberAndDepartureDate(@PathVariable final long train_number,
                                                             @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departure_date,
                                                             @RequestParam(required = false, defaultValue = "0") long version, HttpServletResponse response) {

        List<Train> trains = new ArrayList<>();

        if (departure_date == null) {
            trains = getTrainWithoutDepartureDate(train_number, version);
        } else {
            final Train train = trainRepository.findByDepartureDateAndTrainNumber(departure_date, train_number);
            if (train != null) {
                trains = Arrays.asList(train);
            }
        }

        forSingleLiveTrains.setCacheParameter(response, trains, version);

        return trains;
    }

    @ApiOperation(value = "Returns trains run on {departure_date}", response = Train.class, responseContainer = "List")
    @JsonView(TrainJsonView.LiveTrains.class)
    @RequestMapping(method = RequestMethod.GET, path = "/{departure_date}")
    public Stream<Train> getTrainsByDepartureDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate departure_date, HttpServletResponse response) {//
        CacheControl.addHistoryCacheParametersForDailyResult(departure_date, response);

        return trainStreamRepository.getTrainsByDepartureDate(departure_date);
    }

    private List<Train> getTrainWithoutDepartureDate(long train_number, long version) {
        final List<Object[]> liveTrains = trainRepository.findLiveTrainByTrainNumber(train_number);
        List<TrainId> trainsToRetrieve = extractNewerTrainIds(version, liveTrains);

        if (!trainsToRetrieve.isEmpty()) {
            return trainRepository.findTrains(trainsToRetrieve);
        }

        return Collections.EMPTY_LIST;
    }

    private List<TrainId> extractNewerTrainIds(long version, List<Object[]> liveTrains) {
        return liveTrains.stream().filter(train -> ((BigInteger) train[3]).longValue() > version).map(tuple -> {
            LocalDate departureDate = LocalDate.from(((Date) tuple[1]).toLocalDate());
            BigInteger trainNumber = (BigInteger) tuple[2];
            return new TrainId(trainNumber.longValue(), departureDate);
        }).collect(Collectors.toList());
    }
}
