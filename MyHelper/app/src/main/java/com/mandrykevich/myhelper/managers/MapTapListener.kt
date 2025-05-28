package com.mandrykevich.myhelper.managers

import android.content.Context
import android.view.View
import android.widget.Toast
import com.mandrykevich.myhelper.databinding.FragmentMapBinding
import com.mandrykevich.myhelper.domain.usecase.AddCommentUseCase
import com.mandrykevich.myhelper.utils.OnCommentFetchListener
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.GeoObjectSelectionMetadata
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.MapObject

interface OnObjectTapListener {
    fun onObjectTapped(point: Point?, objectName: String?, objectId: String?)
}

class MapTapListener(
    var id_building: String,
    private val context: Context,
    private val binding: FragmentMapBinding,
    private val commentFetchListener: OnCommentFetchListener,
    private val objectTapListener: OnObjectTapListener

) : GeoObjectTapListener, MapObjectTapListener {

    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        val geoObject = geoObjectTapEvent.geoObject
        val selectionMetadata: GeoObjectSelectionMetadata? = geoObject.metadataContainer.getItem(GeoObjectSelectionMetadata::class.java)

        if (selectionMetadata != null) {
            binding.mainMap.map.selectGeoObject(selectionMetadata)

            id_building = selectionMetadata.objectId
            binding.objectName.text = geoObject.name

            val tappedPoint = geoObject.geometry.firstOrNull()?.point
            objectTapListener.onObjectTapped(tappedPoint, geoObject.name, selectionMetadata.objectId)

            if(binding.objectName.text==null || binding.objectName.text=="" ){
                binding.cardViewInfo.visibility = View.GONE
                binding.cardAddComment.visibility = View.GONE
            } else{
                binding.cardViewInfo.visibility = View.VISIBLE
                commentFetchListener.fetchComments(id_building)
            }
        } else {
            objectTapListener.onObjectTapped(null, null, null)
            binding.cardViewInfo.visibility = View.GONE
        }
        return true
    }

    override fun onMapObjectTap(mapObject: MapObject, point: Point): Boolean {
        // Обработка тапа по объекту карты
        objectTapListener.onObjectTapped(point, null, null)
        return true
    }
}

