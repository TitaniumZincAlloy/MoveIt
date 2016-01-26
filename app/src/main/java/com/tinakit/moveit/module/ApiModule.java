package com.tinakit.moveit.module;

import com.tinakit.moveit.api.GoogleApi;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Tina on 1/8/2016.
 */

@Module
public class ApiModule {

    @Provides
    GoogleApi provideGoogleApi(){

        return new GoogleApi();
    }
}
