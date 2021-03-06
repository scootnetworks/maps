package com.mapbox.rctmgl.components.styles.layers;

import android.content.Context;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.rctmgl.components.AbstractMapFeature;
import com.mapbox.rctmgl.components.mapview.RCTMGLMapView;
import com.mapbox.rctmgl.location.UserLocationLayerConstants;
import com.mapbox.rctmgl.utils.ExpressionParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by nickitaliano on 9/7/17.
 */

public abstract class RCTLayer<T extends Layer> extends AbstractMapFeature {
    public static final String LOG_TAG = RCTLayer.class.getSimpleName();

    protected String mID;
    protected String mSourceID;
    protected String mAboveLayerID;
    protected String mBelowLayerID;

    protected Integer mLayerIndex;
    protected boolean mVisible;
    protected Double mMinZoomLevel;
    protected Double mMaxZoomLevel;
    protected ReadableMap mReactStyle;
    protected Expression mFilter;

    protected MapboxMap mMap;
    protected T mLayer;

    protected Context mContext;
    protected RCTMGLMapView mMapView;

    protected boolean mHadFilter;

    public RCTLayer(Context context) {
        super(context);
        mContext = context;
        mHadFilter = false;
    }

    public String getID() {
        return mID;
    }

    public void setID(String id) {
        mID = id;
    }

    public void setSourceID(String sourceID) {
        mSourceID = sourceID;
    }

    public void setAboveLayerID(String aboveLayerID) {
        if (mAboveLayerID != null && mAboveLayerID.equals(aboveLayerID)) {
            return;
        }

        mAboveLayerID = aboveLayerID;
        if (mLayer != null) {
            removeFromMap(mMapView);
            addAbove(mAboveLayerID);
        }
    }

    public void setBelowLayerID(String belowLayerID) {
        if (mBelowLayerID != null && mBelowLayerID.equals(belowLayerID)) {
            return;
        }

        mBelowLayerID = belowLayerID;
        if (mLayer != null) {
            removeFromMap(mMapView);
            addBelow(mBelowLayerID);
        }
    }

    public void setLayerIndex(int layerIndex) {
        if (mLayerIndex != null && mLayerIndex == layerIndex) {
            return;
        }

        mLayerIndex = layerIndex;
        if (mLayer != null) {
            removeFromMap(mMapView);
            addAtIndex(mLayerIndex);
        }
    }

    public void setVisible(boolean visible) {
        mVisible = visible;

        if (mLayer != null) {
            String visibility = mVisible ? Property.VISIBLE : Property.NONE;
            mLayer.setProperties(PropertyFactory.visibility(visibility));
        }
    }

    public void setMinZoomLevel(double minZoomLevel) {
        mMinZoomLevel = minZoomLevel;

        if (mLayer != null) {
            mLayer.setMinZoom((float) minZoomLevel);
        }
    }

    public void setMaxZoomLevel(double maxZoomLevel) {
        mMaxZoomLevel = maxZoomLevel;

        if (mLayer != null) {
            mLayer.setMaxZoom((float) maxZoomLevel);
        }
    }

    public void setReactStyle(ReadableMap reactStyle) {
        mReactStyle = reactStyle;

        if (mLayer != null) {
            addStyles();
        }
    }

    public void setFilter(ReadableArray readableFilterArray) {
        Expression filterExpression = ExpressionParser.from(readableFilterArray);

        mFilter = filterExpression;

        if (mLayer != null) {
            if (mFilter != null) {
                mHadFilter = true;
                updateFilter(mFilter);
            } else if (mHadFilter) {
                updateFilter(Expression.literal(true));
            }
        }
    }

    public void add() {
        if (!hasInitialized()) {
            return;
        }

        String userBackgroundID = UserLocationLayerConstants.BACKGROUND_LAYER_ID;
        Layer userLocationBackgroundLayer = mMap.getStyle().getLayer(userBackgroundID);

        // place below user location layer
        if (userLocationBackgroundLayer != null) {
            mMap.getStyle().addLayerBelow(mLayer, userBackgroundID);
            return;
        }

        mMap.getStyle().addLayer(mLayer);
    }

    public void addAbove(String aboveLayerID) {
        if (!hasInitialized()) {
            return;
        }
        mMap.getStyle().addLayerAbove(mLayer, aboveLayerID);
    }

    public void addBelow(String belowLayerID) {
        if (!hasInitialized()) {
            return;
        }
        mMap.getStyle().addLayerBelow(mLayer, belowLayerID);
    }

    public void addAtIndex(int index) {
        if (!hasInitialized()) {
            return;
        }
        mMap.getStyle().addLayerAt(mLayer, index);
    }

    protected void insertLayer() {
        if (mMap.getStyle().getLayer(mID) != null) {
            return; // prevent adding a layer twice
        }

        if (mAboveLayerID != null) {
            addAbove(mAboveLayerID);
        } else if (mBelowLayerID != null) {
            addBelow(mBelowLayerID);
        } else if (mLayerIndex != null) {
            addAtIndex(mLayerIndex);
        } else {
            add();
        }

        setZoomBounds();
    }

    protected void setZoomBounds() {
        if (mMaxZoomLevel != null) {
            mLayer.setMaxZoom(mMaxZoomLevel.floatValue());
        }

        if (mMinZoomLevel != null) {
            mLayer.setMinZoom(mMinZoomLevel.floatValue());
        }
    }

    protected void updateFilter(Expression expression) {
        // override if you want to update the filter
    }

    @Override
    public void addToMap(RCTMGLMapView mapView) {
        mMap = mapView.getMapboxMap();
        mMapView = mapView;

        T existingLayer = mMap.getStyle().<T>getLayerAs(mID);
        if (existingLayer != null) {
            mLayer = existingLayer;
        } else {
            mLayer = makeLayer();
            insertLayer();
        }

        addStyles();
        if (mFilter != null) {
            mHadFilter = true;
            updateFilter(mFilter);
        }
    }

    @Override
    public void removeFromMap(RCTMGLMapView mapView) {
        if (mMap.getStyle() != null) {
            mMap.getStyle().removeLayer(mLayer);
        }
    }

    public abstract T makeLayer();
    public abstract void addStyles();

    private boolean hasInitialized() {
        return mMap != null && mLayer != null;
    }
}
