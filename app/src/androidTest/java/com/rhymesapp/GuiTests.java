package com.rhymesapp;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import rhymesapp.RhymesBaseActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
//import android.test.*;

/**
 * Created by Fabrice Vanner on 22.01.2017.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class GuiTests {


    @Rule
    public ActivityTestRule<RhymesBaseActivity> mActivityRule = new ActivityTestRule<RhymesBaseActivity>(
            RhymesBaseActivity.class);

    @Before
    public void initValidString() {
        // Specify a valid string.
        //   mStringToBetyped = "Espresso";
    }


    @Test
    public void backButtonReswitchToApp() {
        onView(withId(R.id.randomQueryButton))
                .perform(click());
        /*
        onView(withId(R.id.greet_button))
                .perform(click());
        onView(withText("Hello Steve!"))
                .check(matches(isDisplayed()));

                */
        pressBack();

    }
}
