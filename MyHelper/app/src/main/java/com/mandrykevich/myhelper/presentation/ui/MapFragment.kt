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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
import com.mandrykevich.myhelper.presentation.viewModel.MapViewModel
import com.mandrykevich.myhelper.presentation.viewModel.SearchViewModel
import com.mandrykevich.myhelper.utils.CommentsAdapter
import com.mandrykevich.myhelper.utils.OnCommentFetchListener
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MapFragment : Fragment(), OnCommentFetchListener {
    private lateinit var binding: FragmentMapBinding
    private lateinit var mapView: MapView
    private lateinit var mapStateManager: MapStateManager
    private lateinit var tapListener: MapTapListener
    private lateinit var addCommentUseCase: AddCommentUseCase
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var searchManager: SearchManager
    private lateinit var viewModel: MapViewModel
    private lateinit var viewModelSearch: SearchViewModel
    private val commentsList = mutableListOf<Comment>()

    private lateinit var imageViewStars: List<ImageView>
    private var activeStars = 0
    private var hasHelper = false
    private var hasElevator = false
    private var hasParking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater)
        viewModel = ViewModelProvider(this).get(MapViewModel::class.java)
        viewModelSearch = ViewModelProvider(this).get(SearchViewModel::class.java)
        setupRecyclerView()
        initializeMap()
        initializeFirebase()
        setupStarClickListeners()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        commentsAdapter = CommentsAdapter(commentsList)
        binding.rvReviews.adapter = commentsAdapter
    }

    private fun initializeMap() {
        mapView = binding.mainMap
        mapStateManager = MapStateManager(requireActivity())
        tapListener = MapTapListener("", requireContext(), binding, this)
        binding.mainMap.map.addTapListener(tapListener)
        val initialPosition = mapStateManager.restoreMapState(mapView)
        mapView.map.move(initialPosition)
    }

    private fun initializeFirebase() {
        val database = FirebaseDatabase.getInstance()
        val auth = FirebaseAuth.getInstance()
        addCommentUseCase = AddCommentUseCase(auth, database)
    }

    private fun setupStarClickListeners() {
        imageViewStars = listOf(
            binding.imOneStar,
            binding.imTwoStar,
            binding.imThreeStar,
            binding.imFourStar,
            binding.imFiveStar
        )
        imageViewStars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener { updateActiveStars(index + 1) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchButton()
        observeSearchResults()
        setupCommentButton()
        setupToggleButtons()
    }

    private fun setupSearchButton() {
        binding.imSearch.setOnClickListener {
            val query = binding.editTextText.text.toString()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(requireContext(), "Введите текст для поиска", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.onEach { geoObjects ->
            mapView.map.mapObjects.clear() // Очищаем предыдущие метки
            geoObjects.forEach { geoObject ->
                geoObject.geometry.firstOrNull()?.point?.let { point ->
                    addPlacemark(point) // Добавляем метку на карту

                    // Сохраняем запрос
                    viewModelSearch.addSearchQuery("Ваш запрос") // Замените на фактический текст запроса

                    // Перемещаем камеру к первой найденной метке
                    val cameraPosition = CameraPosition(Point(point.latitude, point.longitude), 18f, 0.0f, 0.0f)
                    mapView.map.move(cameraPosition, Animation(Animation.Type.SMOOTH, 0.5f), null) // Перемещение камеры
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun setupCommentButton() {
        binding.btnAddCom.setOnClickListener {
            binding.cardView9.visibility = View.VISIBLE
        }

        binding.btnAddComFromForm.setOnClickListener {
            val buildingId = tapListener.id_building
            val commentText = binding.editComText.text.toString()
            addComment(buildingId, commentText)
            binding.cardView9.visibility = View.GONE
            clearForm()
        }
    }

    private fun addComment(buildingId: String, commentText: String) {
        addCommentUseCase.execute(buildingId, commentText, activeStars, hasParking, hasElevator, hasHelper) {}
    }

    private fun setupToggleButtons() {
        binding.ivHasHelper.setOnClickListener { toggleButtonState(binding.ivHasHelper) }
        binding.ivHasElevator.setOnClickListener { toggleButtonState(binding.ivHasElevator) }
        binding.ivHasParking.setOnClickListener { toggleButtonState(binding.ivHasParking) }

        binding.mainMap.setOnClickListener {
            if (binding.cardViewInfo.visibility == View.VISIBLE) {
                binding.cardViewInfo.visibility = View.GONE
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
        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments")
        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                snapshot.children.mapNotNull { it.getValue(Comment::class.java) }
                    .filter { it.buildingId == buildingId }
                    .forEach { comment ->
                        commentsList.add(comment)
                        Log.d("MapFragment", "Comment added: ${comment.comment}")
                    }
                commentsAdapter.notifyDataSetChanged()
                binding.rvReviews.visibility = if (commentsList.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapFragment", "Ошибка получения комментариев: ${error.message}")
            }
        })
    }

    private fun setActiveStars(count: Int) {
        activeStars = count
        imageViewStars.forEachIndexed { index, imageView ->
            imageView.setColorFilter(
                if (index < count) ContextCompat.getColor(requireContext(), R.color.yellow)
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

    private fun addPlacemark(point: Point) {
        val placemark = mapView.map.mapObjects.addPlacemark(point)
        val icon = ImageProvider.fromResource(requireContext(), R.drawable.ic_result)
        placemark.setIcon(icon)
        placemark.setGeometry(point)
    }

    private fun performSearch(query: String) {
        val visibleRegion = VisibleRegionUtils.toPolygon(mapView.map.visibleRegion)
        viewModel.performSearch(query, visibleRegion)
    }

    private val searchSessionListener = object : Session.SearchListener {
        override fun onSearchResponse(response: Response) {
            val geoObjects = response.collection.children.mapNotNull { it.obj }
            geoObjects.forEach { geoObject ->
                geoObject.geometry.firstOrNull()?.point?.let { point ->
                    addPlacemark(point)
                }
            }
        }

        override fun onSearchError(error: Error) {
            Toast.makeText(requireContext(), "Ошибка поиска", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
}


