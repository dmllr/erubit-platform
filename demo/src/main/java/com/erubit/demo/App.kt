package com.erubit.demo


class App : a.erubit.platform.learning.App() {

	override fun onCreate() {
		super.onCreate()

		initialize(object : Initializer() {
			override fun resolveContentsResource(): Int {
				return R.raw._contents
			}
		})
	}

}

