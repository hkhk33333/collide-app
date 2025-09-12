package com.test.testing.discord.viewmodels

import androidx.lifecycle.ViewModel
import com.test.testing.discord.location.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel
    @Inject
    constructor(
        val locationManager: LocationManager,
    ) : ViewModel()
