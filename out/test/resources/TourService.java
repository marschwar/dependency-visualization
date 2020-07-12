package de.hermes.delta.service;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import de.hermes.delta.DeltaApp;
import de.hermes.delta.R;
import de.hermes.delta.command.UpdateSafeplaceAuthorizationResultCommand;
import de.hermes.delta.comparator.StopComparatorByAllProperties;
import de.hermes.delta.dao.TourInfoDao;
import de.hermes.delta.data.DataHolder;
import de.hermes.delta.data.SafeplaceAuthorizationWrapper;
import de.hermes.delta.domain.AgentBarcode;
import de.hermes.delta.domain.Barcode;
import de.hermes.delta.domain.PostProcessingBo;
import de.hermes.delta.domain.ResultBo;
import de.hermes.delta.domain.SafeplaceAuthorizationServiceBo;
import de.hermes.delta.domain.ShipmentBarcode;
import de.hermes.delta.domain.ShipmentBo;
import de.hermes.delta.domain.SiteBarcode;
import de.hermes.delta.domain.StopBo;
import de.hermes.delta.domain.TimeframeServiceBo;
import de.hermes.delta.domain.TourBarcode;
import de.hermes.delta.domain.TourDate;
import de.hermes.delta.domain.TourIdentifier;
import de.hermes.delta.domain.TourInfoBo;
import de.hermes.delta.domain.VehicleBo;
import de.hermes.delta.dto.ErrorResponseDto;
import de.hermes.delta.dto.StopDto;
import de.hermes.delta.dto.TourResponseDto;
import de.hermes.delta.enumeration.DemoTourBarcode;
import de.hermes.delta.enumeration.HadvErrorCode;
import de.hermes.delta.enumeration.JobType;
import de.hermes.delta.enumeration.PostProcessingReason;
import de.hermes.delta.enumeration.ReasonOfReturn;
import de.hermes.delta.enumeration.SafeplaceAuthorizationResultType;
import de.hermes.delta.event.TourLoadErrorEvent;
import de.hermes.delta.event.TourLoadProgressEvent;
import de.hermes.delta.event.TourServiceTourLoadedEvent;
import de.hermes.delta.rest.TourApiClientHelper;
import de.hermes.delta.rest.TourApiService;
import de.hermes.delta.rest.TourModelMapper;
import de.hermes.delta.types.HttpStatusCode;
import de.hermes.delta.util.BarcodeUtil;
import de.hermes.delta.util.CollectionUtil;
import de.hermes.delta.util.Constants;
import de.hermes.delta.util.JobUtil;
import de.hermes.delta.util.TraceUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TourService {

    private static final String TAG = TourService.class.getSimpleName();
    private static int registerOfflineRetryDelayInSeconds = 5 * 60; // default is 5 minutes

    private final FileHandlingService fileHandlingService;
    private final TimeService timeService;
    private final TourInfoDao tourInfoDao;

    private DataHolder dataHolder;
    private TourApiService tourApiService;
    private UpdateTransactionService updateTransactionService;
    private ResultService resultService;
    private ShipmentService shipmentService;
    private TimeframeService timeframeService;
    private PostProcessingDataService postProcessingDataService;

    private Runnable timeframeRunner;
    private ArrayList<Handler> handlerList;

    public TourService(
        @NonNull ResultService resultService,
        @NonNull UpdateTransactionService updateTransactionService,
        @NonNull TimeService timeService,
        @NonNull TourInfoDao tourInfoDao,
        @NonNull FileHandlingService fileHandlingService,
        @NonNull PostProcessingDataService postProcessingDataService) {
        tourApiService = TourApiClientHelper.getTourApiService();
        dataHolder = DataHolder.getInstance();
        timeframeService = TimeframeService.getInstance();
        timeframeRunner = () -> timeframeService.showInfoOnActionbar();
        handlerList = new ArrayList<>();

        this.postProcessingDataService = postProcessingDataService;
        this.resultService = resultService;
        this.updateTransactionService = updateTransactionService;
        this.timeService = timeService;
        this.tourInfoDao = tourInfoDao;
        this.fileHandlingService = fileHandlingService;
    }

    public static TourService getInstance() {
        return ServiceFactory.getTourService();
    }

    public void setShipmentService(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Mirrored method from dataholder to get all stops.
     *
     * @return DataHolder
     */
    public List<StopBo> getStops(TourInfoBo tour) {
        return dataHolder.getStops(tour);
    }


    public boolean activeTourExists() {
        return tourInfoDao.getActiveTourInfo() != null;
    }

    /**
     * Returns all Stops having shipments that have no results.
     *
     * @return list of open stops
     */
    public List<StopBo> getOpenStopsForTour(TourInfoBo tour) {
        List<StopBo> allStops = dataHolder.getStops(tour);
        List<StopBo> openStops = new ArrayList<>();

        for (StopBo currentStop : allStops) {

            for (ShipmentBo shipment : currentStop.getShipments()) {
                ResultBo result = resultService.getLastResultByBarcode(shipment.getBarcode(), tour);
                if (!resultService.isResultFinished(result)) {
                    openStops.add(currentStop);
                    break;
                }
            }
        }

        return openStops;
    }

    /**
     * Returns a list of barcodes of the stop in case they are in the list of barcodes submitted.
     *
     * @param stop must not be null
     * @return arraylist in order to use it within an intent
     */
    public List<ShipmentBarcode> getBarcodesForStopWithinBarcodeList(StopBo stop, List<ShipmentBarcode> barcodes) {
        List<ShipmentBarcode> barcodesFromStop = getBarcodesForStop(stop);
        List<ShipmentBarcode> filteredBarcodesFromStop = new ArrayList<>();
        for (ShipmentBarcode barcode : barcodesFromStop) {
            if (barcodes.contains(barcode)) {
                filteredBarcodesFromStop.add(barcode);
            }
        }
        return filteredBarcodesFromStop;
    }

    public List<ShipmentBarcode> getBarcodesForStopsWithinBarcodeList(List<StopBo> stops, List<ShipmentBarcode> barcodes) {
        List<ShipmentBarcode> result = new ArrayList<>();
        for (StopBo stop : stops) {
            result.addAll(getBarcodesForStopWithinBarcodeList(stop, barcodes));
        }
        return result;
    }

    /**
     * Searches for a stop by shipment barcode
     *
     * @param barcode the shipment barcode
     * @return {@link StopBo} if there is a stop with a shipment identified by given barcode. {@code null} otherwise.
     */
    public StopBo getStopByBarcode(ShipmentBarcode barcode, TourInfoBo tour) {
        List<StopBo> stops = getStopsByBarcodes(Collections.singletonList(barcode), tour);
        if (stops.size() > 0) {
            return stops.get(0);
        }

        return null;
    }

    /**
     * Get barcodes with safeplaceAuthorization for recipient.
     */
    public List<ShipmentBarcode> getBarcodesWithSafeplaceAuthorizationForRecipient(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        List<ShipmentBarcode> result = new ArrayList<>();
        Set<StopBo> stops = getSortedStopsByBarcodes(barcodes, tour);
        // first check if any one time spa exists as this should be
        SafeplaceAuthorizationWrapper oneTimeSpaBarcode = getFirstOneTimeSafeplaceAuthorizationBarcodeInStopsAndBarcodes(stops, barcodes, tour);
        if (oneTimeSpaBarcode != null) {
            result.add(oneTimeSpaBarcode.getOneTimeSafeplaceAuthorizationBarcode());
        } else {
            // iterate through stops to check spa
            for (StopBo stop : stops) {
                if (stop.getRecipient().getSafeplaceAuthorizationService() != null) {
                    result.addAll(getBarcodesForStop(stop));
                }
            }
        }
        // remove barcodes from results that were not in the incoming barcodes
        result.retainAll(barcodes);
        return result;
    }

    /**
     * Will return oneTimeSafeplaceauthorization if one was found. If no safeplacearranegements was found the stops scanned for permanent
     * safeplaceauthorizations.
     *
     * @return null when no safeplaceauthorization was found otherwise the according safeplaceauthorization found
     */
    public SafeplaceAuthorizationWrapper getPrimarySafeplaceAuthorizationForStopWithinBarcodes(StopBo stop, List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        List<ShipmentBo> shipmentWithOneTimeSpaWithoutResult = getShipmentsWithOneTimeSpaWithoutResult(stop, barcodes, tour);

        if (shipmentWithOneTimeSpaWithoutResult.size() > 0) {
            ShipmentBo shipment = shipmentWithOneTimeSpaWithoutResult.get(0);
            SafeplaceAuthorizationServiceBo oneTimeSpa = shipment.getOneTimeSafeplaceArrangementService();
            return new SafeplaceAuthorizationWrapper(
                oneTimeSpa.getSafeplaceAuthorizationText(),
                oneTimeSpa.getSafeplaceAuthorizationId(),
                shipment.getBarcode());
        }
        SafeplaceAuthorizationServiceBo safeplaceAuthorizationService = null;
        if (stop.hasARecipient()) {
            safeplaceAuthorizationService = stop.getRecipient().getSafeplaceAuthorizationService();
        }

        // check if any result exists for a one time safeplaceauthorization as it has to be done first
        List<ResultBo> results = resultService.getResultsByBarcodesAndTour(barcodes, tour);
        // add forecast results in order to check if the current transaction could block any delivery option
        results.addAll(updateTransactionService.getResultForecastForCurrentTransactionWithinBarcodes(barcodes).values());
        boolean resultsContainPermanentSpa = resultService.resultsContainSafeplaceAuthorizationResultType(
            results,
            SafeplaceAuthorizationResultType.PERMANENT_SAFEPLACE_AUTHORIZATION);

        if (safeplaceAuthorizationService != null && !resultsContainPermanentSpa) {
            return new SafeplaceAuthorizationWrapper(
                safeplaceAuthorizationService.getSafeplaceAuthorizationText(),
                safeplaceAuthorizationService.getSafeplaceAuthorizationId());
        }

        return null;
    }

    @NonNull
    private List<ShipmentBo> getShipmentsWithOneTimeSpaWithoutResult(StopBo stop, List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        List<ResultBo> results = resultService.getResultsByBarcodesAndTour(barcodes, tour);
        Collection<UpdateSafeplaceAuthorizationResultCommand> safeplaceAuthorizationCommandsInCurrentTransaction
            = updateTransactionService.getSafeplaceAuthorizationCommandsInCurrentTransaction();

        List<ShipmentBo> shipmentsWithOneTimeSpa = getAllShipmentsHavingOneTimeSpa(stop.getShipments());
        List<ShipmentBo> shipmentsWithOneTimeSpaWithoutResult = new ArrayList<>(shipmentsWithOneTimeSpa);

        for (ShipmentBo shipment : shipmentsWithOneTimeSpa) {
            if (!barcodes.contains(shipment.getBarcode())) {
                shipmentsWithOneTimeSpaWithoutResult.remove(shipment);
            } else {
                for (ResultBo result : results) {
                    if (result.getBarcode().equals(shipment.getBarcode())
                        && result.getSafeplaceAuthorizationResult() != null
                        && result.getSafeplaceAuthorizationResult().getType() == SafeplaceAuthorizationResultType.ONE_TIME_SAFEPLACE_AUTHORIZATION
                        && (resultService.isResultFinished(result) || resultService.hasFinishedSpaResult(result))) {

                        shipmentsWithOneTimeSpaWithoutResult.remove(shipment);
                        break;
                    }
                }

                for (UpdateSafeplaceAuthorizationResultCommand command : safeplaceAuthorizationCommandsInCurrentTransaction) {
                    if (barcodes.contains(command.getBarcode())
                        && command.getSafeplaceAuthorizationResult().getType() == SafeplaceAuthorizationResultType.ONE_TIME_SAFEPLACE_AUTHORIZATION) {

                        shipmentsWithOneTimeSpaWithoutResult.remove(shipment);
                        break;
                    }
                }
            }
        }


        return shipmentsWithOneTimeSpaWithoutResult;
    }

    private List<ShipmentBo> getAllShipmentsHavingOneTimeSpa(Collection<ShipmentBo> shipments) {
        List<ShipmentBo> shipmentWithOneTimeSpa = new ArrayList<>();

        for (ShipmentBo shipment : shipments) {
            if (shipment.getOneTimeSafeplaceArrangementService() != null) {
                shipmentWithOneTimeSpa.add(shipment);
            }
        }
        return shipmentWithOneTimeSpa;
    }

    private SafeplaceAuthorizationWrapper getFirstOneTimeSafeplaceAuthorizationHavingNoResult(StopBo stop, List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        List<ShipmentBo> shipmentsHavingOneTimeSpa = getShipmentsWithOneTimeSpaWithoutResult(stop, barcodes, tour);

        if (shipmentsHavingOneTimeSpa.size() > 0) {
            ShipmentBo shipment = shipmentsHavingOneTimeSpa.get(0);
            SafeplaceAuthorizationServiceBo safeplaceAuthorizationService = shipment.getOneTimeSafeplaceArrangementService();
            return new SafeplaceAuthorizationWrapper(
                safeplaceAuthorizationService.getSafeplaceAuthorizationText(),
                safeplaceAuthorizationService.getSafeplaceAuthorizationId(),
                shipment.getBarcode());
        }
        return null;
    }

    private SafeplaceAuthorizationWrapper getFirstOneTimeSafeplaceAuthorizationBarcodeInStopsAndBarcodes(
        Set<StopBo> stops,
        List<ShipmentBarcode> barcodes,
        TourInfoBo tour) {
        for (StopBo stop : stops) {
            SafeplaceAuthorizationWrapper wrapper = getFirstOneTimeSafeplaceAuthorizationHavingNoResult(stop, barcodes, tour);
            if (wrapper != null) {
                return wrapper;
            }
        }
        return null;
    }

    public void loadTour(@NonNull TourIdentifier tourIdentifier) {
        loadTour(
            tourIdentifier.getSiteBarcode(),
            tourIdentifier.getTourDate(),
            tourIdentifier.getTourBarcode(),
            tourIdentifier.getAgentBarcode()
        );
    }

    /**
     * Fetches the complete tour identified by the tourBarcode
     */
    public void loadTour(SiteBarcode siteBarcode, @NonNull TourDate date, @NonNull TourBarcode tourBarcode, @NonNull AgentBarcode agentBarcode) {
        fileHandlingService.deleteFile(DeviceService.getInstance().getExternalStoragePath(), Constants.TOUR_DELETION_CLEARANCE_FILE_NAME);
        long clientTime = TimeService.getInstance().getCurrentTime();
        LogService.i(TAG, "REST call GET load tour data");

        // Anfrage senden
        raiseTourLoadProgressEvent(4, R.string.progressbar_send_request);

        tourApiService.getTour(
            DeviceService.getInstance().getHardwareId(),
            clientTime,
            siteBarcode.toInternalFormat(),
            date.toCompactFormat(),
            tourBarcode.getValue(),
            TraceUtil.generateTraceID(),
            TraceUtil.generateSpanID())
            .enqueue(new Callback<TourResponseDto>() {
                @SuppressLint("DefaultLocale")
                @Override
                public void onResponse(Call<TourResponseDto> call, Response<TourResponseDto> response) {

                    // Daten verarbeiten
                    raiseTourLoadProgressEvent(4, R.string.progressbar_process_data);
                    LogService.i(TAG, "REST call POST load tour data - response received (%s)", response.isSuccessful() ? "success" : "failure");

                    Gson gson = new Gson();
                    ErrorResponseDto errorResponse;
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            // in case method is invoked multiple times, we take the newest response as tour.
                            // Still this case should be avoided by caller!
                            deleteDataForTourIfExists(siteBarcode, date, tourBarcode);
                            createTour(response.body().getStops(), siteBarcode, date, tourBarcode, agentBarcode);
                        } else {
                            LogService.e(TAG, "REST call GET register scanner was successfull but with empty response body.");
                            new TourLoadErrorEvent(HttpStatusCode.valueOf(HttpsURLConnection.HTTP_INTERNAL_ERROR), null, null);
                        }
                    } else if (response.code() == HttpURLConnection.HTTP_CONFLICT) {
                        LogService.e(TAG, "REST call GET register scanner - not successful: client and server time difference");
                        new TourLoadErrorEvent(HttpStatusCode.valueOf(response.code()), null, response.message()).raise();
                    } else {
                        LogService.w(TAG, "REST call GET register scanner - not successful status code: %d", response.code());
                        if (response.errorBody() != null) {
                            try {
                                errorResponse = gson.fromJson(response.errorBody().charStream(), ErrorResponseDto.class);
                                if (errorResponse != null && !CollectionUtil.isNullOrEmpty(errorResponse.getHadvFaults())) {
                                    // For requests coming from delta, there exists max 1 hadv error code.
                                    HadvErrorCode hadvErrorCode = HadvErrorCode.getByCode(errorResponse.getHadvFaults().get(0).getHadvErrorCode());
                                    new TourLoadErrorEvent(HttpStatusCode.valueOf(response.code()), hadvErrorCode, response.message()).raise();
                                } else {
                                    new TourLoadErrorEvent(HttpStatusCode.valueOf(response.code()), null, response.message()).raise();
                                }
                            } catch (Exception e) {
                                new TourLoadErrorEvent(HttpStatusCode.valueOf(response.code()), null, response.message()).raise();
                            }
                        } else {
                            new TourLoadErrorEvent(HttpStatusCode.valueOf(response.code()), null, response.message()).raise();
                        }
                    }
                }

                @Override
                public void onFailure(Call<TourResponseDto> call, Throwable t) {
                    LogService.e(TAG, "could not load tour data: " + t.getMessage());
                    new TourLoadErrorEvent(HttpStatusCode.valueOf(HttpsURLConnection.HTTP_INTERNAL_ERROR), null, t.getMessage()).raise();
                }
            });

        DataHolder.getInstance().wipeAllValuesInMemoryStore();
    }

    private void deleteDataForTourIfExists(SiteBarcode siteBarcode, TourDate date, TourBarcode tourBarcode) {
        TourInfoBo tourToDelete = getTour(siteBarcode, date, tourBarcode);
        if (tourToDelete != null) {
            deleteDataForTour(tourToDelete);
        }
    }

    private void createTour(List<StopDto> stopDtos, SiteBarcode siteBarcode, TourDate date, TourBarcode tourBarcode, AgentBarcode agentBarcode) {
        // process received data
        Type listType = new TypeToken<List<StopBo>>() {
        }.getType();
        ModelMapper modelMapper = new TourModelMapper();

        List<StopBo> stops = modelMapper.map(stopDtos, listType);
        TourInfoBo tourInfo = new TourInfoBo(siteBarcode, date, tourBarcode, agentBarcode, TimeService.getInstance().getCurrentTime());
        tourInfo.setActive(true);
        tourInfoDao.createTour(tourInfo);

        setTourOnStops(stops, tourInfo);

        BarcodeUtil.detectCollectionShipments(TAG, stops);

        //Tour erstellen
        raiseTourLoadProgressEvent(4, R.string.progressbar_create_tour);

        dataHolder.addStops(stops);
        LogService.i(TAG, "tour data loaded");
        new TourServiceTourLoadedEvent(siteBarcode, date, tourBarcode, stops.size()).raise();

        //Zeitfenster initialisieren
        initializeTimeframeJob();
    }

    private void setTourOnStops(@NonNull Collection<StopBo> stops, TourInfoBo tour) {
        for (StopBo stop : stops) {
            stop.setTourId(tour);
            if (stop.getRecipient() != null) {
                stop.getRecipient().setTourId(tour);
            }
            for (ShipmentBo shipment : stop.getShipments()) {
                shipment.setTourId(tour);
            }
        }
    }

    private void raiseTourLoadProgressEvent(@SuppressWarnings("SameParameterValue") int size, int resourceID) {
        if (DeltaApp.getContext() != null) {
            new TourLoadProgressEvent(size, DeltaApp.getContext().getString(resourceID)).raise();
        } else {
            new TourLoadProgressEvent(size, "UnitTest");
        }
    }

    /**
     * Returns a set of {@link StopBo} by given barcodes
     */
    public SortedSet<StopBo> getSortedStopsByBarcodes(Collection<ShipmentBarcode> barcodes, TourInfoBo tour) {
        SortedSet<StopBo> stops = new TreeSet<>(new StopComparatorByAllProperties());
        stops.addAll(getStopsByBarcodes(barcodes, tour));
        return stops;
    }

    public List<StopBo> getStopsByBarcodes(Collection<ShipmentBarcode> barcodes, TourInfoBo tour) {

        Set<Integer> stopIds = new HashSet<>();

        // if getting stop from shipment is does not contain all information like recipients.
        // thus fetching stop object from database again.
        List<ShipmentBo> shipments = shipmentService.getShipmentsByBarcodes(barcodes, tour);
        if (!CollectionUtil.isNullOrEmpty(shipments)) {
            for (ShipmentBo shipment : shipments) {
                if (shipment.getStop() != null) {
                    stopIds.add(shipment.getStop().getId());
                }
            }
        }

        return getStopsByIds(stopIds);
    }

    public List<StopBo> getStopsByBarcodes(HashMap<ShipmentBarcode, StopBo> stopsByShipmentBarcodes, Collection<ShipmentBarcode> barcodes, TourInfoBo tour) {
        if (stopsByShipmentBarcodes == null) {
            return getStopsByBarcodes(barcodes, tour);
        }

        List<StopBo> stops = new ArrayList<>();
        for (ShipmentBarcode barcode : barcodes) {
            StopBo stop = stopsByShipmentBarcodes.get(barcode);
            if (stop != null && !stops.contains(stop)) {
                stops.add(stop);
            }
        }
        return stops;
    }

    /**
     * Returns an ordered set of stops having the order of given barcodes
     *
     * @param barcodes the list of barcodes. It will define the order of the result
     */
    public Set<StopBo> getStopsByBarcodesKeepBarcodeOrder(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        StopBo[] stopsInBarcodeOrder = new StopBo[barcodes.size()];

        for (StopBo stop : getStopsByBarcodes(barcodes, tour)) {
            int indexInList = Integer.MAX_VALUE;

            for (ShipmentBarcode barcode : getBarcodesForStop(stop)) {
                if (barcodes.contains(barcode)) {
                    indexInList = Math.min(indexInList, barcodes.indexOf(barcode));
                }
            }
            stopsInBarcodeOrder[indexInList] = stop;
        }

        return new LinkedHashSet<>(CollectionUtil.filter(Arrays.asList(stopsInBarcodeOrder), (s) -> s != null));
    }

    /**
     * Returns a list of barcodes for a given stop
     */
    public List<ShipmentBarcode> getBarcodesForStop(StopBo stop) {
        final List<ShipmentBo> shipments = new ArrayList<>(stop.getShipments());
        return CollectionUtil.map(shipments, ShipmentBo::getBarcode);
    }

    /**
     * Returns a list of unfinished barcodes for a given stop
     */
    public List<ShipmentBarcode> getUnfinishedBarcodesForStop(StopBo stop) {
        return resultService.getUnfinishedBarcodes(getBarcodesForStop(stop), getTourById(stop.getTourId()));
    }

    /**
     * Returns the unfinished barcodes for stop without the given barcodes
     */
    public List<ShipmentBarcode> getUnfinishedBarcodesForStopWithoutGivenBarcodes(StopBo stop, List<ShipmentBarcode> barcodes) {
        List<ShipmentBarcode> result = new ArrayList<>(getUnfinishedBarcodesForStop(stop));
        result.removeAll(barcodes);
        return result;
    }

    /**
     * Returns the unfinished barcodes for the stops in list without the given barcodes
     */
    public List<ShipmentBarcode> getUnfinishedBarcodesForStopsWithoutGivenBarcodes(List<StopBo> stops, List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        List<ShipmentBarcode> result = new ArrayList<>();
        for (StopBo stop : stops) {
            result.addAll(getUnfinishedBarcodesForStopWithoutGivenBarcodes(stop, barcodes));
        }
        return result;
    }

    /**
     * Returns a list of barcodes which barcodes exist in given barcode list
     */
    public List<ShipmentBarcode> getMatchingBarcodesInBarcodes(StopBo stopBo, List<ShipmentBarcode> barcodes) {
        List<ShipmentBo> matchingShipmentsInBarcodes = getMatchingShipmentsInBarcodes(stopBo, barcodes);
        List<ShipmentBarcode> matchingBarcodesInBarcodes = new ArrayList<>();
        for (ShipmentBo shipmentBo : matchingShipmentsInBarcodes) {
            matchingBarcodesInBarcodes.add(shipmentBo.getBarcode());
        }
        return matchingBarcodesInBarcodes;
    }

    /**
     * Returns a list of shipments which barcodes exist in given barcode list
     */
    public List<ShipmentBo> getMatchingShipmentsInBarcodes(StopBo stop, Collection<ShipmentBarcode> barcodes) {
        List<ShipmentBo> result = new ArrayList<>();
        for (ShipmentBo shipment : stop.getShipments()) {
            if (barcodes.contains(shipment.getBarcode())) {
                result.add(shipment);
            }
        }
        return result;
    }

    public List<StopBo> getStopsByIds(Collection<Integer> stopIds) {
        if (CollectionUtil.isNullOrEmpty(stopIds)) {
            return new ArrayList<>();
        }

        return dataHolder.getStopsByIds(stopIds);
    }

    public StopBo getStopById(int stopId) {
        return dataHolder.getStopById(stopId);
    }

    public boolean isUnknownShipmentBarcode(ShipmentBarcode barcode, TourInfoBo tour) {
        return ShipmentService.getInstance().getShipmentByBarcode(barcode, tour) == null;
    }

    public boolean isDemoTour(TourInfoBo tourInfoBo) {
        return DemoTourBarcode.matchesAny(tourInfoBo.getTourBarcode());
    }

    public boolean isDemoTourActive() {
        TourInfoBo activeTour = getActiveTour();
        return activeTour != null && isDemoTour(activeTour);
    }

    private void initializeTimeframeJob() {
        cancelAllHandlers();
        TourInfoBo activeTour = getActiveTour();
        if (activeTour != null) {
            List<ShipmentBo> shipmentBoList = timeframeService.getAllOpenTimeframeShipments(activeTour);
            for (ShipmentBo shipment : shipmentBoList) {
                addTimeFrameReminderHandler(shipment);
            }
        }
    }

    private void addTimeFrameReminderHandler(ShipmentBo shipment) {
        TimeframeServiceBo timeframeServiceBo = shipment.getTimeframeService();
        long timeTillHandlerExecute = timeframeServiceBo.getEnd() - System.currentTimeMillis();
        if (timeTillHandlerExecute > 0) {

            timeTillHandlerExecute = Math.max(timeTillHandlerExecute - Constants.TIMESPAN_ONE_HOUR, 0);
            Handler handler = new Handler();
            handler.postDelayed(timeframeRunner, timeTillHandlerExecute);

            handlerList.add(handler);
        }
    }

    void cancelAllHandlers() {
        for (Handler handler : handlerList) {
            handler.removeCallbacks(timeframeRunner);
        }
        handlerList.clear();
    }

    public TourInfoBo getActiveTour() {
        return tourInfoDao.getActiveTourInfo();
    }


    public void saveClearanceBarcode(Barcode clearanceBarcode, boolean isOfflineTour) {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.CLEARANCE_AGENT_BARCODE_COLUMN_NAME, clearanceBarcode.getValue());
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.IS_OFFLINE_TOUR_COLUMN_NAME, isOfflineTour);
    }

    public TourInfoBo getTour(SiteBarcode siteBarcode, TourDate date, TourBarcode tourBarcode) {
        return tourInfoDao.getTour(siteBarcode, date, tourBarcode);
    }

    public void setTourInactive(TourInfoBo currentTour) {
        tourInfoDao.toggleActive(currentTour.getId(), false);
    }

    public void setNextActualStopIdForCurrentTour(int nextStopId) {
        TourInfoBo activeTour = getActiveTour();
        if (activeTour != null) {
            tourInfoDao.setNextActualStopId(activeTour.getId(), nextStopId);
        } else {
            LogService.e(TAG, "Setting of nextActualStopID invoked, but no tour is active!");
        }
    }

    public void updateStopUuidForCurrentTour(UUID uuid) {
        TourInfoBo activeTour = getActiveTour();
        if (activeTour != null) {
            tourInfoDao.setStopUuid(activeTour.getId(), uuid);
        } else {
            LogService.e(TAG, "Setting of stopUuid invoked, but no tour is active!");
        }
    }

    public void deleteDataForTour(TourInfoBo tour) {
        tourInfoDao.delete(tour);
    }

    public List<TourInfoBo> getOpenTours() {
        return tourInfoDao.getInactiveTourInfos();
    }

    public AgentBarcode getAgentBarcodeForTour(TourInfoBo tour) {
        return tour != null ? tour.getAgentBarcode() : null;
    }

    public void setParcellockDailyToken(String dailyToken) {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.PARCELLOCK_DAILY_TOKEN_COLUMN_NAME, dailyToken);
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.PARCELLOCK_SESSION_CREATED_DATE_COLUMN_NAME, timeService.getCurrentDay());
    }

    public void setParcellockSessionId(String sessionId) {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.PARCELLOCK_SESSION_ID_COLUMN_NAME, sessionId);
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.PARCELLOCK_SESSION_CREATED_DATE_COLUMN_NAME, timeService.getCurrentDay());
    }

    void setVehicleData(int mileage, String vehicleBarcode) {
        VehicleBo vehicleData = new VehicleBo(mileage, vehicleBarcode, getActiveTour());
        dataHolder.createOrUpdateVehicleData(vehicleData);
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.VEHICLE_BARCODE_COLUMN_NAME, vehicleData);
    }

    void setReturnedShipments(Map<ShipmentBarcode, Timestamp> shipments) {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.RETURNED_SHIPMENTS_COLUMN_NAME, shipments);
    }

    void setAutomaticReturnedShipmentsScan(boolean automaticTourFinish) {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.AUTO_RETURN_SCAN_COLUMN_NAME, automaticTourFinish);
    }

    public void setEndOfWork(long currentTime) {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.END_OF_WORK_COLUMN_NAME, currentTime);
    }

    public TourInfoBo createNewOfflineTour(SiteBarcode siteBarcode, TourDate date, TourBarcode tourBarcode, AgentBarcode agentBarcode) {
        TourInfoBo tour = new TourInfoBo(siteBarcode, date, tourBarcode, agentBarcode, timeService.getCurrentTime());
        tour.setActive(true);
        tour.setOfflineTour(true);
        tourInfoDao.createTour(tour);
        return tour;
    }

    void setOfflineTourRegistered() {
        tourInfoDao.updateActiveTourInfoColumn(TourInfoBo.OFFLINE_TOUR_REGISTERED_COLUMN_NAME, true);
    }

    static void setRegisterOfflineRetryDelayInSeconds(int seconds) {
        LogService.d(TAG, "Setting delay of retries for registering offline tour to %d seconds.", seconds);
        registerOfflineRetryDelayInSeconds = seconds;
    }

    public void registerOfflineTourInBackgroundIfNeeded(Application application, TourInfoBo tour) {
        if (!tour.isOfflineTour()) {
            return;
        }

        JobScheduler jobScheduler = (JobScheduler) application.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo.Builder builder = new JobInfo.Builder(
            JobUtil.generateJobId(),
            new ComponentName(application.getPackageName(), RegisterOfflineTourJobSchedulerService.class.getName())
        );

        // create PersistableBundle with results to be sent
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(RegisterOfflineTourJobSchedulerService.JOB_PARAM_SITE_BARCODE, tour.getSiteBarcode().toInternalFormat());
        bundle.putString(RegisterOfflineTourJobSchedulerService.JOB_PARAM_TOUR_BARCODE, tour.getTourBarcode().getValue());
        bundle.putString(RegisterOfflineTourJobSchedulerService.JOB_PARAM_TOUR_DATE, tour.getDate().toIsoFormat());
        builder.setExtras(bundle);

        // NOTE: the NetworkType requirement does not work on emulator behind hermes proxy.
        // for testing on emulator, we use setRequiresCharging instead
        if (ConfigurationService.getInstance().isEmulator()) {
            builder.setRequiresCharging(true);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setRequiresBatteryNotLow(true);
            } else {
                builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            }
        }

        builder.setBackoffCriteria(registerOfflineRetryDelayInSeconds * 1000, JobInfo.BACKOFF_POLICY_LINEAR);
        builder.setPersisted(true);

        Objects.requireNonNull(jobScheduler).schedule(builder.build());
    }

    public boolean registerOfflineTour(TourInfoBo tour) {
        try {
            LogService.d(TAG, "Invoke registering of offline tour.");
            Response<ResponseBody> response = tourApiService.registerOfflineTour(
                DeviceService.getInstance().getHardwareId(),
                tour.getSiteBarcode().toInternalFormat(),
                tour.getDate().toCompactFormat(),
                tour.getTourBarcode().getValue(),
                TraceUtil.generateTraceID(),
                TraceUtil.generateSpanID()
            ).execute();

            if (response.isSuccessful()) {
                LogService.i(TAG, "Offline Tour successfully registered.");
                return true;
            } else {
                LogService.d(TAG, "Could not register offline tour. StatusCode is: %d, Message: '%s'", response.code(), response.message());
                return false;
            }

        } catch (IOException e) {
            LogService.d(TAG, "Could not register offline tour. Message is: %s", e.getMessage());
            return false;
        }
    }

    boolean isNoOfflineTourOrAlreadyRegistered(TourInfoBo tour) {
        return tour == null || !tour.isOfflineTour() || tour.isOfflineTourRegistered();
    }

    public boolean checkForCancellation(ShipmentBarcode barcode) {
        PostProcessingBo cancellation = postProcessingDataService.getPostProcessingDataByBarcodeAndReason(barcode, PostProcessingReason.CANCELLATION);
        return cancellation != null;
    }

    /**
     * Automatically set a {@link ReasonOfReturn#CANCELLATION_BY_ORIGINATOR} to all shipments in the active tour that have
     * a cancellation flag set to true.
     *
     * @param context the context that is required to start the background task.
     */
    public void setCancellationReturnCodesInBackground(Context context) {
        if (getActiveTour() == null) {
            return;
        }
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SetCancellationReturnCodeWorker.class)
            .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    public void sendCancellationReturnCode(ShipmentBarcode barcode, TourInfoBo tour) {
        ReasonOfReturn reasonOfReturn = ReasonOfReturn.CANCELLATION_BY_ORIGINATOR;

        // check if cancellation has already been sent
        ResultBo result = resultService.getLastResultByBarcode(barcode, tour);
        if (result != null && reasonOfReturn.equals(result.getReasonOfReturn())) {
            return;
        }

        // create stop for unknown shipments
        if (getStopByBarcode(barcode, tour) == null) {
            LogService.i(TAG, "Creating tentative shipment for barcode: '%s'", barcode);

            JobType jobType = BarcodeUtil.isCollection(barcode) ? JobType.COLLECTION : JobType.DELIVERY;
            ShipmentBo tentativeShipment = shipmentService.getOrCreateShipmentWithBarcode(barcode, jobType, getActiveTour());

            StopBo tentativeStop = new StopBo();
            tentativeStop.setCreatedOnTour(true);
            tentativeStop.setShipments(Collections.singletonList(tentativeShipment));
            tentativeStop.setTourId(tour);
            tentativeShipment.setStop(tentativeStop);

            StopService.getInstance().createOrUpdateStop(tentativeStop);
        } else {
            LogService.d(TAG, "Stop will not be created as it already exists. Barcode is: '%s'", barcode);
        }

        createUndeliveredShipmentResult(barcode, tour, reasonOfReturn);
    }

    /**
     * Marks a given shipment from the current tour as undelivered.
     *
     * @param barcode the barcode of an existing shipment
     * @param tour    the active tour
     * @param reason  the reason of return
     */
    public void markShipmentAsUndelivered(
        @NonNull ShipmentBarcode barcode,
        @NonNull TourInfoBo tour,
        @NonNull ReasonOfReturn reason
    ) {

        // check if result has already been sent
        ResultBo result = resultService.getLastResultByBarcode(barcode, tour);
        if (result != null && reason.equals(result.getReasonOfReturn())) {
            return;
        }

        createUndeliveredShipmentResult(barcode, tour, reason);
    }

    private void createUndeliveredShipmentResult(ShipmentBarcode barcode, TourInfoBo tour, ReasonOfReturn reasonOfReturn) {
        ShipmentBo shipment = shipmentService.getShipmentByBarcode(barcode, tour);
        updateTransactionService.beginNewTransactionIfNotExists();
        updateTransactionService.createUndeliveredShipmentResult(reasonOfReturn, shipment.getJobType(), Collections.singletonList(barcode));
        updateTransactionService.finishCurrentTransaction();
    }

    public TourInfoBo getTourById(int tourId) {
        return tourInfoDao.getTourById(tourId);
    }

    public void deleteActiveTourIfExists() {
        TourInfoBo activeTour = getActiveTour();
        if (activeTour != null) {
            deleteDataForTour(activeTour);
        }
    }
}
