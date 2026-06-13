package com.asc.gymgenie

import com.asc.gymgenie.activity.service.ActivityServiceTest
import com.asc.gymgenie.auth.service.AuthServiceTest
import com.asc.gymgenie.exercise.service.ExerciseServiceTest
import com.asc.gymgenie.user.service.UserServiceTest
import com.asc.gymgenie.workout.service.WorkoutSessionServiceTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GymgenieApplicationTests {

	@Test
	fun sanityCheck() {
		assert(true)
	}

	@Nested
	inner class Auth : AuthServiceTest()

	@Nested
	inner class Exercise : ExerciseServiceTest()

	@Nested
	inner class WorkoutSession : WorkoutSessionServiceTest()

	@Nested
	inner class User : UserServiceTest()

	@Nested
	inner class Activity : ActivityServiceTest()
}
