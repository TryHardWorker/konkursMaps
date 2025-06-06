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
import com.yandex.mapkit.user_location.UserLocationLayer
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.ui_view.ViewProvider
import android.widget.TextView
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.layers.ObjectEvent
import com.mandrykevich.myhelper.managers.OnObjectTapListener
import com.yandex.mapkit.map.IconStyle
import android.graphics.PointF
import com.mandrykevich.myhelper.domain.usecase.AddCommentUseCase
import android.content.Context
import android.content.SharedPreferences
import com.mandrykevich.myhelper.utils.RecentQueriesAdapter
import android.text.TextWatcher
import android.text.Editable
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.runtime.network.NetworkError
import com.yandex.mapkit.directions.driving.DrivingRouterType

class MapFragment : Fragment(), OnCommentFetchListener, OnObjectTapListener {
    private lateinit var binding: FragmentMapBinding
    private lateinit var mapView: MapView
    private lateinit var mapStateManager: MapStateManager
    private lateinit var tapListener: MapTapListener
    private lateinit var addCommentUseCase: AddCommentUseCase
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var viewModel: MapViewModel
    private lateinit var viewModelSearch: SearchViewModel
    val commentsList = mutableListOf<Comment>()

    private lateinit var imageViewStars: List<ImageView>
    private var activeStars = 0
    private var hasHelper = false
    private var hasElevator = false
    private var hasParking = false

    private var userLocationLayer: UserLocationLayer? = null

    // Переменные для последних запросов
    private lateinit var recentQueriesAdapter: RecentQueriesAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "RecentQueriesPrefs"
    private val QUERIES_KEY = "queries"
    private val MAX_QUERIES = 5 // Максимальное количество сохраняемых запросов
    private val MAX_VISIBLE_QUERIES = 3 // Максимальное количество отображаемых запросов

    private var selectedPoint: Point? = null

    private var drivingSession: DrivingSession? = null
    private lateinit var routesCollection: MapObjectCollection

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater)
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]
        viewModelSearch = ViewModelProvider(this)[SearchViewModel::class.java]
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setupRecyclerView()
        initializeMap()
        initializeFirebase()
        setupStarClickListeners()
        setupRecentQueries()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvReviews.layoutManager = LinearLayoutManager(requireContext())
        commentsAdapter = CommentsAdapter(commentsList, showReportButton = true)
        binding.rvReviews.adapter = commentsAdapter
    }

    private fun initializeMap() {
        mapView = binding.mainMap
        mapStateManager = MapStateManager(requireActivity())
        tapListener = MapTapListener("", requireContext(), binding, this, this)
        binding.mainMap.map.addTapListener(tapListener)
        val initialPosition = mapStateManager.restoreMapState(mapView)
        mapView.map.move(
            initialPosition,
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
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
        requestLocationPermission()
        routesCollection = mapView.map.mapObjects.addCollection()
        
        binding.imDirection.setOnClickListener {
            buildRouteToMinsk()
        }
    }

    private fun setupRecentQueries() {
        recentQueriesAdapter = RecentQueriesAdapter(emptyList()) { query ->
            binding.editTextText.setText(query)
            binding.editTextText.setSelection(query.length)
            binding.cvRespond.visibility = View.GONE
        }
        binding.rvLastRespond.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLastRespond.adapter = recentQueriesAdapter

        binding.cvRespond.visibility = View.GONE

        binding.editTextText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    val currentText = s.toString()
                    val allQueries = getRecentQueries()

                    if (currentText.isEmpty()) {
                        // Если текст пустой, показываем последние MAX_VISIBLE_QUERIES запросов
                        val recent = if (allQueries.size > MAX_VISIBLE_QUERIES) allQueries.take(MAX_VISIBLE_QUERIES) else allQueries
                        recentQueriesAdapter.updateQueries(recent)
                        binding.cvRespond.visibility = if (recent.isNotEmpty()) View.VISIBLE else View.GONE
                    } else {
                        // Фильтруем запросы по введенному тексту и ограничиваем количество
                        val filteredQueries = allQueries.filter { it.contains(currentText, ignoreCase = true) }
                            .take(MAX_VISIBLE_QUERIES)
                        recentQueriesAdapter.updateQueries(filteredQueries)
                        binding.cvRespond.visibility = if (filteredQueries.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }

            override fun afterTextChanged(e: Editable?) {}
        })
    }

    private fun loadAndShowRecentQueries() {
        val queries = getRecentQueries()
        if (queries.isNotEmpty()) {
            recentQueriesAdapter.updateQueries(queries)
            binding.cvRespond.visibility = View.VISIBLE
        } else {
            binding.cvRespond.visibility = View.GONE
        }
    }

    private fun saveRecentQuery(query: String) {
        val queries = getRecentQueries().toMutableList()
        queries.remove(query)
        queries.add(0, query)
        while (queries.size > MAX_QUERIES) {
            queries.removeAt(queries.size - 1)
        }
        val editor = sharedPreferences.edit()
        editor.putString(QUERIES_KEY, queries.joinToString(",,"))
        editor.apply()
    }

    private fun getRecentQueries(): List<String> {
        val queriesString = sharedPreferences.getString(QUERIES_KEY, null)
        return if (queriesString.isNullOrEmpty()) {
            emptyList()
        } else {
            queriesString.split(",,").toList()
        }
    }

    private fun setupSearchButton() {
        binding.imSearch.setOnClickListener {
            val query = binding.editTextText.text.toString()
            if (query.isNotEmpty()) {
                saveRecentQuery(query)
                performSearch(query)
                binding.cvRespond.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), "Введите текст для поиска", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.onEach { geoObjects ->
            mapView.map.mapObjects.clear()
            geoObjects.forEach { geoObject ->
                geoObject.geometry.firstOrNull()?.point?.let { point ->
                    addPlacemark(point)
                    if (::viewModelSearch.isInitialized) {
                        viewModelSearch.addSearchQuery("Ваш запрос")
                    }
                    val cameraPosition = CameraPosition(Point(point.latitude, point.longitude), 18f, 0.0f, 0.0f)
                    mapView.map.move(cameraPosition, Animation(Animation.Type.SMOOTH, 0.5f), null)
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
        drivingSession?.cancel()
        super.onDestroyView()
        if (::tapListener.isInitialized) {
            binding.mainMap.map.removeTapListener(tapListener)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
            userLocationLayer?.setVisible(true)
            userLocationLayer?.setObjectListener(userLocationObjectListener)
        } else {
            requestLocationPermission()
        }
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        if (userLocationLayer != null) {
            userLocationLayer?.setVisible(false)
        }
        mapStateManager.saveMapState(mapView)
    }

    override fun fetchComments(buildingId: String) {
        Log.d("MapFragment", "Fetching comments for buildingId: $buildingId (filtering locally)")

        val commentsRef = FirebaseDatabase.getInstance().getReference("Comments")

        commentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentsList.clear()
                Log.d("MapFragment", "Received data snapshot with ${snapshot.childrenCount} total children from 'Comments' node.")

                val fetchedAndFilteredComments = mutableListOf<Comment>()

                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    if (comment != null) {
                        if (comment.buildingId == buildingId) {
                            comment.id = commentSnapshot.key
                            fetchedAndFilteredComments.add(comment)
                            Log.d("MapFragment", "Fetched and filtered comment with ID: ${commentSnapshot.key} for buildingId: ${comment.buildingId}")
                        } else {
                            // Опционально: логировать комментарии, которые не соответствуют buildingId
                            // Log.d("MapFragment", "Skipping comment with ID: ${commentSnapshot.key} for buildingId: ${comment.buildingId}")
                        }
                    } else {
                        Log.w("MapFragment", "Failed to parse comment at key: ${commentSnapshot.key}")
                    }
                }

                commentsList.addAll(fetchedAndFilteredComments)

                if (::commentsAdapter.isInitialized) {
                    commentsAdapter.updateComments(commentsList)
                    Log.d("MapFragment", "CommentsAdapter updated with ${commentsList.size} filtered comments.")
                } else {
                    Log.e("MapFragment", "CommentsAdapter is not initialized!")
                }

                if (commentsList.isNotEmpty()) {
                    binding.rvReviews.visibility = View.VISIBLE
                    Log.d("MapFragment", "Showing RecyclerView, filtered comments found.")
                } else {
                    binding.rvReviews.visibility = View.GONE
                    Log.d("MapFragment", "Hiding RecyclerView, no filtered comments found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MapFragment", "Error fetching comments: ${error.message}")
            }
        })
    }

    private val userLocationObjectListener = object : UserLocationObjectListener {
        override fun onObjectAdded(userLocationView: UserLocationView) {
            userLocationView.arrow.setIcon(
                ImageProvider.fromResource(
                    requireContext(), R.drawable.user_arrow))

            val pinIcon = userLocationView.pin.useCompositeIcon()

            pinIcon.setIcon(
                "icon",
                ImageProvider.fromResource(requireContext(), R.drawable.search_result_placemark),
                IconStyle().setAnchor(
                    android.graphics.PointF(
                        0.5f, 0.5f)))

            val accuracyCircleColor = try {
                ContextCompat.getColor(requireContext(), R.color.green)
            } catch (e: Exception) {
                ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)
            }
            userLocationView.accuracyCircle.fillColor = accuracyCircleColor
        }

        override fun onObjectRemoved(userLocationView: UserLocationView) {
            // Nothing to do
        }

        override fun onObjectUpdated(userLocationView: UserLocationView, objectEvent: ObjectEvent) {
            // Nothing to do
        }
    }

    private val REQUEST_LOCATION_PERMISSION = 1

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapView.mapWindow)
                userLocationLayer?.setVisible(true)
                userLocationLayer?.setObjectListener(userLocationObjectListener)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Для отображения вашего местоположения необходимо разрешение",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun toggleButtonState(button: ImageView) {
        val tagKey = button.id

        val currentState = button.getTag(tagKey) as? Boolean ?: false
        val newState = !currentState
        button.setTag(tagKey, newState)

        val colorResId = if (newState) {
             try {
                 when (button.id) {
                    R.id.iv_has_helper -> R.color.Blue
                    R.id.iv_has_elevator -> R.color.Green
                    R.id.iv_has_parking -> R.color.orange
                    else -> android.R.color.holo_blue_light
                 }
            } catch (e: Exception) {
                android.R.color.holo_blue_light
            }
        } else {
             try {
                 R.color.Grey1
             } catch (e: Exception) {
                 android.R.color.darker_gray
             }
        }

        val color = ContextCompat.getColor(requireContext(), colorResId)
        button.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        when (button.id) {
            R.id.iv_has_helper -> hasHelper = newState
            R.id.iv_has_elevator -> hasElevator = newState
            R.id.iv_has_parking -> hasParking = newState
        }
    }

    private fun clearForm() {
        updateActiveStars(0)
        hasHelper = false
        hasElevator = false
        hasParking = false
        binding.editComText.text.clear()
        if (::imageViewStars.isInitialized) {
             toggleButtonState(binding.ivHasHelper)
             toggleButtonState(binding.ivHasElevator)
             toggleButtonState(binding.ivHasParking)
        }
    }

    private fun updateActiveStars(count: Int) {
        activeStars = count
        if (::imageViewStars.isInitialized) {
            imageViewStars.forEachIndexed { index, imageView ->
                val colorResId = if (index < count) {
                     try {
                         R.color.yellow
                     } catch (e: Exception) {
                         android.R.color.holo_orange_light
                     }
                } else {
                     try {
                         R.color.Grey1
                     } catch (e: Exception) {
                         android.R.color.darker_gray
                     }
                }
                val color = ContextCompat.getColor(requireContext(), colorResId)
                imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private fun addPlacemark(point: Point) {
        val placemark = mapView.map.mapObjects.addPlacemark(
            point,
            ImageProvider.fromResource(requireContext(), R.drawable.search_result_placemark)
        )
        if (::tapListener.isInitialized) {
             placemark.addTapListener(tapListener)
        }
    }

    private fun performSearch(query: String) {
        val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        val options = com.yandex.mapkit.search.SearchOptions()
        val visibleRegion = mapView.map.visibleRegion
        val screenCenter = VisibleRegionUtils.toPolygon(visibleRegion)

        searchManager.submit(
            query,
            screenCenter,
            options,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    if (::viewModel.isInitialized) {
                        viewModel.processSearchResults(response)
                    }
                }

                override fun onSearchError(error: Error) {
                    Log.e("Search", "Search error: ${error.javaClass.name}")
                    Toast.makeText(requireContext(), "Ошибка поиска: ${error.javaClass.name}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onObjectTapped(point: Point?, objectName: String?, objectId: String?) {
        binding.cvRespond.visibility = View.GONE
        selectedPoint = point
    }

    private fun buildRouteToMinsk() {
        val startPoint = selectedPoint
        if (startPoint == null) {
            Toast.makeText(requireContext(), "Сначала выберите точку на карте", Toast.LENGTH_SHORT).show()
            return
        }

        val minskPoint = Point(53.900601, 27.558972) // Центр Минска

        val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()

        val requestPoints = listOf(
            RequestPoint(startPoint, RequestPointType.WAYPOINT, null, null),
            RequestPoint(minskPoint, RequestPointType.WAYPOINT, null, null)
        )

        drivingSession = drivingRouter.requestRoutes(
            requestPoints,
            drivingOptions,
            vehicleOptions,
            object : DrivingSession.DrivingRouteListener {
                override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
                    routesCollection.clear()
                    if (drivingRoutes.isEmpty()) return

                    drivingRoutes.forEachIndexed { index, route ->
                        routesCollection.addPolyline(route.geometry).apply {
                            zIndex = if (index == 0) 10f else 5f
                            setStrokeColor(ContextCompat.getColor(requireContext(),
                                if (index == 0) R.color.green else R.color.green))
                            setStrokeWidth(if (index == 0) 8f else 5f)
                        }
                    }
                }

                override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
                    when (error) {
                        is NetworkError -> Toast.makeText(requireContext(), "Ошибка сети при построении маршрута", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(requireContext(), "Ошибка построения маршрута", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
}


