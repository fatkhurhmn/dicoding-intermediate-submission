package academy.bangkit.storyapp.ui.main.maps

import academy.bangkit.storyapp.R
import academy.bangkit.storyapp.data.Result
import academy.bangkit.storyapp.data.remote.response.StoryResponse
import academy.bangkit.storyapp.databinding.FragmentMapsBinding
import academy.bangkit.storyapp.ui.main.MainActivity
import academy.bangkit.storyapp.utils.Extension.showMessage
import academy.bangkit.storyapp.utils.ViewModelFactory
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import java.net.URL
import java.util.concurrent.Executors


class MapsFragment : Fragment() {

    private lateinit var mMap: GoogleMap
    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val mapsViewModel: MapsViewModel by viewModels {
        ViewModelFactory.getInstance(
            requireContext()
        )
    }

    private val callback = OnMapReadyCallback { mMap ->
        this.mMap = mMap
        with(mMap.uiSettings) {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }

        setStartLocation()
        getAllStory()
        setMapStyle()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private fun setStartLocation() {
        val defaultLocation = LatLng(-6.200000, 106.816666)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 0f))
    }

    private fun getAllStory() {
        val token = arguments?.getString(MainActivity.EXTRA_TOKEN)
        if (token != null) {
            mapsViewModel.getAllStoryWithLocation("Bearer $token")
                .observe(viewLifecycleOwner) { result ->
                    when (result) {
                        is Result.Loading -> {
                            binding?.progressBarMaps?.visibility = View.VISIBLE
                        }

                        is Result.Success -> {
                            binding?.progressBarMaps?.visibility = View.GONE
                            showAllStory(result.data.stories)
                        }

                        is Result.Error -> {
                            binding?.apply {
                                progressBarMaps.visibility = View.GONE
                                imgMapsError.visibility = View.VISIBLE
                                map.visibility = View.GONE
                            }
                            binding?.root?.let { view -> result.error.showMessage(view) }
                        }
                    }
                }
        }
    }

    private fun showAllStory(stories: List<StoryResponse>) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        for (data in stories) {
            if (data.lat != null && data.lon != null) {
                val location = LatLng(data.lat, data.lon)
                val url = URL(data.photoUrl)
                executor.execute {
                    try {
                        val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                        handler.post {
                            val smallMarker = Bitmap.createScaledBitmap(bmp, 200, 200, false)
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .title("Story by ${data.name}")
                                    .snippet(data.description)
                                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                            )
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }

            }
        }
    }

    private fun setMapStyle() {
        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
        } catch (exception: Resources.NotFoundException) {
            exception.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}