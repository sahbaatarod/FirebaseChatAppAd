package edu.sahba.firebasefriendlychat

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class LoginFragmentTest {

    @Test
    fun test_isFragmentInView() {
        val scenario = launchFragmentInContainer<LoginFragment>()
        onView(withId(R.id.fragment_login_parent)).check(matches(isDisplayed()))
    }

}