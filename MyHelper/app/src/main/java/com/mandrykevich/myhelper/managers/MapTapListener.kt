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


class MapTapListener(
    var id_building: String,
    private val context: Context,
    private val binding: FragmentMapBinding,
    private val commentFetchListener: OnCommentFetchListener

) : GeoObjectTapListener {

    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        val geoObject = geoObjectTapEvent.geoObject
        val selectionMetadata: GeoObjectSelectionMetadata? = geoObject.metadataContainer.getItem(GeoObjectSelectionMetadata::class.java)



        if (selectionMetadata != null) {
            binding.mainMap.map.selectGeoObject(selectionMetadata)

            id_building = selectionMetadata.objectId
            binding.objectName.text = geoObject.name
            if(binding.objectName.text==null || binding.objectName.text=="" ){
                binding.cardViewInfo.visibility = View.GONE
                binding.cardAddComment.visibility = View.GONE

            } else{
                binding.cardViewInfo.visibility = View.VISIBLE
                commentFetchListener.fetchComments(id_building)


            }
        } else {
            binding.cardViewInfo.visibility = View.GONE


        }
        return true
    }





}

