package com.mandrykevich.myhelper.presentation.ui

import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mandrykevich.myhelper.R
import com.mandrykevich.myhelper.data.repository.Comment
import com.mandrykevich.myhelper.databinding.FragmentMapBinding
import com.mandrykevich.myhelper.domain.usecase.AddCommentUseCase
import com.mandrykevich.myhelper.managers.MapStateManager
import com.mandrykevich.myhelper.managers.MapTapListener
import com.mandrykevich.myhelper.utils.CommentsAdapter
import com.mandrykevich.myhelper.utils.OnCommentFetchListener
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error

class MapFragment : Fragment(), OnCommentFetchListener {
    private lateinit var binding: FragmentMapBinding
    private lateinit var mapView: MapView
    private lateinit var mapStateManager: MapStateManager
    private lateinit var tapListener: MapTapListener
    private lateinit var addCommentUseCase: AddCommentUseCase
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var searchManager: SearchManager
    private val commentsList = mutableListOf<Comment>()

    private lateinit var imageViewStars: List<ImageView>
    private var activeStars = 0
    private var hasHelper = false
    private var hasElevator = false
    private var hasParking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater)
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        commentsAdapter = CommentsAdapter(commentsList)
        binding.rvReviews.adapter = commentsAdapter
        mapView = binding.mainMap
        mapStateManager = MapStateManager(requireActivity())
        val database = FirebaseDatabase.getInstance()
        val auth = FirebaseAuth.getInstance()
        addCommentUseCase = AddCommentUseCase(auth, database)
        tapListener = MapTapListener("", requireContext(), binding, this)

        imageViewStars = listOf(
            binding.imOneStar,
            binding.imTwoStar,
            binding.imThreeStar,
            binding.imFourStar,
            binding.imFiveStar
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        for (i in imageViewStars.indices) {
            imageViewStars[i].setOnClickListener {
                updateActiveStars(i + 1) // Измените здесь
            }
        }

        binding.btnAddCom.setOnClickListener {
            binding.cardView9.visibility = View.VISIBLE
        }

        binding.ivHasHelper.setOnClickListener { toggleButtonState(binding.ivHasHelper) }
        binding.ivHasElevator.setOnClickListener { toggleButtonState(binding.ivHasElevator) }
        binding.ivHasParking.setOnClickListener { toggleButtonState(binding.ivHasParking) }

        binding.mainMap.setOnClickListener {
            if (binding.cardViewInfo.visibility == View.VISIBLE) {
                binding.cardViewInfo.visibility = View.GONE
            }
        }

        binding.mainMap.map.addTapListener(tapListener)
        val initialPosition = mapStateManager.restoreMapState(mapView)
        mapView.map.move(initialPosition)


        binding.btnAddComFromForm.setOnClickListener {
            val buildingId = tapListener.id_building
            val commentText = binding.editComText.text.toString()
            val rating = activeStars
            val hasDisabledParking = hasParking
            val hasElevator = hasElevator
            val hasHelper = hasHelper

            addCommentUseCase.execute(buildingId, commentText, rating, hasDisabledParking, hasElevator, hasHelper) {}
            binding.cardView9.visibility = View.GONE
            clearForm()
        }

        binding.imSearch.setOnClickListener {
            val query = binding.editTextText.text.toString()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(requireContext(), "Введите текст для поиска", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mainMap.map.removeTapListener(tapListener)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        mapStateManager.saveMapState(mapView)
    }

    override fun fetchComments(buildingId: String) {
        val database = FirebaseDatabase.getInstance()
        val commentsRef = database.getReference("Comments")

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    if (comment != null && comment.buildingId == buildingId) {
                        commentsList.add(comment)
                        Log.d("MapFragment", "Comment added: ${comment.comment}")
                    }
                }
                commentsAdapter.notifyDataSetChanged()

                if (commentsList.isEmpty()) {
                    binding.rvReviews.visibility = View.GONE
                } else {
                    binding.rvReviews.visibility = View.VISIBLE

                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("MapFragment", "Ошибка получения комментариев: ${error.message}")
            }
        })
    }

    private fun setActiveStars(count: Int) {
        activeStars = count
        for (i in imageViewStars.indices) {
            imageViewStars[i].setColorFilter(
                if (i < count) ContextCompat.getColor(requireContext(), R.color.yellow)
                else ContextCompat.getColor(requireContext(), R.color.black),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun updateActiveStars(count: Int) {
        setActiveStars(count)
    }

    private fun toggleButtonState(imageView: ImageView) {
        val isActive = when (imageView) {
            binding.ivHasHelper -> hasHelper
            binding.ivHasElevator -> hasElevator
            binding.ivHasParking -> hasParking
            else -> false
        }

        val newColor = if (!isActive) {
            when (imageView) {
                binding.ivHasHelper -> {
                    hasHelper = true
                    ContextCompat.getColor(requireContext(), R.color.yellow)
                }
                binding.ivHasElevator -> {
                    hasElevator = true
                    ContextCompat.getColor(requireContext(), R.color.yellow)
                }
                binding.ivHasParking -> {
                    hasParking = true
                    ContextCompat.getColor(requireContext(), R.color.yellow)
                }
                else -> ContextCompat.getColor(requireContext(), R.color.black)
            }
        } else {
            when (imageView) {
                binding.ivHasHelper -> {
                    hasHelper = false
                    ContextCompat.getColor(requireContext(), R.color.black)
                }
                binding.ivHasElevator -> {
                    hasElevator = false
                    ContextCompat.getColor(requireContext(), R.color.black)
                }
                binding.ivHasParking -> {
                    hasParking = false
                    ContextCompat.getColor(requireContext(), R.color.black)
                }
                else -> ContextCompat.getColor(requireContext(), R.color.black)
            }
        }

        imageView.setColorFilter(newColor, PorterDuff.Mode.SRC_IN)
    }

    private fun clearForm() {
        setActiveStars(0)
        binding.editComText.text.clear()
        hasHelper = false
        hasElevator = false
        hasParking = false

        binding.ivHasHelper.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black), PorterDuff.Mode.SRC_IN)
        binding.ivHasElevator.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black), PorterDuff.Mode.SRC_IN)
        binding.ivHasParking.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black), PorterDuff.Mode.SRC_IN)
    }

    private val searchSessionListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val geoObjects = response.collection.children.mapNotNull { it.obj }
            // Обработка результатов поиска
            geoObjects.forEach { geoObject ->
                val point = geoObject.geometry
                if (point != null) {
                    mapView.map.mapObjects.addPlacemark()
                }
            }
        }

        override fun onSearchError(p0: Error) {
            Toast.makeText(requireContext(), "Ошибка поиска", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch(query: String) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value // Исправлено
            resultPageSize = 32
        }

        val session = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(mapView.map.visibleRegion),
            searchOptions,
            searchSessionListener
        )
    }


    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
}