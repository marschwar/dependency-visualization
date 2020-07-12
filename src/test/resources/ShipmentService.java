package de.hermes.delta.service;

import static de.hermes.delta.util.CollectionUtil.*;
import static java.util.Collections.emptyList;
import java.util.function.*;
import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import de.hermes.delta.DeltaApp;
import de.hermes.delta.R;
import de.hermes.delta.dao.DaoFactory;
import de.hermes.delta.data.DataHolder;
import de.hermes.delta.domain.CashOnDeliveryServiceBo;
import de.hermes.delta.domain.ShipmentBarcode;
import de.hermes.delta.domain.ShipmentBo;
import de.hermes.delta.domain.TourInfoBo;
import de.hermes.delta.enumeration.JobType;
import de.hermes.delta.enumeration.PostProcessingReason;
import de.hermes.delta.enumeration.ServiceType;
import de.hermes.delta.enumeration.ShipmentType;
import de.hermes.delta.util.CollectionUtil;
import de.hermes.delta.util.Constants;


public class ShipmentService {

    @SuppressWarnings("unused")
    private static final String TAG = ShipmentService.class.getSimpleName();

    private DataHolder dataHolder;
    private Dao<ShipmentBo, Integer> shipmentDao;
    private Dao<CashOnDeliveryServiceBo, Integer> codServiceDao;

    public ShipmentService() {
        dataHolder = DataHolder.getInstance();
        DeltaApp application = DeltaApp.getInstance();

        if (application != null) {
            shipmentDao = DaoFactory.getShipmentDao();
            if (shipmentDao != null) { // could be null in unit tests
                try {
                    shipmentDao.setObjectCache(true);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            codServiceDao = DatabaseService.getInstance().createDao(CashOnDeliveryServiceBo.class, DeltaApp.getContext());
        }
    }

    public static ShipmentService getInstance() {
        return ServiceFactory.getShipmentService();
    }

    public ShipmentBo getShipmentByBarcode(ShipmentBarcode barcode, TourInfoBo tour) {
        return dataHolder.getShipmentByBarcode(barcode, tour);
    }

    public int countShipmentsByShipmentType(List<ShipmentBo> shipments, ShipmentType type) {
        int count = 0;
        for (ShipmentBo shipment : shipments) {
            if (shipment.getShipmentType() == type) {
                count = count + 1;
            }
        }
        return count;
    }

    public int countShipmentsByJobType(List<ShipmentBo> shipments, JobType type) {
        int count = 0;
        for (ShipmentBo shipment : shipments) {
            if (shipment.getJobType() == type) {
                count = count + 1;
            }
        }
        return count;
    }

    public ShipmentBo getOrCreateShipmentWithBarcode(ShipmentBarcode barcode, JobType jobType, TourInfoBo tour) {
        ShipmentBo shipment = getShipmentByBarcode(barcode, tour);
        if (shipment == null) {
            shipment = new ShipmentBo(barcode, jobType, tour);
            shipment.setCancelled(ServiceFactory.getTourService().checkForCancellation(barcode));
        }
        return shipment;
    }

    public void createOrUpdateShipment(ShipmentBo shipment) {
        try {
            if (shipment.getId() > 0) {
                // if shipment already exists, the foreign objects may not be updated correctly.
                codServiceDao.createOrUpdate(shipment.getCashOnDeliveryService());
            } else {
                if (ServiceFactory.getTourService().checkForCancellation(shipment.getBarcode())) {
                    shipment.setCancelled(true);
                }
            }
            shipmentDao.createOrUpdate(shipment);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteShipment(ShipmentBarcode barcode, TourInfoBo tour) {
        dataHolder.deleteShipment(barcode, tour);
    }

    public String getOriginatorNameOrUnknown(Context context, ShipmentBo shipment) {
        String originatorName = shipment.getOriginatorName();
        if (originatorName == null || originatorName.equals("")) {
            return context.getString(R.string.originator_unknown);
        } else {
            return originatorName;
        }
    }

    public Collection<ShipmentBo> getShipmentsByIds(Collection<Integer> shipmentIds) {
        return dataHolder.getShipmentsByIds(shipmentIds);
    }

    /**
     * @param barcodes
     * @param tour
     * @return an immutable list
     */
    public List<ShipmentBo> getShipmentsByBarcodes(Collection<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> query = shipmentDao.queryBuilder();
            query.where().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(query.prepare());
        } catch (SQLException e) {
            throw new RuntimeException("can not get shipments for barcodes: " + barcodes, e);
        }
    }

    /**
     * Filters given barcodes by barcodes belong to shipments having a recipient.
     * Does not filtering in place.
     *
     * @param barcodes a list of barcodes to filter. NOT {@code NULL}
     * @return a new list with filtered barcodes
     */
    public List<ShipmentBarcode> filterExistingBarcodesWithRecipient(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        List<ShipmentBarcode> result = new ArrayList<>();

        for (ShipmentBarcode barcode : barcodes) {
            ShipmentBo shipmentForBarcode = getShipmentByBarcode(barcode, tour);
            if (shipmentForBarcode != null && shipmentForBarcode.hasARecipient()) {
                result.add(barcode);
            }
        }

        return result;
    }

    public List<ShipmentBo> getShipmentsHavingCashOnDeliveryService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .isNotNull(ShipmentBo.CASH_ON_DELIVERY_SERVICE_COLUMN_NAME)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBarcode> filterOnlyCashOnDeliveryServiceBarcodes(List<ShipmentBarcode> barcodes, TourInfoBo tourInfoBo) {
        return map(
            this.getShipmentsHavingCashOnDeliveryService(barcodes, tourInfoBo),
            ShipmentBo::getBarcode);
    }

    public List<ShipmentBo> getShipmentsByBarcodesHavingSafeplaceAuthorization(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .isNotNull(ShipmentBo.ONE_TIME_SAFEPLACE_AUTHORIZATION_COLUMN_NAME)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsByBarcodesHavingParcellockBoxService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().isNotNull(ShipmentBo.PARCELLOCK_SERVICE_COLUMN_NAME).and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsByBarcodesHavingParcellockLockerService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().isNotNull(ShipmentBo.PARCELLOCK_LOCKER_SERVICE_COLUMN_NAME).and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsHavingTimeframeService(TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().isNotNull(ShipmentBo.TIME_FRAME_SERVICE_COLUMN_NAME)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsByBarcodesHavingTimeframeService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().isNotNull(ShipmentBo.TIME_FRAME_SERVICE_COLUMN_NAME).and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean anyShipmentsWithinBarcodesHasNeighbourhoodDeliveryBan(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        return !CollectionUtil.isNullOrEmpty(getShipmentsWithinBarcodesHasNeighbourhoodDeliveryBan(barcodes, tour));
    }

    public List<ShipmentBo> getShipmentsWithinBarcodesHasNeighbourhoodDeliveryBan(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .eq(ShipmentBo.NEIGHBOURHOOD_DELIVERY_BAN_SERVICE_COLUMN_NAME, true)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated use {@link #anyShipmentWithReceiptlessService(List)} if possible to avoid additional database access
     */
    public List<ShipmentBo> getShipmentsWithinBarcodesHasReceiptlessService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .eq(ShipmentBo.RECEIPTLESS_SERVICE_COLUMN_NAME, true)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean anyShipmentWithReceiptlessService(@NonNull List<ShipmentBo> shipments) {
        return CollectionUtil.any(shipments, ShipmentBo::isReceiptlessService);
    }

    public List<ShipmentBo> getShipmentsWithinBarcodesHasReceiptlessGoodsShipmentService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .eq(ShipmentBo.RECEIPTLESS_GOODS_SHIPMENT_SERVICE_COLUMN_NAME, true)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsByBarcodesHavingDesiredNeighbourService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .isNotNull(ShipmentBo.DESIRED_NEIGHBOUR_SERVICE_COLUMN_NAME)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsByBarcodesHavingIdentService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .isNotNull(ShipmentBo.IDENT_SERVICE_COLUMN_NAME)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBarcode> getBarcodesWithoutIdentService(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where()
                .isNull(ShipmentBo.IDENT_SERVICE_COLUMN_NAME)
                .and().in(Constants.BARCODE_COLUMN_NAME, barcodes)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return map(shipmentDao.query(queryBuilder.prepare()), ShipmentBo::getBarcode);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsByBarcodesContainingServiceType(List<ShipmentBarcode> barcodes, ServiceType serviceType, TourInfoBo tour) {
        switch (serviceType) {

            case CASH_ON_DELIVERY:
                return getShipmentsHavingCashOnDeliveryService(barcodes, tour);
            case NEIGHBOURHOOD_DELIVERY_BAN:
                return getShipmentsWithinBarcodesHasNeighbourhoodDeliveryBan(barcodes, tour);
            case DESIRED_NEIGHBOUR:
                return getShipmentsByBarcodesHavingDesiredNeighbourService(barcodes, tour);
            case IDENT:
                return getShipmentsByBarcodesHavingIdentService(barcodes, tour);
            case RECEIPTLESS:
                return getShipmentsWithinBarcodesHasReceiptlessService(barcodes, tour);
            case RECEIPTLESS_GOODS_SHIPMENT:
                return getShipmentsWithinBarcodesHasReceiptlessGoodsShipmentService(barcodes, tour);
            case SAFEPLACE_AUTHORIZATION:
                return getShipmentsByBarcodesHavingSafeplaceAuthorization(barcodes, tour);
            case TIMEFRAME:
                return getShipmentsByBarcodesHavingTimeframeService(barcodes, tour);
            case PARCELLOCK_BOX:
                return getShipmentsByBarcodesHavingParcellockBoxService(barcodes, tour);
            case PARCELLOCK_LOCKER:
                return getShipmentsByBarcodesHavingParcellockLockerService(barcodes, tour);
            default:
                throw new IllegalStateException("not implemented yet");
        }
    }

    public boolean allShipmentsWithinBarcodesContainsServiceReceiptless(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        return getShipmentsWithinBarcodesHasReceiptlessService(barcodes, tour).size() == barcodes.size();
    }

    /**
     * @deprecated use {@link #allReceiptlessGoodsService(Collection)} if possible to avoid additional database access
     */
    public boolean allShipmentsWithinBarcodesContainsServiceReceiptlessGoodsShipment(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        return getShipmentsWithinBarcodesHasReceiptlessGoodsShipmentService(barcodes, tour).size() == barcodes.size();
    }

    public boolean allReceiptlessGoodsService(@NonNull Collection<ShipmentBo> shipments) {
        return CollectionUtil.all(shipments, ShipmentBo::isReceiptlessGoodsShipmentService);
    }

    /**
     * @deprecated use the overloaded method if possible to avoid additional database lookup
     */
    public boolean anyShipmentRequiresPersonalHandover(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        return !CollectionUtil.isNullOrEmpty(getShipmentsByBarcodesContainingServiceType(barcodes, ServiceType.IDENT, tour))
               || !CollectionUtil.isNullOrEmpty(getShipmentsByBarcodesContainingServiceType(barcodes, ServiceType.CASH_ON_DELIVERY, tour));
    }

    public boolean anyShipmentRequiresPersonalHandover(List<ShipmentBo> shipments) {
        return CollectionUtil.any(shipments, (shipment) ->
            shipment.getServicesTypes().contains(ServiceType.IDENT) || shipment.getServicesTypes().contains(ServiceType.CASH_ON_DELIVERY)
        );
    }

    public boolean allShipmentsKnown(List<ShipmentBarcode> barcodes, TourInfoBo tour) {
        Set<ShipmentBarcode> barcodeSet = new HashSet<>(barcodes);
        List<ShipmentBo> shipments = getShipmentsByBarcodes(barcodeSet, tour);
        return barcodeSet.size() == shipments.size();
    }

    public Map<ShipmentBarcode, ShipmentBo> getShipmentMapByBarcodes(Collection<ShipmentBarcode> barcodes, TourInfoBo tour) {
        Map<ShipmentBarcode, ShipmentBo> stops = new HashMap<>();

        for (ShipmentBo shipment : getShipmentsByBarcodes(barcodes, tour)) {
            stops.put(shipment.getBarcode(), shipment);
        }

        return stops;
    }

    public List<ShipmentBarcode> getAllPrakBarcodesFromTour(TourInfoBo tour) {
        List<ShipmentBo> praks = getPraks(tour);
        List<ShipmentBarcode> prakBarcodes = new ArrayList<>();

        for (ShipmentBo prak : praks) {
            prakBarcodes.add(prak.getBarcode());
        }

        return prakBarcodes;
    }

    private List<ShipmentBo> getPraks(TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().eq(ShipmentBo.JOB_TYPE_COLUMN_NAME, JobType.PARCELSHOP_COLLECTION)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());

            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public List<ShipmentBo> getShipmentsWithLimitedQuantity(TourInfoBo tour) {
        try {
            if (tour == null) {
                return new ArrayList<>();
            }

            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().eq(ShipmentBo.LIMITED_QUANTITY_COLUMN_NAME, true)
                .and().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ShipmentBo> getShipmentsForTour(TourInfoBo tour) {
        try {
            QueryBuilder<ShipmentBo, Integer> queryBuilder = shipmentDao.queryBuilder();
            queryBuilder.where().eq(ShipmentBo.TOUR_INFO_ID_COLUMN_NAME, tour.getId());
            return shipmentDao.query(queryBuilder.prepare());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SortedSet<ShipmentBarcode> getBarcodesForTour(TourInfoBo tour) {
        SortedSet<ShipmentBarcode> result = new TreeSet<>();
        for (ShipmentBo shipment : getShipmentsForTour(tour)) {
            result.add(shipment.getBarcode());
        }
        return result;
    }

    public boolean hasPriorityService(List<ShipmentBo> shipments) {
        return CollectionUtil.any(shipments, shipment -> shipment.isPriorityService());
    }

    /**
     * @deprecated Because the cancellation flag will be added to the shipment information as per: GREEN-223
     */
    public void updateAllShipmentsCancellationState() {

        try {
            shipmentDao.updateRaw(
                "update shipment set cancelled=1 where barcode in (select barcode from post_processing where reason=?)",
                PostProcessingReason.CANCELLATION.name());
            shipmentDao.clearObjectCache();
        } catch (SQLException e) {
            Log.e(TAG, "error marking shipments as cancelled");
        }
    }
}
